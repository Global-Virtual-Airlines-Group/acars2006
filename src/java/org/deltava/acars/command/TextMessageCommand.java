// Copyright 2004, 2005, 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import java.util.*;

import org.apache.log4j.Logger;

import org.deltava.beans.Pilot;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;

import org.deltava.acars.util.MessageCache;

import org.deltava.util.*;
import org.deltava.util.system.SystemData;

/**
 * An ACARS server command to send text messages.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class TextMessageCommand extends ACARSCommand {
	
	public static final MessageCache<TextMessage> CACHE = new MessageCache<TextMessage>(10, 30000);

	private static final Logger log = Logger.getLogger(TextMessageCommand.class);
	
	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	public void execute(CommandContext ctx, MessageEnvelope env) {

		// Get the inbound message and the owner
		Pilot usr = env.getOwner();
		TextMessage msg = (TextMessage) env.getMessage();
		if (usr == null) {
			log.warn("Attempt to send anonymous message");
			return;
		}
			
		// If we have messaging restrictions on this user, apply the profanity filter
		switch (usr.getACARSRestriction()) {
			case Pilot.ACARS_NOMSGS:
				log.warn(usr.getName() + " attempted to send message!");
				return;
				
			case Pilot.ACARS_RESTRICT:
				if (ProfanityFilter.flag(msg.getText())) {
					log.warn("Questionable content received from " + usr.getName());
					msg.setText(ProfanityFilter.filter(msg.getText()));
				}
		}
		
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
			Collection<ACARSConnection> dstC = ctx.getACARSConnections(msg.getRecipient());
			for (Iterator<ACARSConnection> i = dstC.iterator(); i.hasNext(); ) {
				ACARSConnection ac = i.next();
				log.info("Message from " + usr.getPilotCode() + " to " + ac.getUserID());
				rUsr = ac.getUser();
				ctx.push(txtRsp, ac.getID());
			}
		}
		
		// Cache the message
		CACHE.push(msg, env.getConnectionID(), (rUsr == null) ? 0 : rUsr.getID());
	}
}