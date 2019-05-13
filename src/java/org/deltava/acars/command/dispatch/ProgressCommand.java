// Copyright 2007, 2009, 2012, 2015, 2019 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.dispatch;

import java.util.List;
import java.sql.Connection;

import org.deltava.acars.beans.*;
import org.deltava.beans.schedule.*;

import org.deltava.dao.*;

import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.dispatch.*;

/**
 * An ACARS Command to process Dispatcher progress requests.
 * @author Luke
 * @version 8.6
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
	@Override
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
		
		// Calculate closest diversion airport
		try {
			Connection con = ctx.getConnection();
			
			// Calculate closest diversion airport
			GetAircraft acdao = new GetAircraft(con);
			Aircraft a = acdao.get(inf.getEquipmentType());
			if (a != null) {
				List<Airport> alts = AlternateAirportHelper.calculateAlternates(a, pos);
				alts.remove(inf.getAirportA());
				if (alts.size() > 5)
					alts.subList(5, alts.size()).clear();
				
				rmsg.addClosestAirports(alts);
			}
			
			// Determine FIR
			GetFIR fdao = new GetFIR(con);
			rmsg.setFIR(fdao.search(pos));
		} catch (DAOException de) {
			log.error("Error calculating alternates - " + de.getMessage(), de);
		} finally {
			ctx.release();
		}
		
		ctx.push(rmsg);
	}
}