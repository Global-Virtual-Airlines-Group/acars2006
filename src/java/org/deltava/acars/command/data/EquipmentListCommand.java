// Copyright 2006, 2007, 2011, 2016, 2019, 2020, 2021 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.util.Collection;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.AircraftMessage;

import org.deltava.beans.schedule.Aircraft;

import org.deltava.dao.*;

/**
 * An ACARS data command to return available Aircraft data.
 * @author Luke
 * @version 9.1
 * @since 1.0
 */

public class EquipmentListCommand extends DataCommand {

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public final void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		AircraftMessage rspMsg = new AircraftMessage(env.getOwner(), msg.getID());
		rspMsg.setShowProfile(Boolean.valueOf(msg.getFlag("showProfile")).booleanValue());
		rspMsg.setShowPolicy(Boolean.valueOf(msg.getFlag("showPolicy")).booleanValue());
		
		try {
			ACARSConnection ac = ctx.getACARSConnection();
			GetAircraft acdao = new GetAircraft(ctx.getConnection());
			Collection<Aircraft> allAC = acdao.getAll();
			if (!ac.getIsDispatch())
				allAC.stream().filter(a -> a.isUsed(ac.getUserData().getAirlineCode())).forEach(rspMsg::add);
			else
				rspMsg.addAll(allAC);
			
			ctx.push(rspMsg);
		} catch (DAOException de) {
			log.error("Error loading equipment types", de);
			ctx.push(new AcknowledgeMessage(env.getOwner(), msg.getID(), "Cannot load equipment - " + de.getMessage()));
		} finally {
			ctx.release();
		}
	}
}