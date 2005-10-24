// Copyright (c) 2005 Luke J. Kolin. All Rights Reserved.
package org.deltava.acars.command;

import java.util.*;

import org.apache.log4j.Logger;

import org.deltava.beans.Pilot;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;

import org.deltava.acars.util.TextMessageCache;

import org.deltava.util.system.SystemData;

/**
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class TextMessageCommand implements ACARSCommand {

	private static final Logger log = Logger.getLogger(TextMessageCommand.class);
	
	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	public void execute(CommandContext ctx, Envelope env) {

		// Get the inbound message and the owner
		Pilot usr = env.getOwner();
		TextMessage msg = (TextMessage) env.getMessage();
		
		// Create the outbound message
		TextMessage txtRsp = new TextMessage(usr, msg.getText());
		txtRsp.setRecipient(msg.getRecipient());
		
		// Send an ACK on the message
		if (SystemData.getBoolean("acars.ack.text")) {
			AcknowledgeMessage ackMsg = new AcknowledgeMessage(usr, msg.getID());
			ctx.push(ackMsg, env.getConnectionID());
		}
		
		// Push the message back to everyone if needed
		Pilot rUsr = null;
		if (msg.isPublic()) {
			ctx.pushAll(txtRsp, env.getConnectionID());
			log.info("Public message from " + usr.getPilotCode());
		} else {
			Collection dstC = ctx.getACARSConnections(msg.getRecipient());
			for (Iterator i = dstC.iterator(); i.hasNext(); ) {
				ACARSConnection ac = (ACARSConnection) i.next();
				log.info("Message from " + usr.getPilotCode() + " to " + ac.getUserID());
				rUsr = ac.getUser();
				ctx.push(txtRsp, ac.getID());
			}
		}
		
		// Cache the message
		TextMessageCache.push(msg, env.getConnectionID(), (rUsr == null) ? 0 : rUsr.getID());
	}
}