// Copyright 2005 Luke J. Kolin. All Rights Reserved.
package org.deltava.acars.command;

import java.util.*;
import java.sql.Connection;

import org.apache.log4j.Logger;

import org.deltava.beans.*;
import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;

import org.deltava.dao.*;
import org.deltava.dao.acars.SetInfo;
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

      // Get the Message and the ACARS connection
      FlightReportMessage msg = (FlightReportMessage) env.getMessage();
		ACARSConnection ac = ctx.getACARSConnection();
      
      // Get the PIREP data
      ACARSFlightReport afr = msg.getPIREP();

      Connection con = null;
      try {
         con = ctx.getConnection();

         // If we were offline, write the Info Data
         if (msg.isOffline() && (msg.getInfo() != null)) {
            ac.setInfo(msg.getInfo());
            
            SetInfo infoDAO = new SetInfo(con);
   			infoDAO.write(msg.getInfo(), env.getConnectionID());
   			afr.setDatabaseID(FlightReport.DBID_ACARS, msg.getInfo().getFlightID());
   			log.info("Received info from " + env.getOwnerID() + " (ID: " + msg.getInfo().getFlightID() + ")");
         }
         
         // If we were offline, write the position data
         if (msg.isOffline() && (!msg.getPositions().isEmpty())) {
            SetPosition posDAO = new SetPosition(con);
            for (Iterator i = msg.getPositions().iterator(); i.hasNext(); ) {
               PositionMessage pmsg = (PositionMessage) i.next();
               posDAO.write(pmsg, env.getConnectionID(), msg.getInfo().getFlightID());
               if (!i.hasNext())
                  ac.setInfo(pmsg);
            }
            
            log.info("Saved " + msg.getPositions().size() + " Position Data records");
         }
         
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
			
			// Get the write DAO and save the PIREP
			SetFlightReport wdao = new SetFlightReport(con);
			wdao.write(afr);
			wdao.writeACARS(afr);

			// Commit the transaction
			con.commit();
			
			// Create the ack message and envelope
			AcknowledgeMessage ackMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
			if (msg.getInfo() != null)
				ackMsg.setEntry("flight_id", String.valueOf(msg.getInfo().getFlightID()));
			
			ctx.push(ackMsg, env.getConnectionID());						
      } catch (DAOException de) {
         try {
            con.rollback();
         } catch (Exception e) {
         } finally {
            log.error(de.getMessage(), de);
         }
      } catch (Exception e) {
         log.error(e.getMessage(), e);
      } finally {
         ctx.release();
      }
   }
}