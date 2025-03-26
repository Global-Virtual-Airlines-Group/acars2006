// Copyright 2012, 2014, 2016, 2019, 2020, 2021 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import org.deltava.acars.beans.MessageEnvelope;
import org.deltava.acars.command.*;
import org.deltava.acars.message.*;

import org.deltava.beans.OnlineNetwork;
import org.deltava.beans.servinfo.*;

import org.deltava.dao.DAOException;
import org.deltava.dao.http.GetVATSIMData;

import org.deltava.util.*;

/**
 * An ACARS command to determine whether a user is connected to an Online Network.
 * @author Luke
 * @version 11.6
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
		final int myID = StringUtils.parse(ctx.getUser().getNetworkID(network), -1);
		
		// Check presence
		boolean isOnline = false;
		if (network != null) {
			NetworkInfo info = ServInfoHelper.getInfo(network);
			isOnline = info.getPilots().stream().anyMatch(p -> (p.getID() == myID));
		}
		
		// Call VATSIM API if we need to
		if (!isOnline && (network == OnlineNetwork.VATSIM)) {
			try {
				GetVATSIMData dao = new GetVATSIMData();
				dao.setConnectTimeout(1500);
				dao.setReadTimeout(2500);
				isOnline = dao.getOnline(String.valueOf(myID));
			} catch (DAOException de) {
				log.atError().log("Error loading VATSIM presence - {}", de.getMessage());
			}
		}

		// Create the response
		AcknowledgeMessage ackMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
		ackMsg.setEntry("isOnline", String.valueOf(isOnline));
		ctx.push(ackMsg);
	}
}