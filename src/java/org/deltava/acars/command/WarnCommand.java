// Copyright 2010, 2011, 2019, 2021, 2023 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import java.util.Collection;
import java.sql.Connection;
import java.util.stream.Collectors;

import org.apache.logging.log4j.*;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;

import org.deltava.beans.*;
import org.deltava.beans.acars.*;

import org.deltava.dao.*;
import org.deltava.dao.acars.SetWarning;

import org.deltava.util.system.SystemData;

/**
 * An ACARS Command to modify a user's warning level. 
 * @author Luke
 * @version 11.1
 * @since 4.0
 */

public class WarnCommand extends ACARSCommand {
	
	private static final Logger log = LogManager.getLogger(WarnCommand.class);
	
	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {

		// Get the message
		WarnMessage msg = (WarnMessage) env.getMessage();
		
		// Load the user
		ACARSConnection ac = ctx.getACARSConnection(msg.getRecipient());
		if (ac == null) {
			log.warn("Unknown user - {}", msg.getRecipient());
			ctx.push(new AcknowledgeMessage(ctx.getUser(), msg.getID(), "Unknown user - " + msg.getRecipient()));
			return;
		}
		
		// Create the status update
		int warnScore = WarningScorer.score(msg.getSender());
		StatusUpdate su = new StatusUpdate(ac.getUser().getID(), UpdateType.CONTENT_WARN);
		su.setAuthorID(env.getOwner().getID());
		
		// Update the warning count - block the user after a certain level of warnings
		int maxWarn = SystemData.getInt("acars.maxWarnings", 10); boolean disableVoiceText = false;
		synchronized (ac) {
			boolean isMuted = ac.getMuted() || (ac.getUser().getACARSRestriction() == Restriction.NOMSGS);
			int warnings = msg.isReset() ? 0 : ac.getWarningScore() + warnScore;
			ac.setWarningScore(warnings);
			if (warnings >= maxWarn) {
				su.setDescription("Voice/Text disabled after " + warnings + " content warning score");
				log.warn(ac.getUser().getName() + " " + su.getDescription());
				ac.setMuted(true);
				ac.getUser().setACARSRestriction(Restriction.NOMSGS);
				disableVoiceText = true;
			} else if (isMuted && msg.isReset()) {
				log.info("Reset warnings for " + ac.getUserID());
				su.setDescription("Warning level reset");
				ac.setMuted(false);
				ac.getUser().setACARSRestriction(Restriction.RESTRICT);
			} else
				su.setDescription("Sent voice content warning (" + warnScore + " pts)");
		}

		try {
			Connection con = ctx.getConnection();
			ctx.startTX();
			
			// Write the status update
			SetStatusUpdate sudao = new SetStatusUpdate(con);
			sudao.write(su, ctx.getDB());
			
			// Log the warning and update the Pilot
			SetPilot pwdao = new SetPilot(con);
			SetWarning wwdao = new SetWarning(con);
			if (msg.isReset()) {
				wwdao.clear(ac.getUser().getID());
			} else {
				wwdao.warn(ac.getUser().getID(), env.getOwner().getID(), warnScore);
				if (disableVoiceText)
					pwdao.write(ac.getUser(), ac.getUserData().getDB());
			}
			
			ctx.commitTX();
		} catch (DAOException de) {
			log.atError().withThrowable(de).log("Error writing user warning - {}", de.getMessage());
			ctx.rollbackTX();
			AcknowledgeMessage ackMsg = new AcknowledgeMessage(ctx.getUser(), msg.getID());
			ackMsg.setEntry("error", de.getMessage());
		} finally {
			ctx.release();
		}
		
		// Send user notification
		SystemTextMessage notifyMsg = new SystemTextMessage();
		if (!msg.isReset()) {
			notifyMsg.setWarning(true);
			notifyMsg.addMessage("You have received an ACARS content warning");
			if (disableVoiceText)
				notifyMsg.addMessage("ACARS voice/text messaging disabled");
		} else
			notifyMsg.addMessage("ACARS content warnings cleared");
		
		ctx.push(notifyMsg, ac.getID(), true);
		
		// Send notification messages
		Collection<ACARSConnection> aCons = ctx.getACARSConnectionPool().getAll().stream().filter(WarnCommand::filter).collect(Collectors.toList());
		SystemTextMessage nMsg = new SystemTextMessage();
		nMsg.addMessage(ac.getUser().getName() + " received ACARS content warning from " + msg.getSender().getName() + " ( " + warnScore + " pts)");
		aCons.forEach(c -> ctx.push(nMsg, c.getID(), false));
	}
	
	private static boolean filter(ACARSConnection ac) {
		if (!ac.isAuthenticated()) return false;
		
		Pilot p = ac.getUser();
		return p.isInRole("HR") || p.isInRole("Operations") || p.isInRole("Dispatch") || (p.getRank().ordinal() >= Rank.ACP.ordinal());
	}
}