// Copyright 2019, 2020, 2023 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import org.deltava.acars.beans.MessageEnvelope;

import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.RunwayListMessage;

import org.deltava.beans.Simulator;
import org.deltava.beans.schedule.Airport;

import org.deltava.dao.*;
import org.deltava.util.system.SystemData;

/**
 * An ACARS data command to return Airport runway information.
 * @author Luke
 * @version 11.1
 * @since 8.6
 */

public class RunwayInfoCommand extends DataCommand {

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message and the airports
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		Airport a = SystemData.getAirport(msg.getFlag("airport"));
		if (a == null) {
			AcknowledgeMessage errMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
			errMsg.setEntry("error", "Unknown airport - " + msg.getFlag("airport"));
			ctx.push(errMsg);
			return;
		}
		
		// Get the sim version for the runway surface
		InfoMessage imsg = ctx.getACARSConnection().getFlightInfo();
		Simulator sim = (imsg == null) ? Simulator.FSX : imsg.getSimulator();
		
		// Create the response
		RunwayListMessage rspMsg = new RunwayListMessage(env.getOwner(), msg.getID(), false);
		rspMsg.setAirportD(a);
		
		try {
			GetNavData navdao = new GetNavData(ctx.getConnection());
			rspMsg.addAll(navdao.getRunways(a, sim));
			ctx.push(rspMsg);
		} catch (DAOException de) {
			log.atError().withThrowable(de).log("Error loading runway info - {}", de.getMessage());
			ctx.push(new AcknowledgeMessage(env.getOwner(), msg.getID(), "Cannot load runway list - " + de.getMessage()));
		} finally {
			ctx.release();
		}
	}
}