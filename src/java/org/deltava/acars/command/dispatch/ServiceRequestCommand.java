// Copyright 2007, 2008, 2009, 2010, 2011, 2012, 2014, 2016, 2019, 2020, 2021, 2022, 2023, 2024, 2025 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.dispatch;

import java.util.*;
import java.sql.Connection;
import java.time.Instant;

import org.deltava.beans.*;
import org.deltava.beans.flight.*;
import org.deltava.beans.navdata.Gate;
import org.deltava.beans.schedule.*;

import org.deltava.comparators.GeoComparator;

import org.deltava.dao.*;

import org.deltava.util.GeoUtils;
import org.deltava.util.system.SystemData;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.dispatch.*;

/**
 * An ACARS Command to handle Dispatch service request messages.
 * @author Luke
 * @version 12.0
 * @since 2.0
 */

public class ServiceRequestCommand extends DispatchCommand {

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the inbound message
		RequestMessage msg = (RequestMessage) env.getMessage();
		ACARSConnection c = ctx.getACARSConnection();
		
		// Validate that the route is valid
		UserData ud = c.getUserData();
		Flight schedInfo = null;
		Collection<PopulatedRoute> plans = new ArrayList<PopulatedRoute>();
		try {
			Connection con = ctx.getConnection();
			
			// Check minimum build number
			GetACARSBuilds abdao = new GetACARSBuilds(con);
			if (!abdao.isDispatchAvailable(c)) {
				ctx.release();
				log.warn("{} requesting Dispatch service using invalid build {}", c.getUser().getName(), Integer.valueOf(c.getClientBuild()));	

				// Send response
				SystemTextMessage txtMsg = new SystemTextMessage();
				txtMsg.addMessage("ACARS Client Build " + c.getClientBuild() + " cannot request Dispatch service");
				ctx.push(txtMsg);
				return;
			}
			
			// Check the schedule
			GetRawSchedule rsdao = new GetRawSchedule(con);
			GetSchedule sdao = new GetSchedule(con);
			sdao.setSources(rsdao.getSources(true, ud.getDB()));
			schedInfo = sdao.getFlightNumber(msg, ud.getDB());
			boolean routeValid = (schedInfo != null);
			
			// If we're not valid, check for a draft flight assignment
			if (!routeValid) {
				GetFlightReports prdao = new GetFlightReports(con);
				Collection<FlightReport> pireps = prdao.getDraftReports(ud.getID(), msg, ud.getDB());
				for (Iterator<FlightReport> i = pireps.iterator(); i.hasNext() && !routeValid; ) {
					FlightReport fr = i.next();
					boolean isOK = fr.hasAttribute(FlightReport.ATTR_CHARTER) || (fr.getDatabaseID(DatabaseID.ASSIGN) > 0);
					isOK &= msg.matches(fr);
					if (isOK) {
						log.info("Validated route {} to {} using draft PIREP", msg.getAirportD(), msg.getAirportA());
						routeValid = true;
					}
				}
			}
			
			// If we're still not valid, check for an event
			if (!routeValid) {
				Instant now = Instant.ofEpochMilli(msg.getTime());
				GetEvent edao = new GetEvent(con); 
				routeValid |= !edao.getPossibleEvents(msg, OnlineNetwork.VATSIM, now, ud.getAirlineCode()).isEmpty();
				routeValid |= !edao.getPossibleEvents(msg, OnlineNetwork.IVAO, now, ud.getAirlineCode()).isEmpty();
				routeValid |= !edao.getPossibleEvents(msg, OnlineNetwork.PILOTEDGE, now, ud.getAirlineCode()).isEmpty();
				if (routeValid)
					log.info("Validated route {} to {} using Online Event", msg.getAirportD(), msg.getAirportD());
			}
			
			// Set the route validated flag
			msg.setRouteValid(routeValid);
			
			// Get the aircraft type and check ETOPS
			GetAircraft acdao = new GetAircraft(con);
			Aircraft a = acdao.get(msg.getEquipmentType());
			if (a == null)
				throw new DAOException("Unknown aircraft - " + msg.getEquipmentType());
			
			AircraftPolicyOptions opts = a.getOptions(ud.getAirlineCode());
			Collection<GeoLocation> gc = GeoUtils.greatCircle(msg.getAirportD(), msg.getAirportA(), GeoUtils.GC_SEGMENT_SIZE);
			ETOPS e = ETOPSHelper.classify(gc).getResult();
			msg.setETOPSWarning(ETOPSHelper.isWarn(opts.getETOPS(), e));
			
			// Find the closest gate
			GetGates gdao = new GetGates(con);
			GateHelper gh = new GateHelper(msg, msg.getAirline(), 25, false);
			gh.addDepartureGates(gdao.getGates(msg.getAirportD()), gdao.getUsage(msg, true, ctx.getDB()));
			gh.addArrivalGates(gdao.getGates(msg.getAirportA()), gdao.getUsage(msg, false, ctx.getDB()));
			msg.setArrivalGates(gh.getArrivalGates());
			SortedSet<Gate> gates = new TreeSet<Gate>(new GeoComparator(msg, true));
			gates.addAll(gh.getDepartureGates());
			if (!gates.isEmpty())
				msg.setClosestGate(gates.first());
			
			// Load existing plans
			RouteLoadHelper helper = new RouteLoadHelper(con, msg);
			helper.loadDispatchRoutes();
			
			// Load cached routes
			if (!helper.hasRoutes())
				helper.loadCachedRoutes();
			
			// If we still don't have any routes, load from PIREPs
			if (!helper.hasRoutes())
				helper.loadPIREPRoutes(c.getUserData().getDB());

			// Populate the routes
			helper.loadWeather();
			helper.calculateBestTerminalRoute();
			helper.populateRoutes();
			helper.getRoutes().stream().filter(PopulatedRoute.class::isInstance).map(PopulatedRoute.class::cast).forEach(plans::add);
		} catch (DAOException de) {
			log.atError().withThrowable(de).log("Cannot validate/load route - {}", de.getMessage());
			ctx.push(new AcknowledgeMessage(env.getOwner(), msg.getID(), "Cannot validate route - " + de.getMessage()));
		} finally {
			ctx.release();
		}
		
		// Send to dispatchers if not in auto dispatch mode
		int reqsSent = 0; int outOfRange = 0;
		Collection<ACARSConnection> cons = ctx.getACARSConnectionPool().getAll(ac -> ac.getIsDispatch() && !ac.getUserBusy() && !ac.getUserHidden());
		if (!msg.isAutoDispatch() || plans.isEmpty()) {
			for (ACARSConnection ac : cons) {
				int distance = ac.getLocation().distanceTo(msg);
				if (ac.getUser().getID() == c.getUser().getID())
					log.warn("{} attempting self dispatch", c.getUserID());
				else if (distance <= ac.getRange()) {
					reqsSent++;
					ctx.push(msg, ac.getID(), true);
					log.info("Dispatch service request {} send to {} {}", Long.valueOf(msg.getID()), ac.getUserID(), Long.valueOf(ac.getID()));
				} else {
					outOfRange++;
					log.info("Dispatch service request not sent to {}, distance={}", ac.getUserID(), Integer.valueOf(distance));
				}
			}
		}
		
		// If we sent to a dispatcher, send a system text message back
		if (reqsSent > 0) {
			SystemTextMessage txtMsg = new SystemTextMessage();
			txtMsg.addMessage("Dispatch Request sent to " + reqsSent + " Dispatcher(s)");
			if ((schedInfo != null) && !msg.getAirline().equals(schedInfo.getAirline()))
				txtMsg.addMessage("Airline changed to " + schedInfo.getAirline().getName());
			if (!msg.isRouteValid())
				txtMsg.addMessage("You are requesting an Invalid Route, and are unlikely to receive Service!");
			
			ctx.push(txtMsg);
			return;
		}

		// Return back the routes
		if (!plans.isEmpty()) {
			RouteInfoMessage rmsg = new RouteInfoMessage(c.getUser(), msg.getID());
			rmsg.setClosestGate(msg.getClosestGate());
			msg.getArrivalGates().forEach(rmsg::addGate);
			plans.forEach(rmsg::addPlan);
			
			// Add schedule info - if schedInfo is null but routeValid is true, create a dummy entry
			rmsg.setScheduleInfo(schedInfo);
			if (msg.isRouteValid() && !rmsg.isRouteValid()) {
				Airline a = SystemData.getAirline(ud.getAirlineCode());
				int fNum = c.getUser().getPilotNumber();
				if (fNum >= 10000)
					fNum -= 10000;
				
				rmsg.setScheduleInfo(new ScheduleEntry(a, Math.max(1, fNum), 1));
			}
			
			// Send out of schedule warning
			if (!msg.isRouteValid()) {
				SystemTextMessage txtMsg = new SystemTextMessage();
				txtMsg.addMessage(String.format("%s - %s not found in %s schedule", msg.getAirportD().getIATA(), msg.getAirline().getICAO(), ud.getAirlineCode()));
				ctx.push(txtMsg);
			}
			
			// Build message
			StringBuilder buf = new StringBuilder("No Dispatchers available");
			if (outOfRange > 0)
				buf.append(" (" + outOfRange + " out of range)");

			buf.append(", loaded " + plans.size() + " Dispatch routes from database");
			rmsg.setMessage(buf.toString());
			ctx.push(rmsg);
		} else {
			SystemTextMessage txtMsg = new SystemTextMessage();
			if (outOfRange > 0)
				txtMsg.addMessage(outOfRange + " Dispatchers online, but out of range");
			if (!msg.isRouteValid())
				txtMsg.addMessage(String.format("%s - %s not found in %s schedule", msg.getAirportD().getIATA(), msg.getAirline().getICAO(), ud.getAirlineCode()));
			txtMsg.addMessage("No available Dispatchers within range, and no Dispatch routes found.");
			ctx.push(txtMsg);
			ctx.push(new CancelMessage(env.getOwner()));
		}
	}
}