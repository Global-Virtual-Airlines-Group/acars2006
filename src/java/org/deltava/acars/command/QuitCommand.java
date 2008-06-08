// Copyright 2005, 2006, 2007, 2008 Global Virtual Airline Group. All Rights Reserved.
package org.deltava.acars.command;

import java.util.*;
import java.sql.Connection;

import org.apache.log4j.Logger;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.PilotMessage;
import org.deltava.acars.message.dispatch.CancelMessage;

import org.deltava.dao.*;
import org.deltava.dao.acars.SetInfo;

import org.deltava.util.system.SystemData;

/**
 * An ACARS command to handle disconnections by authenticated users.
 * @author Luke
 * @version 2.2
 * @since 1.0
 */

public class QuitCommand extends ACARSCommand {
   
   private static final Logger log = Logger.getLogger(QuitCommand.class);
	
	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	public void execute(CommandContext ctx, MessageEnvelope env) {
	   
	   // Get the message
	   QuitMessage msg = (QuitMessage) env.getMessage();
	   
		// Mark the flight as closed
	   if (msg.getFlightID() != 0) {
			try {
				Connection c = ctx.getConnection();
				SetInfo infoDAO = new SetInfo(c);
				infoDAO.close(msg.getFlightID(), env.getConnectionID(), false);
				
				// If Teamspeak is enabled, mark us as disconnected
				if (SystemData.getBoolean("airline.voice.ts2.enabled")) {
					log.debug("Disabled " + env.getOwnerID() + " TS2 access");
					SetTS2Data ts2wdao = new SetTS2Data(c);
					ts2wdao.setActive(env.getOwnerID(), false);
				}
			} catch (DAOException de) {
				log.error(de.getMessage(), de);
			} finally {
				ctx.release();
			}
	   }
	   
		// Create a deletepilots message
		PilotMessage drmsg = new PilotMessage(env.getOwner(), DataMessage.REQ_REMOVEUSER, msg.getID());
		drmsg.add(env.getOwner());
		drmsg.setDispatch(msg.isDispatch());
		
		// Get all connections
		Collection<ACARSConnection> cons = ctx.getACARSConnectionPool().get("*");
		if (msg.isHidden()) {
			for (Iterator<ACARSConnection> i = cons.iterator(); i.hasNext(); ) {
				ACARSConnection ac = i.next();
				if ((ac.getID() != env.getConnectionID()) && ac.isAuthenticated() && ac.getUser().isInRole("HR"))
					ctx.push(drmsg, ac.getID());
			}
		} else {
			for (Iterator<ACARSConnection> i = cons.iterator(); i.hasNext(); ) {
				ACARSConnection ac = i.next();
				if (ac.getID() != env.getConnectionID())
					ctx.push(drmsg, ac.getID());
				if (msg.isDispatch() && (ac.getDispatcherID() == env.getConnectionID()))
					ctx.push(new CancelMessage(env.getOwner()), ac.getID());
			}
		}
	}
}