// Copyright 2011, 2014, 2019 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import java.io.IOException;

import org.apache.log4j.Logger;

import org.deltava.beans.mvs.*;
import org.deltava.acars.beans.*;
import org.deltava.acars.message.VoiceMessage;

import org.deltava.acars.mvs.*;
import org.deltava.acars.workers.Worker;

import org.deltava.util.RoleUtils;

/**
 * An ACARS command to mix voice messages.
 * @author Luke
 * @version 8.6
 * @since 4.0
 */

public class VoiceMixCommand extends ACARSCommand {
	
	private static final Logger log = Logger.getLogger(VoiceMixCommand.class);
	
	private final VoiceChannels vc = VoiceChannels.getInstance();

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {

		// Get the message
		VoiceMessage vmsg = (VoiceMessage) env.getMessage();
		ACARSConnection ac = ctx.getACARSConnection();
		if (ac.getMuted())
			return;
		
		// Parse the voice message
		Packet pkt = null;
		try {
			pkt = Packet.parse(vmsg.getData());
			if (pkt == null) return;
			pkt.setConnectionID(ac.getID());
			pkt.setUserID(ac.getUserID());
			if (pkt.getLocation() == null)
				pkt.setLocation(ac.getMPLocation());
		} catch (IOException ie) {
			log.warn(ie.getMessage());
			return;
		}
		
		// Make sure this is greater than the max seq for the connection
		synchronized (ac) {
			if (ac.getVoiceSequence() >= (pkt.getID() + 2))
				log.warn("Out of sequence voice packet from " + ac.getUserID() + ", " + ac.getVoiceSequence() + " >= " + vmsg.getID());
			else
				ac.setVoiceSequence(pkt.getID());
		}
		
		// Get the channel
		PopulatedChannel pc = vc.get(ac.getID());
		if (pc == null) {
			log.warn(ac.getUserID() + " no longer in any Voice Channel");
			return;
		}
		
		// Check talk access
		Channel ch = pc.getChannel();
		boolean canTalk = RoleUtils.hasAccess(ac.getUser().getRoles(), ch.getTalkRoles());
		canTalk |= RoleUtils.hasAccess(pc.getRolesPresent(), ch.getDynTalkRoles());
		if (!canTalk) {
			log.warn(ac.getUserID() + " attempting to talk in " + ch.getName());
			return;
		}
		
		// Add the connection ID to the packet
		byte[] pktData = Packet.rewrite(pkt);
		
		// Check if we're in range of the channel
		int maxRange = pc.getChannel().getRange();
		ACARSConnectionPool pool = ctx.getACARSConnectionPool();
		for (Long ID : pc.getConnectionIDs()) {
			ACARSConnection avc = pool.get(ID.longValue());
			if ((avc == null) || !avc.isVoiceEnabled())
				continue;
			else if ((avc.getID() == ac.getID()) && !ac.isVoiceEcho())
				continue;
			
			// Check for range limitations
			if ((maxRange > 0) && (pkt.getLocation() != null)) {
				int rcvDistance = pkt.getLocation().distanceTo(avc.getLocation());
				if (rcvDistance > maxRange) {
					if (log.isDebugEnabled())
						log.debug(avc.getUserID() + " out of range: " + rcvDistance + " > " + maxRange);
					
					continue;
				}
			}
			
			BinaryEnvelope oenv = new BinaryEnvelope(vmsg.getSender(), pktData, avc.getID());
			Worker.RAW_OUTPUT.add(oenv);
		}
	}
}