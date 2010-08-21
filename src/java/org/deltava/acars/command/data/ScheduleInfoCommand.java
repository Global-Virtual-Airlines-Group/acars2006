// Copyright 2006, 2007, 2008, 2009, 2010 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.sql.Connection;

import org.deltava.acars.beans.MessageEnvelope;

import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.ScheduleMessage;

import org.deltava.beans.schedule.*;
import org.deltava.dao.*;

import org.deltava.util.StringUtils;
import org.deltava.util.system.SystemData;

/**
 * An ACARS data command to search the Flight Schedule.
 * @author Luke
 * @version 3.2
 * @since 1.0
 */

public class ScheduleInfoCommand extends DataCommand {

	/**
	 * Initializes the Command.
	 */
	public ScheduleInfoCommand() {
		super(ScheduleInfoCommand.class);
	}

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	public final void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();

		// Build the schedule search crtieria
		Airline al = SystemData.getAirline(msg.getFlag("airline"));
		ScheduleSearchCriteria sc = new ScheduleSearchCriteria(al, StringUtils.parse(msg.getFlag("flightNum"), 0), 0);
		sc.setAirportD(SystemData.getAirport(msg.getFlag("airportD")));
		sc.setAirportA(SystemData.getAirport(msg.getFlag("airportA")));
		sc.setHourD(StringUtils.parse(msg.getFlag("hourD"), -1));
		sc.setHourA(StringUtils.parse(msg.getFlag("hourA"), -1));
		sc.setMaxResults(StringUtils.parse(msg.getFlag("maxResults"), 0));
		sc.setDistance(StringUtils.parse(msg.getFlag("distance"), 0));
		sc.setDistanceRange(sc.getDistance() > 0 ? 200 : 0);
		sc.setEquipmentTypes(StringUtils.split(msg.getFlag("eqType"), ","));
		sc.setDBName(ctx.getACARSConnection().getUserData().getDB());
		sc.setCheckDispatchRoutes(true);
		sc.setSortBy("RAND()");
		sc.setDispatchOnly(Boolean.valueOf(msg.getFlag("dispatchOnly")).booleanValue());
		if ((sc.getMaxResults() < 1) || (sc.getMaxResults() > 150))
			sc.setMaxResults(50);

		// Do the search
		ScheduleMessage rspMsg = new ScheduleMessage(env.getOwner(), msg.getID());
		try {
			Connection con = ctx.getConnection();
			
			// Get the schedule search DAO
			GetSchedule sdao = new GetSchedule(con);
			sdao.setQueryMax(sc.getMaxResults());
			rspMsg.addAll(sdao.search(sc));
			ctx.push(rspMsg, env.getConnectionID());
		} catch (DAOException de) {
			log.error("Error searching Schedule - " + de.getMessage(), de);
			AcknowledgeMessage errMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
			errMsg.setEntry("error", "Cannot search Flight Schedule - " + de.getMessage());
			ctx.push(errMsg, ctx.getACARSConnection().getID());
		} finally {
			ctx.release();
		}
	}
}