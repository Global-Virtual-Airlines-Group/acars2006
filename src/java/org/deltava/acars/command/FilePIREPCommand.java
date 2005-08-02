// Copyright 2005 Luke J. Kolin. All Rights Reserved.
package org.deltava.acars.command;

import java.util.*;
import java.sql.Connection;

import org.apache.log4j.Logger;

import org.deltava.beans.*;
import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;

import org.deltava.dao.*;
import org.deltava.dao.acars.SetPosition;

/**
 * An ACARS command to file a Flight Report.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */
public class FilePIREPCommand implements ACARSCommand {

	private static final Logger log = Logger.getLogger(FilePIREPCommand.class);

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	public void execute(CommandContext ctx, Envelope env) {
		
		// Log PIREP filing
		log.info("Receiving PIREP from " + env.getOwner().getName() + " (" + env.getOwnerID() + ")");
		
		// Get the Message and the ACARS connection
		FlightReportMessage msg = (FlightReportMessage) env.getMessage();
		ACARSConnection ac = ctx.getACARSConnection();

		// Get the PIREP data and flight information
		ACARSFlightReport afr = msg.getPIREP();
		InfoMessage info = (InfoMessage) ac.getInfo(ACARSConnection.FLIGHT_INFO);
		
		// Generate the response message
		AcknowledgeMessage ackMsg = new AcknowledgeMessage(ac.getUser(), msg.getID());

		Connection con = null;
		try {
			con = ctx.getConnection();

			// Search for draft Flight Reports for this city pair
			GetFlightReports prdao = new GetFlightReports(con);
			List dFlights = prdao.getDraftReports(env.getOwner().getID(), afr.getAirportD(), afr.getAirportA());

			// If we found a flight report, save its database ID and copy its ID to the PIREP we will file
			if (!dFlights.isEmpty()) {
				FlightReport fr = (FlightReport) dFlights.get(0);
				afr.setID(fr.getID());
				afr.setDatabaseID(FlightReport.DBID_ASSIGN, fr.getDatabaseID(FlightReport.DBID_ASSIGN));
				afr.setDatabaseID(FlightReport.DBID_EVENT, fr.getDatabaseID(FlightReport.DBID_EVENT));
			}

			// Check if this Flight Report counts for promotion
			GetEquipmentType eqdao = new GetEquipmentType(con);
			afr.setCaptEQType(eqdao.getPrimaryTypes(afr.getEquipmentType()));

			// Start the transaction
			con.setAutoCommit(false);
			
			// Get the position write DAO and write the positions
			if (info != null) {
			   afr.setDatabaseID(FlightReport.DBID_ACARS, info.getFlightID());
			   SetPosition pwdao = new SetPosition(con);
			   for (Iterator i = info.getPositions().iterator(); i.hasNext(); ) {
			      PositionMessage pmsg = (PositionMessage) i.next();
			      pwdao.write(pmsg, ac.getID(), info.getFlightID());
			   }
			}

			// Get the write DAO and save the PIREP
			SetFlightReport wdao = new SetFlightReport(con);
			wdao.write(afr);
			wdao.writeACARS(afr);

			// Commit the transaction
			con.commit();
		} catch (DAOException de) {
			try {
				con.rollback();
			} catch (Exception e) {
			} finally {
				log.error(de.getMessage(), de);
				ackMsg.setEntry("error", "PIREP Submission failed - " + de.getMessage());
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			ackMsg.setEntry("error", "PIREP Submission failed - " + e.getMessage());
		} finally {
			ctx.release();
		}
		
		// Send the response
		ctx.push(ackMsg, ac.getID());
	}
}