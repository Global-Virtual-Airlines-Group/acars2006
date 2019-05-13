// Copyright 2010, 2011, 2019 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import java.sql.Connection;

import org.apache.log4j.Logger;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.ChannelListMessage;

import org.deltava.beans.StatusUpdate;
import org.deltava.beans.mvs.PopulatedChannel;

import org.deltava.dao.*;
import org.deltava.dao.acars.SetWarning;

import org.deltava.util.system.SystemData;

/**
 * An ACARS Command to modify a user's warning level. 
 * @author Luke
 * @version 8.6
 * @since 4.0
 */

public class WarnCommand extends ACARSCommand {
	
	private static final Logger log = Logger.getLogger(WarnCommand.class);
	
	private final VoiceChannels _vc = VoiceChannels.getInstance();

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
		ACARSConnection avc = ctx.getACARSConnection(msg.getRecipient());
		if (avc == null) {
			log.warn("Unknown user - " + msg.getRecipient());
			AcknowledgeMessage ackMsg = new AcknowledgeMessage(ctx.getUser(), msg.getID());
			ackMsg.setEntry("error", "Unknown user - " + msg.getRecipient());
			ctx.push(ackMsg);
			return;
		}
		
		// Create the status update
		StatusUpdate su = new StatusUpdate(avc.getUser().getID(), StatusUpdate.VOICE_WARN);
		su.setAuthorID(env.getOwner().getID());
		
		// Update the warning count - block the user after a certain level of warnings
		int maxWarn = SystemData.getInt("acars.voice.maxWarnings", 3);
		synchronized (avc) {
			int warnings = msg.isReset() ? 0 : avc.getWarnings() + 1;
			avc.setWarnings(warnings);
			if (warnings >= maxWarn) {
				su.setDescription("Voice disabled after " + warnings + " content warnings");
				log.warn(avc.getUser().getName() + " " + su.getDescription());
				avc.setMuted(true);
			} else if (avc.getMuted() && msg.isReset()) {
				log.info("Reset warnings for " + avc.getUserID());
				su.setDescription("Warning level reset");
				avc.setMuted(false);
			} else
				su.setDescription("Sent voice content warning");
		}

		try {
			Connection con = ctx.getConnection();
			ctx.startTX();
			
			// Write the status update
			SetStatusUpdate sudao = new SetStatusUpdate(con);
			sudao.write(avc.getUserData().getDB(), su);
			
			// Log the warning 
			SetWarning wdao = new SetWarning(con);
			if (msg.isReset())
				wdao.clear(avc.getUser().getID());
			else
				wdao.warn(avc.getUser().getID(), env.getOwner().getID());
			
			ctx.commitTX();
		} catch (DAOException de) {
			ctx.rollbackTX();
			AcknowledgeMessage ackMsg = new AcknowledgeMessage(ctx.getUser(), msg.getID());
			log.error("Error writing user warning - " + de.getMessage(), de);
			ackMsg.setEntry("error", de.getMessage());
		} finally {
			ctx.release();
		}
		
		// Send a channel list message to all voice users
		PopulatedChannel pc = _vc.get(avc.getID());
		ChannelListMessage clmsg = new ChannelListMessage(env.getOwner(), msg.getID());
		clmsg.setWarnings(ctx.getACARSConnectionPool().getWarnings());
		clmsg.setClearList(false);
		clmsg.add(pc);
		ctx.pushVoice(clmsg, -1);
	}
}