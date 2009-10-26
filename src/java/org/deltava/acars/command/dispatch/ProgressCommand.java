// Copyright 2007, 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.dispatch;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.dispatch.*;

/**
 * An ACARS Command to process Dispatcher progress requests.
 * @author Luke
 * @version 2.7
 * @since 2.1
 */

public class ProgressCommand extends DispatchCommand {
	
	/**
	 * Initializes the Command.
	 */
	public ProgressCommand() {
		super(ProgressCommand.class);
	}

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the inbound message and the owner
		ProgressRequestMessage msg = (ProgressRequestMessage) env.getMessage();

		// Find the Connection
		ACARSConnection ac = ctx.getACARSConnection(msg.getRecipient());
		if ((ac == null) || (ac.getFlightInfo() == null) || (ac.getPosition() == null))
			return;
		
		// Construct the message
		InfoMessage inf = ac.getFlightInfo();
		PositionMessage pos = ac.getPosition();
		ProgressResponseMessage rmsg = new ProgressResponseMessage(env.getOwner());
		rmsg.setRecipient(msg.getRecipient());
		rmsg.setEquipmentType(inf.getEquipmentType());
		rmsg.setAirportD(inf.getAirportD());
		rmsg.setAirportA(inf.getAirportA());
		rmsg.setAirportL(inf.getAirportL());
		rmsg.setFuel(pos.getFuelRemaining());
		rmsg.setBurnRate(pos.getFuelFlow());
		rmsg.setGroundSpeed(pos.getGspeed());
		rmsg.setLocation(pos);
		
		// Send the response
		ctx.push(rmsg, env.getConnectionID());
	}
}