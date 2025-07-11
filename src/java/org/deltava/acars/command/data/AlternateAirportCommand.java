// Copyright 2012, 2019, 2020, 2023 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.util.List;

import org.deltava.beans.schedule.*;
import org.deltava.acars.beans.MessageEnvelope;

import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.AlternateAirportMessage;

import org.deltava.dao.*;

import org.deltava.util.StringUtils;
import org.deltava.util.system.SystemData;

/**
 * An ACARS server command to calculate an alternate airport.
 * @author Luke
 * @version 11.1
 * @since 4.2
 */

public class AlternateAirportCommand extends DataCommand {

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {

		// Get the destination airport and equipment
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		int max = StringUtils.parse(msg.getFlag("max"), 1);
		GeoPosition loc = new GeoPosition(StringUtils.parse(msg.getFlag("lat"), 0d), StringUtils.parse(msg.getFlag("lng"), 0d));
		Airport aA = SystemData.getAirport(msg.getFlag("airport"));
		try {
			GetAircraft acdao = new GetAircraft(ctx.getConnection());
			Aircraft a = acdao.get(msg.getFlag("eqType"));
			if (a == null)
				throw new IllegalArgumentException("Unknown aircraft type - " + msg.getFlag("eqType"));
			
			// Get the alternates
			AlternateAirportHelper hlp = new AlternateAirportHelper(ctx.getACARSConnection().getUserData().getAirlineCode());
			List<Airport> alts = hlp.calculateAlternates(a, loc);
			if (aA != null)
				alts.remove(aA);
			if (alts.size() > max)
				alts.subList(max, alts.size()).clear();
			
			// Return an ACK or a list, depending on the number requested
			if (max == 1) {
				Airport ap = alts.isEmpty() ? null : alts.get(0);
				AcknowledgeMessage aaMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
				if (ap != null) {
					aaMsg.setEntry("icao", ap.getICAO());
					aaMsg.setEntry("iata", ap.getIATA());
				}
				
				ctx.push(aaMsg);
			} else {
				AlternateAirportMessage aamsg = new AlternateAirportMessage(env.getOwner(), msg.getID());
				aamsg.addAll(alts);
				ctx.push(aamsg);
			}
		} catch (DAOException de) {
			log.atError().withThrowable(de).log("Error calculating Alternate - {}", de.getMessage());
			ctx.push(new AcknowledgeMessage(env.getOwner(), msg.getID(), "Cannot calculate Alternate - " + de.getMessage()));
		} finally {
			ctx.release();
		}
	}
}