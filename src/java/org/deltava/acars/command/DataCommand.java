// Copyright 2004, 2005, 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import java.util.*;

import java.sql.Connection;

import org.apache.log4j.Logger;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;


import org.deltava.beans.FlightReport;
import org.deltava.beans.system.UserData;
import org.deltava.beans.schedule.*;
import org.deltava.beans.servinfo.*;
import org.deltava.beans.navdata.*;
import org.deltava.beans.ts2.Server;

import org.deltava.comparators.AirportComparator;

import org.deltava.dao.*;
import org.deltava.dao.file.GetServInfo;

import org.deltava.util.ThreadUtils;
import org.deltava.util.servinfo.ServInfoLoader;
import org.deltava.util.system.SystemData;

/**
 * An ACARS server command to handle data requests.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class DataCommand extends ACARSCommand {

	private static final Logger log = Logger.getLogger(DataCommand.class);

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@SuppressWarnings("unchecked")
	public void execute(CommandContext ctx, Envelope env) {

		// Get the message
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		ACARSConnection ac = ctx.getACARSConnection();

		// Get the connections to process
		Iterator<ACARSConnection> i = ctx.getACARSConnections(msg.getFilter()).iterator();

		// Create the response
		DataResponseMessage dataRsp = new DataResponseMessage(env.getOwner(), msg.getRequestType());
		log.info("Data Request (" + DataMessage.REQ_TYPES[msg.getRequestType()] + ") from " + env.getOwnerID());
		ctx.setMessage("Processing Data Request (" + DataMessage.REQ_TYPES[msg.getRequestType()] + ") from "
				+ env.getOwnerID());

		switch (msg.getRequestType()) {
			// Get all of the pilot info stuff
			case DataMessage.REQ_PLIST:
				while (i.hasNext()) {
					ACARSConnection c = i.next();
					dataRsp.addResponse(c.getPosition());
				}

				break;

			case DataMessage.REQ_BUSY:
				ac.setUserBusy(Boolean.valueOf(msg.getFlag("isBusy")).booleanValue());

				// Push the update to everyone else
				DataResponseMessage drmsg = new DataResponseMessage(env.getOwner(), DataMessage.REQ_BUSY);
				drmsg.addResponse(ac);
				ctx.pushAll(drmsg, 0);
				break;

			case DataMessage.REQ_TS2SERVERS:
				try {
					Connection con = ctx.getConnection();

					// Get the DAO and the server info
					GetTS2Data dao = new GetTS2Data(con);
					Collection<Server> srvs = dao.getServers(env.getOwner().getRoles());
					for (Iterator<Server> si = srvs.iterator(); si.hasNext();) {
						Server srv = si.next();
						dataRsp.addResponse(srv);
					}
				} catch (DAOException de) {
					log.error("Error loading TS2 Server datas - " + de.getMessage(), de);
				} finally {
					ctx.release();
				}

				break;

			// Get Pilot/position info
			case DataMessage.REQ_USRLIST:
			case DataMessage.REQ_PILOTINFO:
				boolean showHidden = ctx.getACARSConnection().getUser().isInRole("HR");
				while (i.hasNext()) {
					ACARSConnection acon = i.next();
					if (showHidden || (!acon.getUserHidden()))
						dataRsp.addResponse(acon);
				}

				break;

			// Get equipment list
			case DataMessage.REQ_EQLIST:
				Set<String> eqTypes = new TreeSet<String>((Collection<? extends String>) SystemData
						.getObject("eqtypes"));
				dataRsp.addResponse("eqtype", eqTypes);
				break;

			// Get airline list
			case DataMessage.REQ_ALLIST:
				UserData usrData = ac.getUserData();
				
				// Only retrieve airlines applicable to the specific user
				Map airlines = (Map) SystemData.getObject("airlines");
				for (Iterator<Airline> ai = airlines.values().iterator(); ai.hasNext();) {
					Airline a = ai.next();
					if (a.getApplications().contains(usrData.getAirlineCode()))
						dataRsp.addResponse(a);
				}

				break;

			// Get airport list
			case DataMessage.REQ_APLIST:
				Map allAirports = (Map) SystemData.getObject("airports");
				Set<Airport> airports = new TreeSet<Airport>(new AirportComparator<Airport>(AirportComparator.NAME));
				airports.addAll(allAirports.values());
				for (Iterator<Airport> ai = airports.iterator(); ai.hasNext();) {
					Airport a = ai.next();
					dataRsp.addResponse(a);
				}

				break;

			// Get private voice info
			case DataMessage.REQ_PVTVOX:
				dataRsp.addResponse("url", SystemData.get("airline.voice.url"));
				break;

			// Get controller info
			case DataMessage.REQ_ATCINFO:
				String network = msg.getFlag("network").toUpperCase();
				if ("OFFLINE".equals(network))
					break;

				// Get the network info from the cache
				NetworkInfo info = GetServInfo.getCachedInfo(network);
				ServInfoLoader loader = new ServInfoLoader(network);

				// If we get null, then block until we can load it; if we're expired, spawn a new loader thread
				if ((info == null) && (!ServInfoLoader.isLoading(network))) {
					log.info("Loading " + network + " data in main thread");
					Thread t = null;
					synchronized (ServInfoLoader.class) {
						t = new Thread(loader, network + " ServInfo Loader");
						t.setDaemon(true);
						t.setPriority(Math.max(Thread.MIN_PRIORITY, Thread.currentThread().getPriority() - 1));
						ServInfoLoader.addLoader(network, t);
					}

					// Wait for the thread to exit
					int totalTime = 0;
					while (ThreadUtils.isAlive(t) && (totalTime < 10000)) {
						totalTime += 250;
						ThreadUtils.sleep(250);
					}

					// If the thread hasn't died, then kill it
					if (totalTime >= 10000) {
						ThreadUtils.kill(t, 1000);
						info = new NetworkInfo(network);
					} else
						info = loader.getInfo();
				} else if (info == null) {
					info = new NetworkInfo(network);
				} else if (info.getExpired()) {
					synchronized (ServInfoLoader.class) {
						if (!ServInfoLoader.isLoading(network)) {
							log.info("Spawning new ServInfo load thread");
							Thread t = new Thread(loader, network + " ServInfo Loader");
							t.setDaemon(true);
							t.setPriority(Math.max(Thread.MIN_PRIORITY, Thread.currentThread().getPriority() - 1));
							ServInfoLoader.addLoader(network, t);
						} else
							log.warn("Already loading " + network + " information");
					}
				}

				// Filter the controllers based on range from position
				if (info != null) {
					Collection<Controller> ctrs = info.getControllers(ac.getPosition());
					for (Iterator<Controller> ci = ctrs.iterator(); ci.hasNext();)
						dataRsp.addResponse(ci.next());
				}

				break;

			// Get flight information
			case DataMessage.REQ_ILIST:
				while (i.hasNext()) {
					ACARSConnection c = i.next();
					dataRsp.addResponse(c.getFlightInfo());
				}

				break;

			// Get approach charts
			case DataMessage.REQ_CHARTS:
				Airport a = SystemData.getAirport(msg.getFlag("id"));
				if (a == null) {
					AcknowledgeMessage errMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
					errMsg.setEntry("error", "Unknown Airport " + msg.getFlag("id"));
					ctx.push(errMsg, ctx.getACARSConnection().getID());
					return;
				}

				try {
					Connection con = ctx.getConnection();

					// Get the DAO and the charts
					GetChart dao = new GetChart(con);
					Collection<Chart> charts = dao.getCharts(a);
					for (Iterator<Chart> ci = charts.iterator(); ci.hasNext();)
						dataRsp.addResponse(ci.next());
				} catch (DAOException de) {
					log.error("Error loading charts for " + msg.getFlag("id") + " - " + de.getMessage(), de);
					AcknowledgeMessage errMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
					errMsg.setEntry("error", "Cannot load " + msg.getFlag("id") + " charts");
					ctx.push(errMsg, ctx.getACARSConnection().getID());
				} finally {
					ctx.release();
				}

				break;

			// Get draft PIREP info
			case DataMessage.REQ_DRAFTPIREP:
				try {
					Connection con = ctx.getConnection();

					// Get the DAO and the flight report
					String db = ctx.getACARSConnection().getUserData().getDB();
					GetFlightReports frdao = new GetFlightReports(con);
					Collection<FlightReport> dFlights = frdao.getDraftReports(env.getOwner().getID(), null, null, db);
					for (Iterator<FlightReport> fi = dFlights.iterator(); fi.hasNext();) {
						FlightReport fr = fi.next();
						dataRsp.addResponse(fr);
					}
				} catch (DAOException de) {
					log.error("Error loading draft PIREP data for " + msg.getFlag("id") + " - " + de.getMessage(), de);
					AcknowledgeMessage errMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
					errMsg.setEntry("error", "Cannot load draft Flight Report");
					ctx.push(errMsg, ctx.getACARSConnection().getID());
				} finally {
					ctx.release();
				}

				break;

			// Get navaid/runway info
			case DataMessage.REQ_NAVAIDINFO:
				boolean isRunway = (msg.getFlag("runway") != null);
				try {
					Connection con = ctx.getConnection();

					// Get the DAO and find the Navaid in the DAFIF database
					GetNavData dao = new GetNavData(con);
					NavigationDataBean nav = null;
					if (isRunway) {
						Airport ap = SystemData.getAirport(msg.getFlag("id").toUpperCase());

						// Add a leading zero to the runway if required
						if (ap != null) {
							String runway = msg.getFlag("runway");
							if (Character.isLetter(runway.charAt(runway.length() - 1)) && (runway.length() == 2)) {
								runway = "0" + runway;
							} else if (runway.length() == 1) {
								runway = "0" + runway;
							}

							nav = dao.getRunway(ap.getICAO(), runway);
							if (nav != null) {
								log.info("Loaded Runway data for " + nav.getCode() + " " + runway);
								dataRsp.addResponse(nav);
							}
						}
					} else {
						NavigationDataMap ndMap = dao.get(msg.getFlag("id"));
						if (!ndMap.isEmpty()) {
							nav = ndMap.get(msg.getFlag("id"), ac.getPosition());
							log.info("Loaded Navigation data for " + nav.getCode());
							dataRsp.addResponse(new NavigationRadioBean(msg.getFlag("radio"), nav, msg.getFlag("hdg")));
						}
					}
				} catch (Exception e) {
					log.error("Error loading navaid " + msg.getFlag("id") + " - " + e.getMessage(), e);
					AcknowledgeMessage errorMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
					errorMsg.setEntry("error", "Cannot load navaid " + msg.getFlag("id"));
					ctx.push(errorMsg, ctx.getACARSConnection().getID());
				} finally {
					ctx.release();
				}

				break;

			default:
				log.error("Unsupported Data Request Messasge - " + msg.getRequestType());
		}

		// Push the response
		ctx.push(dataRsp, env.getConnectionID());
	}

	/**
	 * Returns the maximum execution time of this command before a warning is issued.
	 * @return the maximum execution time in milliseconds
	 */
	@Override
	public final int getMaxExecTime() {
		return 9000;
	}
}