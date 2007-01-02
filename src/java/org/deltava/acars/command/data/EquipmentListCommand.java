// Copyright 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.sql.Connection;

import org.deltava.acars.beans.MessageEnvelope;

import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.AircraftMessage;

import org.deltava.dao.*;

/**
 * An ACARS data command to return available Aircraft data.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class EquipmentListCommand extends DataCommand {

	/**
	 * Initializes the Command.
	 */
	public EquipmentListCommand() {
		super(EquipmentListCommand.class);
	}
	
	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	public final void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		AircraftMessage rspMsg = new AircraftMessage(env.getOwner(), msg.getID());
		rspMsg.setShowProfile(Boolean.valueOf(msg.getFlag("showProfile")).booleanValue());
		
		try {
			Connection con = ctx.getConnection();
			GetAircraft acdao = new GetAircraft(con);
			rspMsg.addAll(acdao.getAircraftTypes());
		} catch (DAOException de) {
			log.error("Error loading equipment types", de);
			AcknowledgeMessage errMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
			errMsg.setEntry("error", "Cannot load equipment types");
			ctx.push(errMsg, ctx.getACARSConnection().getID());
		} finally {
			ctx.release();
		}
		
		// Push the response
		ctx.push(rspMsg, env.getConnectionID());
	}
}