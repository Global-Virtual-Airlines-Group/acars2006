// Copyright 2005, 2006, 2008, 2009, 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.io.*;
import java.util.Collection;

import org.deltava.acars.beans.MessageEnvelope;

import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.ControllerMessage;

import org.deltava.beans.*;
import org.deltava.beans.servinfo.NetworkInfo;

import org.deltava.dao.file.GetServInfo;

import org.deltava.util.system.SystemData;

/**
 * An ACARS Server command to display online ATC data.
 * @author Luke
 * @version 3.6
 * @since 1.0
 */

public class ATCInfoCommand extends DataCommand {

	/**
	 * Initializes the Command.
	 */
	public ATCInfoCommand() {
		super(ATCInfoCommand.class);
	}

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	public final void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message and the network
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		OnlineNetwork network = null;
		try {
			network = OnlineNetwork.valueOf(msg.getFlag("network").toUpperCase());
			Collection<?> networkNames = (Collection<?>) SystemData.getObject("online.networks");
			if (!networkNames.contains(network.toString()))
				return;
		} catch (IllegalArgumentException iae) {
			log.warn("Unknown Online network - " + msg.getFlag("network"));
			return;
		}
		
		// Get the data
		NetworkInfo info = null;
		try {
			File f = new File(SystemData.get("online." + network.toString().toLowerCase() + ".local.info"));
			if (f.exists()) {
				GetServInfo sidao = new GetServInfo(new FileInputStream(f));
				info = sidao.getInfo(network);
			}
		} catch (Exception e) {
			log.error("Cannot load " + network + " ServInfo feed - " + e.getMessage(), e);
			return;
		}
		
		// Get the position
		GeoLocation loc = ctx.getACARSConnection().getPosition();
		if (loc == null)
			loc = SystemData.getAirport(ctx.getACARSConnection().getUser().getHomeAirport());

		// Filter the controllers based on range from position
		ControllerMessage rspMsg = new ControllerMessage(env.getOwner(), msg.getID());
		if (info != null)
			rspMsg.addAll(info.getControllers(loc));
		
		// Push the response
		ctx.push(rspMsg, env.getConnectionID());
	}
}	