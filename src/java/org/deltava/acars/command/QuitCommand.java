// Copyright (c) 2005 Luke J. Kolin. All Rights Reserved.
package org.deltava.acars.command;

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
	   
	   // Get the message
	   QuitMessage msg = (QuitMessage) env.getMessage();
	   
		// Mark the flight as closed
	   if (msg.getFlightID() != 0) {
			try {
				Connection c = ctx.getConnection();
				SetInfo infoDAO = new SetInfo(c);
				infoDAO.close(msg.getFlightID(), env.getConnectionID(), false);
			} catch (DAOException de) {
				log.error(de.getMessage(), de);
			} finally {
				ctx.release();
			}
	   }

		// Create a deletepilots message
		DataResponseMessage drmsg = new DataResponseMessage(env.getOwner(), DataMessage.REQ_REMOVEUSER);
		drmsg.addResponse(env.getOwner());
		
		// Send to everyone except ourself
		ctx.pushAll(msg, env.getConnectionID());
	}
}