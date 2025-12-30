// Copyright 2024, 2025 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.util.*;
import java.sql.Connection;

import org.deltava.acars.beans.MessageEnvelope;
import org.deltava.acars.command.*;
import org.deltava.acars.message.*;

import org.deltava.beans.ExternalID;
import org.deltava.beans.flight.*;
import org.deltava.beans.schedule.*;
import org.deltava.beans.simbrief.*;

import org.deltava.dao.*;
import org.deltava.dao.http.*;

import org.deltava.util.system.SystemData;

/**
 * An ACARS command to determine if a Pilot has a pending SimBrief flight.
 * @author Luke
 * @version 12.4
 * @since 12.2
 */

public class SimBriefPlanCommand extends DataCommand {

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		AcknowledgeMessage ackMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
		Airport a = SystemData.getAirport(msg.getFlag("airport"));
		if (a == null) {
			log.warn("Unknown Airport - {}", msg.getFlag("airport"));
			ackMsg.setEntry("hasPlan", "false");
			ctx.push(ackMsg);
			return;
		}

		// Check for draft Flight Reports
		try {
			Connection con = ctx.getConnection();
			
			// Get flights with SimBrief briefings
			GetFlightReports frdao = new GetFlightReports(con);
			List<FlightReport> flights = frdao.getDraftReports(env.getOwner().getID(), null, ctx.getDB());
			flights.removeIf(fr -> !fr.hasAttribute(Attribute.SIMBRIEF));
			long planCount = flights.stream().filter(fr -> (fr.getAirportD().equals(a))).count();
			
			// Load the package directly
			try {
				GetSimBrief sbdao = new GetSimBrief();
				sbdao.setCompression(Compression.BROTLI, Compression.GZIP);
				String sbdata = sbdao.refresh(ctx.getUser().getExternalID(ExternalID.NAVIGRAPH), null);
				BriefingPackage pkg = SimBriefParser.parse(sbdata);
				if (pkg.getAirportD().equals(a)) {
					boolean isDupe = flights.stream().anyMatch(fr -> fr.matches(pkg));
					if (!isDupe) {
						planCount++;
						ackMsg.setEntry("directLoad", "true");
					}
				}
			} catch (HTTPDAOException hde) {
				String errorMsg = (hde instanceof GetSimBrief.SimBriefException sbe) ? SimBriefParser.parseError(sbe.getMessage()) : "???";
				log.warn("Error {} loading latest SimBrief plan for {} - {}", Integer.valueOf(hde.getStatusCode()), ctx.getUser().getName(), errorMsg);
			} catch (Exception e) {
				log.atError().withThrowable(e).log("Error parsing SimBrief plan for {} - {}", ctx.getUser().getName(), e.getMessage());
			}
			
			ackMsg.setEntry("hasPlan", String.valueOf(planCount > 0));
			ackMsg.setEntry("size", String.valueOf(planCount));
		} catch (DAOException de) {
			log.atError().withThrowable(de).log("Error loading briefing package - {}", de.getMessage());
		} finally {
			ctx.release();
		}
		
		ctx.push(ackMsg);
	}
}