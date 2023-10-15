// Copyright 2006, 2007, 2011, 2016, 2019, 2020, 2021, 2022, 2023 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.util.*;
import java.sql.Connection;

import org.deltava.beans.*;
import org.deltava.beans.schedule.Aircraft;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.AircraftMessage;

import org.deltava.dao.*;

/**
 * An ACARS data command to return available Aircraft data.
 * @author Luke
 * @version 11.1
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
		ACARSConnection ac = ctx.getACARSConnection();
		
		AircraftMessage rspMsg = new AircraftMessage(env.getOwner(), msg.getID());
		rspMsg.setShowProfile(Boolean.parseBoolean(msg.getFlag("showProfile")));
		rspMsg.setShowPolicy(Boolean.parseBoolean(msg.getFlag("showPolicy")));
		try {
			Connection con = ctx.getConnection();
			
			// Get all of our IDs and airline codes
			Collection<String> airlineCodes = new HashSet<String>() {{ add(ac.getUserData().getAirlineCode()); }};
			if (!ac.getUserData().getIDs().isEmpty()) {
				GetUserData uddao = new GetUserData(con);
				UserDataMap udm = uddao.get(ac.getUserData().getIDs());
				udm.values().stream().map(UserData::getAirlineCode).forEach(airlineCodes::add);
			}
			
			// Get the aircraft
			GetAircraft acdao = new GetAircraft(con);
			Collection<Aircraft> allAC = acdao.getAll();
			allAC.stream().filter(a -> a.getApps().isEmpty()).forEach(a -> log.warn("No options for " + a.getName()));
			if (!ac.getIsDispatch())
				allAC.stream().filter(a -> airlineCodes.stream().anyMatch(aCode -> a.isUsed(aCode))).forEach(rspMsg::add);
			else
				rspMsg.addAll(allAC);
			
			ctx.push(rspMsg);
		} catch (DAOException de) {
			log.atError().withThrowable(de).log("Error loading equipment types - {}", de.getMessage());
			ctx.push(new AcknowledgeMessage(env.getOwner(), msg.getID(), "Cannot load equipment - " + de.getMessage()));
		} finally {
			ctx.release();
		}
	}
}