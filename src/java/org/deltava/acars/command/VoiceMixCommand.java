// Copyright 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import java.io.IOException;

import org.apache.log4j.Logger;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.VoiceMessage;
import org.deltava.acars.workers.Worker;

import org.deltava.beans.mvs.PopulatedChannel;
import org.deltava.beans.schedule.GeoPosition;

/**
 * An ACARS command to mix voice messages.
 * @author Luke
 * @version 4.0
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
		
		// Parse the voice message
		try {
			Packet.parse(vmsg);
			if (vmsg.getLocation() == null)
				vmsg.setLocation(ac.getMPLocation());
		} catch (IOException ie) {
			log.warn(ie.getMessage());
			//return;
		}
		
		// Make sure this is greater than the max seq for the connection
		synchronized (ac) {
			if (ac.getVoiceSequence() >= vmsg.getID()) {
				log.warn("Out of sequence voice packet from " + ac.getUserID() + ", " + ac.getVoiceSequence() + " >= " + vmsg.getID());
				return;
			}
				
			ac.setVoiceSequence(vmsg.getID());
		}
		
		// Get the channel
		PopulatedChannel pc = vc.get(ac.getID());
		if (pc == null) {
			log.warn(ac.getUserID() + " no longer in any Voice Channel");
			return;
		}
		
		// Check if we're in range of the channel
		int maxRange = pc.getChannel().getRange();
		GeoPosition ctr = (maxRange > 0) ? new GeoPosition(pc.getChannel().getCenter()) : null;
		if (maxRange > 0) {
			int myDistance = ctr.distanceTo(vmsg.getLocation());
			if (myDistance > maxRange) {
				log.info(ac.getUserID() + " out of range of " + pc.getChannel().getName());
				return;
			} else if (myDistance == -1) {
				log.info(ac.getUserID() + " no location for " + pc.getChannel().getName());
				return;
			}
		}
		
		// Loop through the connection IDs, sending if in range of center
		ACARSConnectionPool pool = ctx.getACARSConnectionPool();
		for (Long ID : pc.getConnectionIDs()) {
			ACARSConnection avc = pool.get(ID.longValue());
			if ((avc == null) || !avc.isVoiceEnabled())
				continue;
			
			// Check for range limitations
			if (maxRange > 0) {
				int rcvDistance = ctr.distanceTo(avc.getLocation());
				if (rcvDistance <= maxRange) {
					BinaryEnvelope oenv = new BinaryEnvelope(vmsg.getSender(), vmsg.getData(), avc.getID());
					Worker.RAW_OUTPUT.add(oenv);
				}
			} else {
				BinaryEnvelope oenv = new BinaryEnvelope(vmsg.getSender(), vmsg.getData(), avc.getID());
				Worker.RAW_OUTPUT.add(oenv);
			}
		}
	}
}