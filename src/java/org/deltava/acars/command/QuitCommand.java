// Copyright (c) 2005 Luke J. Kolin. All Rights Reserved.
package org.deltava.acars.command;

import java.util.Date;
import java.sql.Connection;

import org.apache.log4j.Logger;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;

import org.deltava.dao.DAOException;
import org.deltava.dao.acars.SetInfo;

/**
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class QuitCommand implements ACARSCommand {
   
   private static final Logger log = Logger.getLogger(QuitCommand.class);
	
	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	public void execute(CommandContext ctx, Envelope env) {
	   
	   // Get the ACARS connection and flight information
	   ACARSConnection ac = ctx.getACARSConnection();
	   InfoMessage info = ac.getFlightInfo();
	   
		// Mark the flight as closed
	   if ((info != null) && (info.getEndTime() == null)) {
	      info.setEndTime(new Date());

			try {
				Connection c = ctx.getConnection();
				SetInfo infoDAO = new SetInfo(c);
				infoDAO.close(info.getFlightID(), env.getConnectionID(), true);
			} catch (DAOException de) {
				log.error(de.getMessage(), de);
			} finally {
				ctx.release();
			}
			
			// Clear flight info and log
			log.info("Flight Completed by " + ac.getUserID());
	   }

		// Create a deletepilots message
		DataResponseMessage msg = new DataResponseMessage(env.getOwner(), DataMessage.REQ_REMOVEUSER);
		msg.addResponse(env.getOwner());
		
		// Send to everyone except ourself
		ctx.pushAll(msg, env.getConnectionID());
	}
}