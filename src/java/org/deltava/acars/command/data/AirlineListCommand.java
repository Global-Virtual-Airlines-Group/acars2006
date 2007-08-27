// Copyright 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.util.*;

import org.deltava.acars.beans.MessageEnvelope;

import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.AirlineMessage;

import org.deltava.beans.UserData;
import org.deltava.beans.schedule.Airline;

import org.deltava.util.system.SystemData;

/**
 * An ACARS data command to return available Airline data.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class AirlineListCommand extends DataCommand {

	/**
	 * Initializes the Command.
	 */
	public AirlineListCommand() {
		super(AirlineListCommand.class);
	}

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	public final void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		UserData usrData = ctx.getACARSConnection().getUserData();

		// Only retrieve airlines applicable to the specific user
		Map airlines = (Map) SystemData.getObject("airlines");
		AirlineMessage rspMsg = new AirlineMessage(env.getOwner(), msg.getID());
		for (Iterator ai = airlines.values().iterator(); ai.hasNext();) {
			Airline a = (Airline) ai.next();
			if (a.getActive() && a.getApplications().contains(usrData.getAirlineCode()))
				rspMsg.add(a);
		}

		// Push the response
		ctx.push(rspMsg, env.getConnectionID());
	}
}