// Copyright 2004, 2005, 2006, 2007, 2009, 2012, 2016, 2019, 2020, 2021, 2023 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import java.util.*;

import org.apache.logging.log4j.*;

import org.deltava.beans.*;
import org.deltava.dao.DAOException;
import org.deltava.dao.acars.SetMessage;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;

import org.deltava.util.UserID;

/**
 * An ACARS server command to send text messages.
 * @author Luke
 * @version 11.1
 * @since 1.0
 */

public class TextMessageCommand extends ACARSCommand {
	
	private static final Logger log = LogManager.getLogger(TextMessageCommand.class);
	
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
		Collection<ACARSConnection> cons = ctx.getACARSConnectionPool().getAll(ac -> ac.isAuthenticated());
		hasHR |= cons.stream().anyMatch(ac -> ac.getUser().isInRole("HR") || ac.getUser().isInRole("Dispatch"));
		
		// If we have messaging restrictions on this user, apply the profanity filter
		TextMessage txtRsp = null;
		switch (usr.getACARSRestriction()) {
			case NOMSGS:
				log.warn("{} attempted to send message", usr.getName());
				txtRsp = new TextMessage(null, "ACARS text Message blocked");
				txtRsp.setRecipient(usr.getPilotCode());
				ctx.push(txtRsp);
				return;
				
			case RESTRICT:
				if (hasHR) break;
				log.warn("{} attempted to send message, no HR/Dispatch online", usr.getName());
				txtRsp = new TextMessage(null, "ACARS text Message blocked");
				txtRsp.setRecipient(usr.getPilotCode());
				ctx.push(txtRsp);
				return;
			
			default:
				break;
		}
		
		// Create the outbound message
		txtRsp = new TextMessage(usr, msg.getText());
		txtRsp.setRecipient(msg.getRecipient());
		
		// Check if the recipient has a databsae ID
		UserID rcptID = new UserID(msg.getRecipient());
		boolean isDBID = !rcptID.hasAirlineCode();
		
		// Push the message back to everyone if needed
		Pilot rUsr = null;
		if (msg.isPublic()) {
			log.info("Public message from {}", usr.getPilotCode());
			UserData usrLoc = ctx.getACARSConnection().getUserData();
			for (ACARSConnection ac : cons) {
				UserData ud = ac.getUserData();
				if (ud.getAirlineCode().equals(usrLoc.getAirlineCode()) && (ud.getID() != usrLoc.getID()))
					ctx.push(txtRsp, ac.getID(), false);
			}
		} else {
			log.info("Message from {} to {}", usr.getPilotCode(), msg.getRecipient());
			Collection<ACARSConnection> dstC = new ArrayList<ACARSConnection>();
			if (usr.isInRole("HR")) {
				ACARSConnection ac = ctx.getACARSConnection(msg.getRecipient());
				if (ac != null)
					dstC.add(ac);
			} else
				dstC.addAll(cons);

			// Send the message
			for (ACARSConnection ac: dstC) {
				Pilot p = ac.getUser();
				if (isDBID && (p.getID() == rcptID.getUserID())) {
					rUsr = p;
					ctx.push(txtRsp, ac.getID(), false);
				} else if (ac.getUserID().equalsIgnoreCase(msg.getRecipient()))  {
					rUsr = p;
					ctx.push(txtRsp, ac.getID(), false);
				} else if (ac.getUserHidden() && !ac.getUserBusy())
					ctx.push(txtRsp, ac.getID(), false);
			}
		}
		
		// Send an ACK on the message
		AcknowledgeMessage ackMsg = new AcknowledgeMessage(usr, msg.getID());
		ctx.push(ackMsg);
		
		// Write the message
		try {
			SetMessage dao = new SetMessage(ctx.getConnection());
			dao.write(msg, (rUsr == null) ? 0 : rUsr.getID());
		} catch (DAOException de) {
			log.atError().withThrowable(de).log("Error writing text message - {}", de.getMessage());
		} finally {
			ctx.release();
		}
	}
}