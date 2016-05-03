// Copyright 2004, 2005, 2006, 2007, 2009, 2012, 2016 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import java.util.*;

import org.apache.log4j.Logger;

import org.deltava.beans.Pilot;

import org.deltava.dao.DAOException;
import org.deltava.dao.acars.SetMessage;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;

import org.deltava.util.UserID;
import org.deltava.util.system.SystemData;

/**
 * An ACARS server command to send text messages.
 * @author Luke
 * @version 7.0
 * @since 1.0
 */

public class TextMessageCommand extends ACARSCommand {
	
	private static final Logger log = Logger.getLogger(TextMessageCommand.class);
	
	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {

		// Get the inbound message and the owner
		Pilot usr = ctx.getUser();
		TextMessage msg = (TextMessage) env.getMessage();
		if (usr == null) {
			log.warn("Attempt to send anonymous message");
			return;
		}
		
		// Check if HR or dispatch is online
		boolean hasHR = usr.isInRole("HR") || usr.isInRole("Dispatch");
		Collection<ACARSConnection> cons = ctx.getACARSConnectionPool().getAll();
		for (Iterator<ACARSConnection> i = cons.iterator(); !hasHR && i.hasNext(); ) {
			ACARSConnection ac = i.next();
			Pilot p = ac.getUser();
			if (p != null)
				hasHR |= p.isInRole("HR") || p.isInRole("Dispatch");
		}
		
		// If we have messaging restrictions on this user, apply the profanity filter
		TextMessage txtRsp = null;
		switch (usr.getACARSRestriction()) {
			case NOMSGS:
				log.warn(usr.getName() + " attempted to send message!");
				txtRsp = new TextMessage(null, "ACARS text Message blocked");
				txtRsp.setRecipient(usr.getPilotCode());
				ctx.push(txtRsp, env.getConnectionID());
				return;
				
			case RESTRICT:
				if (hasHR) break;
				log.warn(usr.getName() + " attempted to send message, no HR/Dispatch online");
				txtRsp = new TextMessage(null, "ACARS text Message blocked");
				txtRsp.setRecipient(usr.getPilotCode());
				ctx.push(txtRsp, env.getConnectionID());
				return;
			
			default:
				break;
		}
		
		// Create the outbound message
		txtRsp = new TextMessage(usr, msg.getText());
		txtRsp.setRecipient(msg.getRecipient());
		
		// Send an ACK on the message
		if (SystemData.getBoolean("acars.ack.text")) {
			AcknowledgeMessage ackMsg = new AcknowledgeMessage(usr, msg.getID());
			ctx.push(ackMsg, env.getConnectionID());
		}
		
		// Check if the recipient has a databsae ID
		UserID rcptID = new UserID(msg.getRecipient());
		boolean isDBID = !rcptID.hasAirlineCode();
		
		// Push the message back to everyone if needed
		Pilot rUsr = null;
		if (msg.isPublic()) {
			ctx.pushAll(txtRsp, env.getConnectionID());
			log.info("Public message from " + usr.getPilotCode());
		} else {
			log.info("Message from " + usr.getPilotCode() + " to " + msg.getRecipient());
			Collection<ACARSConnection> dstC = new ArrayList<ACARSConnection>();
			if (usr.isInRole("HR")) {
				ACARSConnection ac = ctx.getACARSConnection(msg.getRecipient());
				if (ac != null)
					dstC.add(ac);
			} else
				dstC.addAll(ctx.getACARSConnectionPool().getAll());

			// Send the message
			for (ACARSConnection ac: dstC) {
				Pilot p = ac.getUser();
				if (isDBID && ac.isAuthenticated() && (p.getID() == rcptID.getUserID())) {
					rUsr = p;
					ctx.push(txtRsp, ac.getID());
				} else if (ac.getUserID().equalsIgnoreCase(msg.getRecipient()))  {
					rUsr = p;
					ctx.push(txtRsp, ac.getID());
				} else if (ac.getUserHidden() && !ac.getUserBusy())
					ctx.push(txtRsp, ac.getID());
			}
		}
		
		// Write the message
		try {
			SetMessage dao = new SetMessage(ctx.getConnection());
			dao.write(msg, (rUsr == null) ? 0 : rUsr.getID());
		} catch (DAOException de) {
			log.error("Error writing text message - " + de.getMessage(), de);
		} finally {
			ctx.release();
		}
	}
}