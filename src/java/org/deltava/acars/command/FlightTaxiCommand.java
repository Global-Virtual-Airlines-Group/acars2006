// Copyright 2024 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import java.sql.Connection;

import org.apache.logging.log4j.*;

import org.deltava.acars.beans.MessageEnvelope;
import org.deltava.acars.message.AcknowledgeMessage;
import org.deltava.acars.message.TaxiTimeMessage;

import org.deltava.beans.acars.FlightInfo;

import org.deltava.dao.*;

/**
 * An ACARS Command to log flight taxi durations.
 * @author Luke
 * @version 11.2
 * @since 11.2
 */

public class FlightTaxiCommand extends ACARSCommand {
	
	private static final Logger log = LogManager.getLogger(FlightTaxiCommand.class);

	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message
		TaxiTimeMessage tmsg = (TaxiTimeMessage) env.getMessage();
		
		// Log taxi times
		AcknowledgeMessage ackMsg = new AcknowledgeMessage(tmsg.getSender(), tmsg.getID());
		try {
			Connection con = ctx.getConnection();

			// Find the flight
			GetACARSData dao = new GetACARSData(con);
			FlightInfo inf = dao.getInfo(tmsg.getFlightID());
			if (inf == null) {
				log.warn("Invalid Flight ID - {}", Integer.valueOf(tmsg.getFlightID()));
				ackMsg.setEntry("error", "Invalid Flight ID - " + tmsg.getFlightID());
				ctx.push(ackMsg);
				return;
			}
				
			// Log the data
			SetACARSData wdao = new SetACARSData(con);
			wdao.writeTaxi(inf, tmsg.getInboundTaxiTime(), tmsg.getOutboundTaxiTime());
		} catch (DAOException de) {
			ackMsg.setEntry("error", "Cannot load taxi time - " + de.getMessage());
			log.atError().withThrowable(de).log("Cannot log taxi times - {}", de.getMessage());
		} finally {
			ctx.release();
		}

		ctx.push(ackMsg);
	}
}