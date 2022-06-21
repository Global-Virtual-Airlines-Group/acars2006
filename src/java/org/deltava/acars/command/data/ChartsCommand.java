// Copyright 2004, 2005, 2006, 2008, 2012, 2019, 2020, 2022 Global Virtual Airlines Group. All Rights Reserved.
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
 * @version 10.2
 * @since 1.0
 */

public class ChartsCommand extends DataCommand {

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();

		// Check the airport
		Airport a = SystemData.getAirport(msg.getFlag("id"));
		if (a == null) {
			ctx.push(new AcknowledgeMessage(env.getOwner(), msg.getID(), "Unknown Airport " + msg.getFlag("id")));
			return;
		}
		
		// Check if we are not including PDF charts
		boolean noPDF = Boolean.parseBoolean(msg.getFlag("noPDF"));
		try {
			Connection con = ctx.getConnection();

			// Get the DAO and the charts
			GetChart dao = new GetChart(con);
			Collection<Chart> charts = dao.getCharts(a);
			ChartsMessage rspMsg = new ChartsMessage(env.getOwner(), msg.getID());
			rspMsg.setAirport(a);
			charts.stream().filter(ch -> !noPDF || ch.getImgType() != Chart.ImageType.PDF).forEach(rspMsg::add);
			
			// Push the response
			if (rspMsg.getResponse().isEmpty()) {
				SystemTextMessage txtMsg = new SystemTextMessage();
				txtMsg.addMessage("No charts available for " + a);
				ctx.push(txtMsg);
			} else
				ctx.push(rspMsg);
		} catch (DAOException de) {
			log.error("Error loading charts for " + msg.getFlag("id") + " - " + de.getMessage(), de);
			ctx.push(new AcknowledgeMessage(env.getOwner(), msg.getID(), "Cannot load " + msg.getFlag("id") + " charts"));
		} finally {
			ctx.release();
		}
	}
}