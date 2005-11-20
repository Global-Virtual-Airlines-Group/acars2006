// Copyright (c) 2005 Luke J. Kolin. All Rights Reserved.
package org.deltava.acars.command;

import java.util.*;
import java.net.HttpURLConnection;

import java.sql.Connection;

import org.apache.log4j.Logger;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;

import org.deltava.beans.schedule.*;
import org.deltava.beans.servinfo.*;
import org.deltava.beans.navdata.*;

import org.deltava.comparators.AirportComparator;

import org.deltava.dao.*;
import org.deltava.dao.http.GetServInfo;

import org.deltava.util.system.SystemData;

/**
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class DataCommand implements ACARSCommand {

	private static final Logger log = Logger.getLogger(DataCommand.class);

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	public void execute(CommandContext ctx, Envelope env) {

		// Get the message
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		ACARSConnection ac = ctx.getACARSConnection();

		// Get the connections to process
		Iterator i = ctx.getACARSConnections(msg.getFilter()).iterator();

		// Create the response
		DataResponseMessage dataRsp = new DataResponseMessage(env.getOwner(), msg.getRequestType());
		log.info("Data Request (" + DataMessage.REQ_TYPES[msg.getRequestType()] + ") from " + env.getOwnerID());

		switch (msg.getRequestType()) {
			// Get all of the pilot info stuff
			case DataMessage.REQ_PLIST:
				while (i.hasNext()) {
					ACARSConnection c = (ACARSConnection) i.next();
					dataRsp.addResponse(c.getPosition());
				}

				break;

			// Get Pilot/position info
			case DataMessage.REQ_USRLIST:
			case DataMessage.REQ_PILOTINFO:
				while (i.hasNext())
					dataRsp.addResponse(i.next());

				break;

			// Get equipment list
			case DataMessage.REQ_EQLIST:
				Set<String> eqTypes = new TreeSet<String>((Collection<? extends String>) SystemData.getObject("eqtypes"));
				dataRsp.addResponse("eqtype", eqTypes);
				break;

			// Get airline list
			case DataMessage.REQ_ALLIST:
				Map airlines = (Map) SystemData.getObject("airlines");
				for (i = airlines.values().iterator(); i.hasNext();) {
					Airline a = (Airline) i.next();
					dataRsp.addResponse(a.getCode(), a.getName());
				}

				break;

			// Get airport list
			case DataMessage.REQ_APLIST:
				Map allAirports = (Map) SystemData.getObject("airports");
				Set<Object> airports = new TreeSet<Object>(new AirportComparator(AirportComparator.NAME));
				airports.addAll(allAirports.values());
				for (i = airports.iterator(); i.hasNext();) {
					Airport a = (Airport) i.next();
					dataRsp.addResponse(a);
				}

				break;

			// Get private voice info
			case DataMessage.REQ_PVTVOX:
				dataRsp.addResponse("url", SystemData.get("airline.voice.url"));
				break;
				
			// Get controller info
			case DataMessage.REQ_ATCINFO:
				String network = msg.getFlag("network").toLowerCase();
				NetworkInfo info = null;
				try {
					// Connect to info URL
					HttpURLConnection urlcon = ctx.getURL(SystemData.get("online." + network + ".status_url"));

					// Get network URLs
					GetServInfo sdao = new GetServInfo(urlcon);
					NetworkStatus status = sdao.getStatus(network);
					urlcon.disconnect();
					
					// Get network status
					urlcon = ctx.getURL(status.getDataURL());
					GetServInfo idao = new GetServInfo(urlcon);
					idao.setBufferSize(40960);
					info = idao.getInfo(network);
					urlcon.disconnect();
				} catch (Exception e) {
					log.warn("Error retrieving " + network.toUpperCase() + " data - " + e.getMessage());
				}
				
				// Filter the controllers based on rante from position
				if ((info != null) && (ac.getPosition() != null)) {
					Collection<Controller> ctrs = info.getControllers(ac.getPosition(), 400);
					for (Iterator<Controller> ci = ctrs.iterator(); ci.hasNext(); )
						dataRsp.addResponse(ci.next());
				}
					
				break;

			// Get flight information
			case DataMessage.REQ_ILIST:
				while (i.hasNext()) {
					ACARSConnection c = (ACARSConnection) i.next();
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
					Collection charts = dao.getCharts(a);
					for (Iterator ci = charts.iterator(); ci.hasNext();)
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
					AcknowledgeMessage errMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
					errMsg.setEntry("error", "Cannot load navaid " + msg.getFlag("id"));
					ctx.push(errMsg, ctx.getACARSConnection().getID());
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
}