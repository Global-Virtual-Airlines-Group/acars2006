// Copyright 2019, 2021 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import java.sql.Connection;

import org.apache.log4j.Logger;

import org.deltava.acars.message.*;

import org.deltava.beans.acars.FlightInfo;
import org.deltava.acars.beans.MessageEnvelope;

import org.deltava.dao.*;
import org.deltava.dao.acars.SetSystemInfo;

/**
 * An ACARS server command to handle client performance counter messages.
 * @author Luke
 * @version 10.0
 * @since 8.6
 */

public class PerformanceCommand extends ACARSCommand {
	
	private static final Logger log = Logger.getLogger(PerformanceCommand.class); 

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		PerformanceMessage msg = (PerformanceMessage) env.getMessage();
		InfoMessage info = ctx.getACARSConnection().getFlightInfo();
		boolean idOK = (info != null) && (info.getFlightID() != msg.getFlightID());
		try {
			Connection con = ctx.getConnection();
			if (!idOK) {
				GetACARSData idao = new GetACARSData(con);
				FlightInfo inf = idao.getInfo(msg.getFlightID());
				idOK = (inf != null) && (inf.getID() == msg.getFlightID());
				if (!idOK)
					throw new IllegalArgumentException(String.format("Flight ID %d not owned by %s", Integer.valueOf(msg.getFlightID()), env.getOwner().getPilotCode()));
			}
			
			SetSystemInfo dao = new SetSystemInfo(con);
			dao.write(msg);
			ctx.push(new AcknowledgeMessage(ctx.getUser(), msg.getID()));
		} catch (IllegalArgumentException iae) {
			ctx.push(new AcknowledgeMessage(ctx.getUser(), msg.getID(), iae.getMessage()));
		} catch (DAOException de) {
			log.error(de.getMessage(), de);
			ctx.push(new AcknowledgeMessage(ctx.getUser(), msg.getID(), de.getMessage()));
		} finally {
			ctx.release();
		}
	}
}