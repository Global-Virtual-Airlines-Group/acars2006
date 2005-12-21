// Copyright (c) 2004, 2005 Global Virtual Airline Group. All Rights Reserved.
package org.deltava.acars.command;

import java.util.*;

import org.apache.log4j.Logger;

import org.deltava.beans.Pilot;
import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;

import org.deltava.acars.workers.NetworkReader;
import org.deltava.acars.xml.MessageWriter;

import org.deltava.util.StringUtils;
import org.deltava.util.system.SystemData;

/**
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class DiagnosticCommand implements ACARSCommand {

	private static final Logger log = Logger.getLogger(DiagnosticCommand.class);

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@SuppressWarnings("unchecked")
	public void execute(CommandContext ctx, Envelope env) {

		// Get the message and the connection pool
		DiagnosticMessage msg = (DiagnosticMessage) env.getMessage();
		ACARSConnectionPool cPool = ctx.getACARSConnectionPool();
		
		// Check user access
		Pilot usr = env.getOwner();
		if (!usr.isInRole("Admin")) {
			AcknowledgeMessage ackMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
			ackMsg.setEntry("error", "Insufficient Access");
			ctx.push(ackMsg, env.getConnectionID());
			return;
		}

		switch (msg.getRequestType()) {

			// Kick a user based on connection ID
			case DiagnosticMessage.REQ_KICK:

				// Try and get the connection
				Collection<ACARSConnection> cons = cPool.get(msg.getRequestData());
				for (Iterator<ACARSConnection> i = cons.iterator(); i.hasNext();) {
					ACARSConnection ac = i.next();
					MessageWriter.remove(ac.getID());
					cPool.remove(ac);
					log.warn("Connection " + StringUtils.formatHex(ac.getID()) + " (" + ac.getUserID() + ") KICKED by " + env.getOwnerID());

					// Send the ACK
					AcknowledgeMessage daMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
					daMsg.setEntry("cid", StringUtils.formatHex(ac.getID()));
					daMsg.setEntry("user", ac.getUserID());
					daMsg.setEntry("addr", ac.getRemoteAddr());
					ctx.push(daMsg, env.getConnectionID());
				}

				break;

			// Block an IP address or Hostname
			case DiagnosticMessage.REQ_BLOCK:

				// Get the list of blocked connections and add the address
				Collection blockedAddrs = (Collection) SystemData.getObject(NetworkReader.BLOCKADDR_LIST);
				blockedAddrs.add(msg.getRequestData());
				log.warn("Address " + msg.getRequestData() + " BLOCKED by " + env.getOwnerID());

				// Kick any connections from this address
				for (Iterator<ACARSConnection> i = cPool.getAll().iterator(); i.hasNext();) {
					ACARSConnection ac = i.next();
					if ((blockedAddrs.contains(ac.getRemoteAddr())) || (blockedAddrs.contains(ac.getRemoteHost()))) {
						MessageWriter.remove(ac.getID());
						cPool.remove(ac);
						log.warn("Connection " + StringUtils.formatHex(ac.getID()) + " (" + ac.getUserID() + ") KICKED by " + env.getOwnerID());
						
						// Send the ACK
						AcknowledgeMessage daMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
						daMsg.setEntry("cid", StringUtils.formatHex(ac.getID()));
						daMsg.setEntry("user", ac.getUserID());
						daMsg.setEntry("addr", ac.getRemoteAddr());
						ctx.push(daMsg, env.getConnectionID());
					}
				}

				break;

			// Kick the user and block his IP address
			default:
				log.error("Unsupported Diagnostic Message - " + msg.getRequestType());
		}
	}
}