// Copyright 2012 Global Virtual Airlines Group. All Rights Reserved.
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
 * @version 4.2
 * @since 4.2
 */

public class AlternateAirportCommand extends DataCommand {

	/**
	 * Initializes the Command.
	 */
	public AlternateAirportCommand() {
		super(AlternateAirportCommand.class);
	}

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
			List<Airport> alts = AlternateAirportHelper.calculateAlternates(a, loc);
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
				
				ctx.push(aaMsg, ctx.getACARSConnection().getID());
			} else {
				AlternateAirportMessage aamsg = new AlternateAirportMessage(env.getOwner(), msg.getID());
				aamsg.addAll(alts);
				ctx.push(aamsg, ctx.getACARSConnection().getID());
			}
		} catch (DAOException de) {
			log.error("Error calculating Alternate - " + de.getMessage(), de);
			AcknowledgeMessage errMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
			errMsg.setEntry("error", "Cannot calculate Alternate - " + de.getMessage());
			ctx.push(errMsg, ctx.getACARSConnection().getID());
		} finally {
			ctx.release();
		}
	}
}