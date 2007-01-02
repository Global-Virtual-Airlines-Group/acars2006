// Copyright 2004, 2005, 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.util.*;
import java.sql.Connection;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.ChartsMessage;

import org.deltava.beans.schedule.*;
import org.deltava.dao.*;

import org.deltava.util.system.SystemData;

/**
 * An ACARS command to display approach charts.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class ChartsCommand extends DataCommand {

	/**
	 * Initializes the Command.
	 */
	public ChartsCommand() {
		super(ChartsCommand.class);
	}
	
	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	public final void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();

		// Check the airport
		ChartsMessage rspMsg = new ChartsMessage(env.getOwner(), msg.getID());
		Airport a = SystemData.getAirport(msg.getFlag("id"));
		if (a == null) {
			AcknowledgeMessage errMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
			errMsg.setEntry("error", "Unknown Airport " + msg.getFlag("id"));
			ctx.push(errMsg, ctx.getACARSConnection().getID());
			return;
		}
		
		// Check if we are not including PDF charts
		boolean noPDF = Boolean.valueOf(msg.getFlag("noPDF")).booleanValue();
		try {
			Connection con = ctx.getConnection();

			// Get the DAO and the charts
			GetChart dao = new GetChart(con);
			Collection<Chart> charts = dao.getCharts(a);
			for (Iterator<Chart> ci = charts.iterator(); ci.hasNext(); ) {
				Chart ch = ci.next();
				if ((ch.getImgType() != Chart.IMG_PDF) || !noPDF)
					rspMsg.add(ch);
			}
		} catch (DAOException de) {
			log.error("Error loading charts for " + msg.getFlag("id") + " - " + de.getMessage(), de);
			AcknowledgeMessage errMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
			errMsg.setEntry("error", "Cannot load " + msg.getFlag("id") + " charts");
			ctx.push(errMsg, ctx.getACARSConnection().getID());
		} finally {
			ctx.release();
		}

		// Push the response
		ctx.push(rspMsg, env.getConnectionID());
	}
}
