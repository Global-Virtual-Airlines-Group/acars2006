// Copyright 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2016, 2017, 2019, 2021, 2023 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import java.util.*;
import java.sql.Connection;

import org.apache.logging.log4j.*;

import org.deltava.beans.*;
import org.deltava.beans.acars.Restriction;
import org.deltava.beans.system.AirlineInformation;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;

import static org.deltava.acars.workers.Worker.*;

import org.deltava.dao.*;
import org.deltava.mail.*;
import org.deltava.util.MailUtils;
import org.deltava.util.StringUtils;

/**
 * An ACARS server command to execute system administration tasks.
 * @author Luke
 * @version 11.1
 * @since 1.0
 */

public class DiagnosticCommand extends ACARSCommand {

	private static final Logger log = LogManager.getLogger(DiagnosticCommand.class);

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {

		// Get the message and the connection pool
		DiagnosticMessage msg = (DiagnosticMessage) env.getMessage();
		ACARSConnectionPool cPool = ctx.getACARSConnectionPool();
		
		// Get user and create error message
		Pilot usr = env.getOwner();
		AcknowledgeMessage ackMsg = new AcknowledgeMessage(env.getOwner(), msg.getID(), "Insufficient Access");

		switch (msg.getRequestType()) {
			// Kick a user based on connection ID
			case KICK:
				if (!usr.isInRole("HR")) {
					ctx.push(ackMsg);
					return;
				}

				// Try and get the connection
				Collection<ACARSConnection> cons = new ArrayList<ACARSConnection>();
				ACARSConnection acon = cPool.get(msg.getRequestData());
				if (acon != null)
					cons.add(acon);
				else
					log.warn("Cannot kick {}", msg.getRequestData());
				
				for (Iterator<ACARSConnection> i = cons.iterator(); i.hasNext();) {
					ACARSConnection ac = i.next();
					log.warn("Connection {} ({}) KICKED by {}", StringUtils.formatHex(ac.getID()), ac.getUserID(), env.getOwnerID());
					
					// Tell the client not to reconnect
					ctx.push(msg, ac.getID(), true);
					
					// Save the QUIT message
					MSG_INPUT.add(new MessageEnvelope(new QuitMessage(ac), ac.getID()));

					// Send the ACK
					AcknowledgeMessage daMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
					daMsg.setEntry("cid", StringUtils.formatHex(ac.getID()));
					daMsg.setEntry("user", ac.getUserID());
					daMsg.setEntry("addr", ac.getRemoteAddr());
					ctx.push(daMsg);
					
					// Log the KICK
					StatusUpdate upd = new StatusUpdate(ac.getUser().getID(), UpdateType.COMMENT);
					upd.setAuthorID(usr.getID());
					upd.setDescription("Kicked from ACARS server");
					
					Connection con = null;
					try {
						con = ctx.getConnection();
						
						// Write the KICK record
						SetStatusUpdate udao = new SetStatusUpdate(con);
						udao.write(upd, ac.getUserData().getDB());
						
						// Get the pilot record
						GetPilot pdao = new GetPilot(con);
						GetUserData uddao = new GetUserData(con);
						UserData ud = uddao.get(ac.getUserData().getID());
						Pilot p = pdao.get(ud);
						
						// Restrict ACARS access
						if (p != null) {
							p.setACARSRestriction(Restriction.RESTRICT);
							SetPilot pwdao = new SetPilot(con);
							pwdao.write(p, ud.getDB());
						} else
							log.warn("Cannot update Pilot record for {}", usr.getName());
					} catch (DAOException de) {
						log.atError().withThrowable(de).log("Cannot log KICK - {}", de.getMessage());
					} finally {
						ctx.release();
					}

					// Remove the connection
					ac.close();
					cPool.remove(ac);
				}

				break;

			// Block an IP address or Hostname
			case BLOCK:
				if (!usr.isInRole("HR")) {
					ctx.push(ackMsg);
					return;
				}
				
				// Kick any connections from this address
				log.warn("Address {} BLOCKED by {}", msg.getRequestData(), env.getOwnerID());
				for (Iterator<ACARSConnection> i = cPool.getAll().iterator(); i.hasNext(); ) {
					ACARSConnection ac = i.next();
					if (ac.getRemoteAddr().equals(msg.getRequestData())) {
						log.warn("Connection {} ({}) KICKED by {}", StringUtils.formatHex(ac.getID()), ac.getUserID(), env.getOwnerID());
						
						// Tell the client not to reconnect
						ctx.push(msg, ac.getID(), true);
						
						// Save the QUIT message
						MSG_INPUT.add(new MessageEnvelope(new QuitMessage(ac), ac.getID()));
						
						// Send the ACK
						AcknowledgeMessage daMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
						daMsg.setEntry("cid", StringUtils.formatHex(ac.getID()));
						daMsg.setEntry("user", ac.getUserID());
						daMsg.setEntry("addr", ac.getRemoteAddr());
						ctx.push(daMsg);
						
						// Log the BLOCK
						StatusUpdate upd = new StatusUpdate(ac.getUser().getID(), UpdateType.COMMENT);
						upd.setAuthorID(usr.getID());
						upd.setDescription("Kicked from ACARS server - blocked IP " + msg.getRequestData());

						Connection con = null;
						try {
							con = ctx.getConnection();
							
							// Write the KICK record
							SetStatusUpdate udao = new SetStatusUpdate(con);
							udao.write(upd, ac.getUserData().getDB());
							
							// Get the pilot record
							GetPilot pdao = new GetPilot(con);
							GetUserData uddao = new GetUserData(con);
							UserData ud = uddao.get(ac.getUserData().getID());
							Pilot p = pdao.get(ud);
							
							// Restrict ACARS access
							if (p != null) {
								p.setACARSRestriction(Restriction.BLOCK);
								SetPilot pwdao = new SetPilot(con);
								pwdao.write(p, ud.getDB());
							} else
								log.warn("Cannot update Pilot record for {}", usr.getName());
						} catch (DAOException de) {
							log.atError().withThrowable(de).log("Cannot log BLOCK - {}", de.getMessage());
						} finally {
							ctx.release();
						}
						
						// Remove the connection
						cPool.remove(ac);
					}
				}

				break;

			// FIXME: Refector this into warn command!
			case WARN:
				boolean isSC = (usr.getRank() == Rank.SC) || (usr.getRank().isCP());
				if (!usr.isInRole("HR") && !usr.isInRole("Examination") && !usr.isInRole("PIREP") && !usr.isInRole("Instructor") && !isSC) {
					ctx.push(ackMsg);
					return;
				}
				
				log.warn("ACARS Content Warning from {}", env.getOwnerID());
				
				// Search for logged-in HR role members
				boolean sentMessage = false;
				for (ACARSConnection ac : cPool.getAll()) {
					if (ac.getUser().isInRole("HR") && (ac.getUser().getID() != usr.getID())) {
						sentMessage = true;
						TextMessage txtmsg = new TextMessage(usr, "ACARS Chat Content Warning - " + msg.getRequestData());
						txtmsg.setRecipient(ac.getUserID());
						ctx.push(txtmsg, ac.getID(), false);
					}
				}
				
				// Create e-mail message for all HR members
				if (!sentMessage) {
					Connection con = null;
					try {
						con = ctx.getConnection();
						
						// Get the airlines
						GetPilotDirectory pdao = new GetPilotDirectory(con);
						GetUserData uddao = new GetUserData(con);
						Collection<AirlineInformation> airlines = uddao.getAirlines(true).values();
						for (AirlineInformation ai : airlines) {
							Collection<Pilot> pilots = pdao.getByRole("HR", ai.getDB());
							for (Pilot p : pilots) {
								MessageContext mctxt = new MessageContext(ai.getCode());
								mctxt.addData("user", usr);
								mctxt.setSubject("ACARS Content Warning");
								mctxt.setBody("Potentially inappropriate content in ACARS has been reported - " + msg.getRequestData() + "\n\n${user.name}");
								
								// Send the message
								Mailer mailer = new Mailer(MailUtils.makeAddress("acars", ai.getDomain(), "ACARS"));
								mailer.setContext(mctxt);
								mailer.send(p);
							}
						}
					} catch (DAOException de) {
						log.atError().withThrowable(de).log("Cannot send content notification - {}", de.getMessage());
					} finally {
						ctx.release();
					}
				}
				
				break;
				
			default:
				log.error("Unsupported Diagnostic Message - {}", msg.getRequestType());
		}
	}
}