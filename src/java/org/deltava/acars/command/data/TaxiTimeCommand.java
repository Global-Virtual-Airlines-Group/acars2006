// Copyright 2019, 2020, 2021, 2022, 2023, 2024 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.time.Duration;

import org.deltava.acars.beans.MessageEnvelope;
import org.deltava.acars.command.*;
import org.deltava.acars.message.*;

import org.deltava.beans.acars.TaxiTime;
import org.deltava.beans.schedule.Airport;

import org.deltava.dao.*;

import org.deltava.util.system.SystemData;

/**
 * An ACARS command to return airport taxi times.
 * @author Luke
 * @version 11.2
 * @since 8.6
 */

public class TaxiTimeCommand extends DataCommand {

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		boolean isTakeoff = Boolean.parseBoolean(msg.getFlag("isTakeoff"));
		Airport a = SystemData.getAirport(msg.getFlag("airport"));
		if (a == null) {
			ctx.push(new AcknowledgeMessage(env.getOwner(), msg.getID(), "Unknown Airport - " + msg.getFlag("airport")));
			return;
		}
		
		AcknowledgeMessage ackMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
		try {
			GetACARSTaxiTimes ttdao = new GetACARSTaxiTimes(ctx.getConnection());
			TaxiTime tt = ttdao.getTaxiTime(a); 
			Duration d = isTakeoff ? tt.getOutboundTime() : tt.getInboundTime();
			Duration stdv = isTakeoff? tt.getOutboundStdDev() : tt.getInboundStdDev();
			int cnt = isTakeoff ? tt.getOutboundCount() : tt.getInboundCount();
			ackMsg.setEntry("taxiTime", String.valueOf(d.toSeconds()));
			ackMsg.setEntry("year", String.valueOf(tt.getYear()));
			ackMsg.setEntry("size", String.valueOf(cnt));
			ackMsg.setEntry("stdDev", String.valueOf(stdv.toSeconds()));
		} catch (DAOException de) {
			ackMsg.setEntry("error", "Cannot load taxi time - " + de.getMessage());
			log.atError().withThrowable(de).log("Error loading taxi times - {}" + de.getMessage());
		} finally {
			ctx.release();
		}
		
		ctx.push(ackMsg);
	}
}