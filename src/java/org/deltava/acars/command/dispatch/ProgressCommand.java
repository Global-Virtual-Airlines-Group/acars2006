// Copyright 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.dispatch;

import java.util.*;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.dispatch.*;

/**
 * An ACARS Command to process Dispatcher progress requests.
 * @author Luke
 * @version 2.1
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
		Collection<ACARSConnection> cons = ctx.getACARSConnections(msg.getRecipient());
		if (cons.isEmpty())
			return;
		
		// Get the connection and ensure we can respond
		ACARSConnection ac = cons.iterator().next();
		if ((ac.getFlightInfo() == null) || (ac.getPosition() == null))
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