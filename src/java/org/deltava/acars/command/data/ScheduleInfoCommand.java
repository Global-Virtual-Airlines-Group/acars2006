// Copyright 2006, 2007, 2008, 2009, 2010, 2011, 2018, 2019, 2020, 2021, 2022 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.sql.Connection;

import org.deltava.acars.beans.MessageEnvelope;

import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.ScheduleMessage;

import org.deltava.beans.Inclusion;
import org.deltava.beans.UserData;
import org.deltava.beans.schedule.*;
import org.deltava.dao.*;

import org.deltava.util.StringUtils;
import org.deltava.util.system.SystemData;

/**
 * An ACARS data command to search the Flight Schedule.
 * @author Luke
 * @version 10.2
 * @since 1.0
 */

public class ScheduleInfoCommand extends DataCommand {

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public final void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		UserData usrLoc = ctx.getACARSConnection().getUserData();

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
		sc.setDBName(usrLoc.getDB());
		sc.setCheckDispatchRoutes(true);
		sc.setExcludeHistoric(Boolean.parseBoolean(msg.getFlag("excludeHistoric")) ? Inclusion.EXCLUDE : Inclusion.ALL);
		sc.setFlightsPerRoute(sc.isPopulated() ? 0 : 3);
		sc.setSortBy("RAND()");
		sc.setDispatchOnly(Boolean.parseBoolean(msg.getFlag("dispatchOnly")) ? Inclusion.INCLUDE : Inclusion.ALL);
		sc.setFlightsPerRoute(StringUtils.parse(msg.getFlag("maxPerRoute"), 0));
		if ((sc.getMaxResults() < 1) || (sc.getMaxResults() > 150))
			sc.setMaxResults(50);

		// Do the search
		ScheduleMessage rspMsg = new ScheduleMessage(env.getOwner(), msg.getID());
		try {
			Connection con = ctx.getConnection();
			GetRawSchedule rsdao = new GetRawSchedule(con);
			GetScheduleSearch sdao = new GetScheduleSearch(con);
			sdao.setSources(rsdao.getSources(true, usrLoc.getDB()));
			sdao.setQueryMax(sc.getMaxResults());
			rspMsg.addAll(sdao.search(sc));
			ctx.push(rspMsg);
		} catch (DAOException de) {
			log.error("Error searching Schedule - " + de.getMessage(), de);
			ctx.push(new AcknowledgeMessage(env.getOwner(), msg.getID(), "Cannot search Flight Schedule - " + de.getMessage()));
		} finally {
			ctx.release();
		}
	}
}