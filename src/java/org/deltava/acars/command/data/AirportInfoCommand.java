// Copyright 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.util.*;

import org.deltava.beans.schedule.Airport;
import org.deltava.beans.navdata.*;
import org.deltava.beans.wx.METAR;

import org.deltava.comparators.RunwayComparator;

import org.deltava.dao.*;
import org.deltava.dao.file.GetNOAAWeather;

import org.deltava.acars.beans.MessageEnvelope;

import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.AirportInfoMessage;

import org.deltava.util.system.SystemData;

/**
 * An ACARS data command to return Airport weather and runway choices.
 * @author Luke
 * @version 2.6
 * @since 2.6
 */

public class AirportInfoCommand extends DataCommand {

	/**
	 * Initializes the Command.
	 */
	public AirportInfoCommand() {
		super(AirportInfoCommand.class);
	}

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {

		// Get the message and the airport
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		Airport aD = SystemData.getAirport(msg.getFlag("airportD"));
		Airport aA = SystemData.getAirport(msg.getFlag("airportA"));
		if (aD == null)
			return;
		
		try {
			// Get the weather
			GetNOAAWeather wxdao = new GetNOAAWeather();
			METAR wxD = wxdao.getMETAR(new AirportLocation(aD));
			METAR wxA = (aA == null) ? null : wxdao.getMETAR(new AirportLocation(aA));
			
			// Get the runway choices
			GetACARSRunways rwdao = new GetACARSRunways(ctx.getConnection());
			List<Runway> rwyD = rwdao.getPopularRunways(aD, aA, true);
			List<Runway> rwyA = rwdao.getPopularRunways(aD, aA, false);
			if (wxD != null)
				Collections.sort(rwyD, new RunwayComparator(wxD.getWindDirection()));
			if (wxA != null)
				Collections.sort(rwyA, new RunwayComparator(wxA.getWindDirection()));
			
			// Build the departure airport response
			AirportInfoMessage msgD = new AirportInfoMessage(env.getOwner(), msg.getID());
			msgD.setAirport(aD);
			msgD.setMETAR(wxD);
			msgD.addAll(rwyD);
			ctx.push(msgD, ctx.getACARSConnection().getID());
			
			// Build the arrival airport response
			if (aA != null) {
				AirportInfoMessage msgA = new AirportInfoMessage(env.getOwner(), msg.getID());	
				msgA.setAirport(aA);
				msgA.setMETAR(wxA);
				msgA.addAll(rwyA);	
				ctx.push(msgA, ctx.getACARSConnection().getID());
			}
		} catch (DAOException de) {
			log.error("Error getting Airport information - " + de.getMessage(), de);
			AcknowledgeMessage errMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
			errMsg.setEntry("error", "Cannot fetch Airport info - " + de.getMessage());
			ctx.push(errMsg, ctx.getACARSConnection().getID());
		} finally {
			ctx.release();
		}
	}
}