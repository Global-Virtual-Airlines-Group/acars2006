// Copyright 2012, 2014 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import org.deltava.acars.beans.MessageEnvelope;
import org.deltava.acars.command.*;
import org.deltava.acars.message.*;

import org.deltava.beans.OnlineNetwork;
import org.deltava.beans.servinfo.*;

import org.deltava.util.StringUtils;

/**
 * An ACARS command to determine whether a user is connected to an Online Network.
 * @author Luke
 * @version 5.4
 * @since 4.1
 */

public class OnlinePresenceCommand extends DataCommand {

	/**
	 * Initializes the Command.
	 */
	public OnlinePresenceCommand() {
		super(OnlinePresenceCommand.class);
	}

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {

		// Get the message and the network
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		OnlineNetwork network = null;
		try {
			network = OnlineNetwork.valueOf(msg.getFlag("network").toUpperCase());
		} catch (IllegalArgumentException iae) {
			log.warn("Unknown Online network - " + msg.getFlag("network"));
		}

		// Create the response
		AcknowledgeMessage ackMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
		if (network != null) {
			int myID = StringUtils.parse(ctx.getUser().getNetworkID(network), -1);
			NetworkInfo info = ServInfoHelper.getInfo(network);

			// Check for the user
			for (Pilot p : info.getPilots()) {
				if (p.getID() == myID) {
					ackMsg.setEntry("isOnline", "true");
					break;
				}
			}
		}

		ctx.push(ackMsg, ctx.getACARSConnection().getID());
	}
}