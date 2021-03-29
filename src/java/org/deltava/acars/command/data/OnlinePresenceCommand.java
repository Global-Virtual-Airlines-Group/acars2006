// Copyright 2012, 2014, 2016, 2019, 2020, 2021 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import org.deltava.acars.beans.MessageEnvelope;
import org.deltava.acars.command.*;
import org.deltava.acars.message.*;

import org.deltava.beans.OnlineNetwork;
import org.deltava.beans.servinfo.*;

import org.deltava.util.*;

/**
 * An ACARS command to determine whether a user is connected to an Online Network.
 * @author Luke
 * @version 10.0
 * @since 4.1
 */

public class OnlinePresenceCommand extends DataCommand {

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {

		// Get the message and the network
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		OnlineNetwork network = EnumUtils.parse(OnlineNetwork.class, msg.getFlag("network"), null);

		// Create the response
		AcknowledgeMessage ackMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
		if (network != null) {
			final int myID = StringUtils.parse(ctx.getUser().getNetworkID(network), -1);
			NetworkInfo info = ServInfoHelper.getInfo(network);
			ackMsg.setEntry("isOnline", String.valueOf(info.getPilots().stream().anyMatch(p -> (p.getID() == myID))));
		}

		ctx.push(ackMsg);
	}
}