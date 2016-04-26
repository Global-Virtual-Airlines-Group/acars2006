// Copyright 2011, 2016 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;
import org.deltava.acars.message.*;

import org.deltava.beans.econ.*;

import org.deltava.util.*;
import org.gvagroup.common.SharedData;

/**
 * An ACARS Command to request a passenger load factor for a flight. 
 * @author Luke
 * @version 7.0
 * @since 4.0
 */

public class LoadFactorCommand extends DataCommand {

	/**
	 * Initializes the Command.
	 */
	public LoadFactorCommand() {
		super(LoadFactorCommand.class);
	}

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		double loadFactor = 1;
		
		// Calculate flight load factor
		ACARSConnection ac = ctx.getACARSConnection();
		java.io.Serializable econ = SharedData.get(SharedData.ECON_DATA + ac.getUserData().getAirlineCode());
		if (econ != null) {
			LoadFactor lf = new LoadFactor((EconomyInfo) IPCUtils.reserialize(econ));
			loadFactor = lf.generate();
		}
			
		// Send the ACK
		AcknowledgeMessage ackMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
		ackMsg.setEntry("loadFactor", StringUtils.format(loadFactor, "0.00000"));
		ctx.push(ackMsg, ac.getID());
	}
}