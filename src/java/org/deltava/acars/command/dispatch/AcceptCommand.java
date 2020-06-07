// Copyright 2007, 2009, 2010, 2011, 2016, 2019 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.dispatch;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;

import org.deltava.acars.message.AcknowledgeMessage;
import org.deltava.acars.message.data.ChannelListMessage;
import org.deltava.acars.message.dispatch.*;
import org.deltava.beans.mvs.PopulatedChannel;

/**
 * An ACARS Command to accept Dispatch service requests. 
 * @author Luke
 * @version 9.0
 * @since 2.0
 */

public class AcceptCommand extends DispatchCommand {

	/**
	 * Initializes the Command.
	 */
	public AcceptCommand() {
		super(AcceptCommand.class);
	}

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message
		AcceptMessage msg = (AcceptMessage) env.getMessage();
		
		// Get the connection
		ACARSConnection ac = ctx.getACARSConnection(msg.getRecipient());
		if (ac == null) {
			log.warn("Unknown recipient ID - " + msg.getRecipient());
			return;
		}
		
		// Check dispatch status
		if ((ac.getDispatcherID() == 0) && !ac.getIsDispatch()) {
			ACARSConnection dac = ctx.getACARSConnection(env.getConnectionID());
			ac.setDispatcherID(dac.getID());
			 
			// If the dispatcher and the pilot each have voice, switch the pilot into the dispatcher's voice room
			if (dac.isVoiceEnabled() && ac.isVoiceEcho()) {
				VoiceChannels vc = VoiceChannels.getInstance();
				PopulatedChannel oldCh = vc.get(ac.getID());
				PopulatedChannel dspCh = vc.get(dac.getID());
				
				// Switch channnels if necessary
				try {
					PopulatedChannel pc = (!oldCh.equals(dspCh)) ? vc.add(ac, dspCh.getChannel().getName()) : null;
					if (pc != null) {
						ChannelListMessage clmsg = new ChannelListMessage(ac.getUser(), msg.getID());
						clmsg.setWarnings(ctx.getACARSConnectionPool().getWarnings());
						clmsg.setClearList(false);
						clmsg.add(pc);
						clmsg.add(oldCh);
						ctx.pushVoice(clmsg, -1);
					}
				} catch (SecurityException se) {
					log.warn(ac.getUser().getName() + " cannot join Voice Channel " + dspCh.getChannel().getName());
				}
			}
			
			log.info(dac.getUserID() + " accepted dispatch request from " + ac.getUserID());
			
			// Send the ACK
			AcknowledgeMessage ackMsg = new AcknowledgeMessage(env.getOwner(), msg.getParentID());
			ackMsg.setEntry("dispatcher", env.getOwnerID());
			ackMsg.setEntry("dispatcherID", String.valueOf(env.getOwner().getID()));
			ctx.push(ackMsg, ac.getID(), true);
		} else if (ac.getDispatcherID() != 0)
			log.info(ac.getUserID() + " already has dispatch service");
		
		// Send a cancel message to all other dispatchers
		CancelMessage cMsg = new CancelMessage(ac.getUser());
		ctx.pushDispatch(cMsg, ac.getDispatcherID());
	}
}