// Copyright 2007, 2008, 2009, 2010, 2011, 2012, 2014, 2016, 2019 Global Virtual Airlines Group. All Rights Reserved.
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
 * @version 8.6
 * @since 2.0
 */

public class ServiceRequestCommand extends DispatchCommand {

	/**
	 * Initializes the Command.
	 */
	public ServiceRequestCommand() {
		super(ServiceRequestCommand.class);
	}

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
				log.warn(c.getUser().getName() + " requesting Dispatch service using invalid build " + c.getClientBuild());	

				// Send response
				SystemTextMessage txtMsg = new SystemTextMessage();
				txtMsg.addMessage("ACARS Client Build " + c.getClientBuild() + " cannot request Dispatch service");
				ctx.push(txtMsg);
				return;
			}
			
			// Check the schedule
			GetSchedule sdao = new GetSchedule(con);
			schedInfo = sdao.getFlightNumber(msg, ud.getDB());
			boolean routeValid = (schedInfo != null);
			
			// If we're not valid, check for a draft flight assignment
			if (!routeValid) {
				GetFlightReports prdao = new GetFlightReports(con);
				Collection<FlightReport> pireps = prdao.getDraftReports(ud.getID(), msg, ud.getDB());
				for (Iterator<FlightReport> i = pireps.iterator(); i.hasNext() && !routeValid; ) {
					FlightReport fr = i.next();
					boolean isOK = fr.hasAttribute(FlightReport.ATTR_CHARTER) || (fr.getDatabaseID(DatabaseID.ASSIGN) > 0);
					isOK &= msg.getAirportD().equals(fr.getAirportD());
					isOK &= msg.getAirportA().equals(fr.getAirportA());
					if (isOK) {
						log.info("Validated route " + msg.getAirportD() + " to " + msg.getAirportA() + " using draft PIREP");
						routeValid = true;
					}
				}
			}
			
			// If we're still not valid, check for an event
			if (!routeValid) {
				Instant now = Instant.now();
				GetEvent edao = new GetEvent(con); 
				routeValid |= (edao.getPossibleEvent(msg, OnlineNetwork.VATSIM, now) > 0);
				routeValid |= (edao.getPossibleEvent(msg, OnlineNetwork.IVAO, now) > 0);
				if (routeValid)
					log.info("Validated route " + msg.getAirportD() + " to " + msg.getAirportA() + " using Online Event");
			}
			
			// Set the route validated flag
			msg.setRouteValid(routeValid);
			
			// Get the aircraft type and check ETOPS
			GetAircraft acdao = new GetAircraft(con);
			Aircraft a = acdao.get(msg.getEquipmentType());
			Collection<GeoLocation> gc = GeoUtils.greatCircle(msg.getAirportD(), msg.getAirportA(), 20);
			ETOPS e = ETOPSHelper.classify(gc).getResult();
			msg.setETOPSWarning(ETOPSHelper.validate(a, e));
			
			// Find the closest gate
			GetGates gdao = new GetGates(con);
			gdao.setQueryMax(25);
			msg.setArrivalGates(gdao.getPopularGates(msg, msg.getSimulator(), false));
			SortedSet<Gate> gates = new TreeSet<Gate>(new GeoComparator(msg, true));
			gates.addAll(gdao.getGates(msg.getAirportD(), msg.getSimulator()));
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
			for (FlightRoute fr : helper.getRoutes()) {
				if (fr instanceof PopulatedRoute)
					plans.add((PopulatedRoute) fr);
			}
		} catch (DAOException de) {
			log.error("Cannot validate/load route - " + de.getMessage(), de);
			AcknowledgeMessage errorMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
			errorMsg.setEntry("error", "Cannot validate route");
			ctx.push(errorMsg);
		} finally {
			ctx.release();
		}
		
		// Send to dispatchers if not in auto dispatch mode
		int reqsSent = 0; int outOfRange = 0;
		Collection<ACARSConnection> cons = ctx.getACARSConnectionPool().getAll();
		if (!msg.isAutoDispatch() || plans.isEmpty()) {
			for (Iterator<ACARSConnection> i = cons.iterator(); i.hasNext(); ) {
				ACARSConnection ac = i.next();
				if (ac.getIsDispatch() && !ac.getUserBusy() && !ac.getUserHidden()) {
					GeoPosition gp = new GeoPosition(ac.getLocation());
					int distance = gp.distanceTo(msg);
					if (ac.getUser().getID() == c.getUser().getID())
						log.warn(c.getUserID() + " attempting self dispatch");
					else if (distance <= ac.getRange()) {
						reqsSent++;
						ctx.push(msg, ac.getID(), true);
					} else {
						outOfRange++;
						log.info("Dispatch service request not sent to " + ac.getUserID() + ", distance=" + distance);
					}
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

		// Return back the routes - only if valid
		if (!plans.isEmpty() && msg.isRouteValid()) {
			RouteInfoMessage rmsg = new RouteInfoMessage(c.getUser(), msg.getID());
			rmsg.setClosestGate(msg.getClosestGate());
			for (Gate g : msg.getArrivalGates())
				rmsg.addGate(g);
			for (PopulatedRoute rp : plans)
				rmsg.addPlan(rp);
			
			// Add schedule info - if schedInfo is null but routeValid is true, create a dummy entry
			rmsg.setScheduleInfo(schedInfo);
			if (msg.isRouteValid() && !rmsg.isRouteValid()) {
				Airline a = SystemData.getAirline(ud.getAirlineCode());
				int fNum = c.getUser().getPilotNumber();
				if (fNum >= 10000)
					fNum -= 10000;
				
				rmsg.setScheduleInfo(new ScheduleEntry(a, Math.max(1, fNum), 1));
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
			txtMsg.addMessage("No available Dispatchers within range, and no Dispatch routes found.");
			ctx.push(txtMsg);
			ctx.push(new CancelMessage(env.getOwner()));
		}
	}
}