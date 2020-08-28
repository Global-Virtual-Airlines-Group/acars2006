// Copyright 2005, 2006, 2008, 2009, 2011, 2014, 2016, 2019, 2020 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.util.Collection;

import org.deltava.acars.beans.MessageEnvelope;
import org.deltava.acars.command.*;

import org.deltava.acars.message.*;
import org.deltava.acars.message.data.ControllerMessage;

import org.deltava.beans.*;
import org.deltava.beans.servinfo.*;

import org.deltava.util.system.SystemData;

/**
 * An ACARS Server command to display online ATC data.
 * @author Luke
 * @version 9.1
 * @since 1.0
 */

public class ATCInfoCommand extends DataCommand {

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public final void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message and the network
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		OnlineNetwork network = OnlineNetwork.fromName(msg.getFlag("network"));
		Collection<?> networkNames = (Collection<?>) SystemData.getObject("online.networks");
		if (!networkNames.contains(network.toString()))
			return;
		
		// Get the data
		NetworkInfo info = ServInfoHelper.getInfo(network);
		
		// Get the position
		GeoLocation loc = ctx.getACARSConnection().getPosition();
		if (loc == null)
			loc = SystemData.getAirport(ctx.getACARSConnection().getUser().getHomeAirport());

		// Filter the controllers based on range from position
		ControllerMessage rspMsg = new ControllerMessage(env.getOwner(), msg.getID());
		rspMsg.setNetwork(network);
		rspMsg.addAll(info.getControllers(loc));
		ctx.push(rspMsg);
	}
}	