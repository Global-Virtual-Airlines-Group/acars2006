// Copyright 2004, 2005, 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import java.util.*;
import java.sql.Connection;

import org.apache.log4j.Logger;

import org.deltava.beans.*;
import org.deltava.beans.system.UserData;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;

import org.deltava.acars.security.UserBlocker;

import org.deltava.dao.*;

import org.deltava.util.StringUtils;

/**
 * An ACARS server command to execute system administration tasks.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class DiagnosticCommand extends ACARSCommand {

	private static final Logger log = Logger.getLogger(DiagnosticCommand.class);

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@SuppressWarnings("unchecked")
	public void execute(CommandContext ctx, MessageEnvelope env) {

		// Get the message and the connection pool
		DiagnosticMessage msg = (DiagnosticMessage) env.getMessage();
		ACARSConnectionPool cPool = ctx.getACARSConnectionPool();
		
		// Check user access
		Pilot usr = env.getOwner();
		if (!usr.isInRole("HR")) {
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
					log.warn("Connection " + StringUtils.formatHex(ac.getID()) + " (" + ac.getUserID() + ") KICKED by " + env.getOwnerID());
					
					// Save the QUIT message
					QuitMessage qmsg = new QuitMessage(ac.getUser());
					qmsg.setFlightID(ac.getFlightID());
					MessageStack.MSG_INPUT.push(new MessageEnvelope(qmsg, ac.getID()));
					MessageStack.MSG_INPUT.wakeup(false);

					// Send the ACK
					AcknowledgeMessage daMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
					daMsg.setEntry("cid", StringUtils.formatHex(ac.getID()));
					daMsg.setEntry("user", ac.getUserID());
					daMsg.setEntry("addr", ac.getRemoteAddr());
					ctx.push(daMsg, env.getConnectionID());
					
					// Log the KICK
					StatusUpdate upd = new StatusUpdate(ac.getUser().getID(), StatusUpdate.COMMENT);
					upd.setAuthorID(usr.getID());
					upd.setDescription("Kicked from ACARS server");
					
					Connection con = null;
					try {
						con = ctx.getConnection();
						
						// Write the KICK record
						SetStatusUpdate udao = new SetStatusUpdate(con);
						udao.write(upd);
						
						// Get the pilot record
						GetPilot pdao = new GetPilot(con);
						GetUserData uddao = new GetUserData(con);
						UserData ud = uddao.get(ac.getUserData().getID());
						Pilot p = pdao.get(ud);
						
						// Restrict ACARS access
						if (p != null) {
							p.setACARSRestriction(Pilot.ACARS_RESTRICT);
							
							// Update the pilot record
							SetPilot pwdao = new SetPilot(con);
							pwdao.write(p, ud.getDB());
						} else {
							log.warn("Cannot update Pilot record for " + usr.getName());
						}
					} catch (DAOException de) {
						log.error("Cannot log KICK - " + de.getMessage(), de);
					} finally {
						ctx.release();
					}

					// Remove the connection
					cPool.remove(ac);
				}

				break;

			// Block an IP address or Hostname
			case DiagnosticMessage.REQ_BLOCK:
				log.warn("Address " + msg.getRequestData() + " BLOCKED by " + env.getOwnerID());

				// Kick any connections from this address
				for (Iterator<ACARSConnection> i = cPool.getAll().iterator(); i.hasNext();) {
					ACARSConnection ac = i.next();
					if (ac.getRemoteAddr().equals(msg.getRequestData())) {
						UserBlocker.ban(ac);
						log.warn("Connection " + StringUtils.formatHex(ac.getID()) + " (" + ac.getUserID() + ") KICKED by " + env.getOwnerID());
						
						// Save the QUIT message
						QuitMessage qmsg = new QuitMessage(ac.getUser());
						qmsg.setFlightID(ac.getFlightID());
						MessageStack.MSG_INPUT.push(new MessageEnvelope(qmsg, ac.getID()));
						MessageStack.MSG_INPUT.wakeup(false);
						
						// Send the ACK
						AcknowledgeMessage daMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
						daMsg.setEntry("cid", StringUtils.formatHex(ac.getID()));
						daMsg.setEntry("user", ac.getUserID());
						daMsg.setEntry("addr", ac.getRemoteAddr());
						ctx.push(daMsg, env.getConnectionID());
						
						// Log the BLOCK
						StatusUpdate upd = new StatusUpdate(ac.getUser().getID(), StatusUpdate.COMMENT);
						upd.setAuthorID(usr.getID());
						upd.setDescription("Kicked from ACARS server - blocked IP " + msg.getRequestData());

						Connection con = null;
						try {
							con = ctx.getConnection();
							
							// Write the KICK record
							SetStatusUpdate udao = new SetStatusUpdate(con);
							udao.write(upd);
							
							// Get the pilot record
							GetPilot pdao = new GetPilot(con);
							GetUserData uddao = new GetUserData(con);
							UserData ud = uddao.get(ac.getUserData().getID());
							Pilot p = pdao.get(ud);
							
							// Restrict ACARS access
							if (p != null) {
								p.setACARSRestriction(Pilot.ACARS_RESTRICT);
								
								// Update the pilot record
								SetPilot pwdao = new SetPilot(con);
								pwdao.write(p, ud.getDB());
							} else {
								log.warn("Cannot update Pilot record for " + usr.getName());
							}
						} catch (DAOException de) {
							log.error("Cannot log BLOCK - " + de.getMessage(), de);
						} finally {
							ctx.release();
						}
						
						// Remove the connection
						cPool.remove(ac);
					}
				}

				break;

			// Kick the user and block his IP address
			default:
				log.error("Unsupported Diagnostic Message - " + msg.getRequestType());
		}
	}
}