// Copyright 2005, 2006, 2014, 2019, 2020, 2021, 2022, 2023 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.util.*;
import java.sql.Connection;

import org.deltava.beans.flight.*;
import org.deltava.beans.simbrief.BriefingPackage;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.*;

import org.deltava.dao.*;

/**
 * An ACARS command to load draft Flight Reports for a Pilot. 
 * @author Luke
 * @version 10.4
 * @since 1.0
 */

public class DraftFlightCommand extends DataCommand {

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public final void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		DraftPIREPMessage rspMsg = new DraftPIREPMessage(env.getOwner(), msg.getID());
		try {
			Connection con = ctx.getConnection();
			GetFlightReports frdao = new GetFlightReports(con);
			GetSimBriefPackages sbdao = new GetSimBriefPackages(con);
			
			// Load the flights and briefing packages
			Map<Integer, BriefingPackage> sbPkgs = new HashMap<Integer, BriefingPackage>();
			List<FlightReport> flights = frdao.getDraftReports(env.getOwner().getID(), null, ctx.getDB());
			for (FlightReport dfr : flights)
				sbPkgs.put(Integer.valueOf(dfr.getID()), sbdao.getSimBrief(dfr.getID(), ctx.getDB()));
			
			// Combine and convert into DraftFlightPackage
			flights.stream().map(DraftFlightReport.class::cast).map(dfr -> new DraftFlightPackage(dfr, sbPkgs.get(Integer.valueOf(dfr.getID())))).forEach(rspMsg::add);
			ctx.push(rspMsg);
		} catch (DAOException de) {
			log.error("Error loading draft PIREP data for " + msg.getFlag("id") + " - " + de.getMessage(), de);
			ctx.push(new AcknowledgeMessage(env.getOwner(), msg.getID(), "Cannot load draft Flight Report - " + de.getMessage()));
		} finally {
			ctx.release();
		}
	}
}