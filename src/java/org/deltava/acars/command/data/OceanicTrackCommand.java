// Copyright 2008, 2009, 2010, 2011, 2016, 2019, 2020 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.time.Instant;

import org.deltava.beans.navdata.*;

import org.deltava.dao.*;
import org.deltava.util.*;

import org.deltava.acars.beans.MessageEnvelope;

import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.OceanicTrackMessage;

/**
 * An ACARS data command to return available NAT and PACOT data.
 * @author Luke
 * @version 9.1
 * @since 2.2
 */

public class OceanicTrackCommand extends DataCommand {
	
	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message and the route type
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		OceanicTrackInfo.Type rType = OceanicTrackInfo.Type.NAT;
		try {
			rType = OceanicTrackInfo.Type.valueOf(msg.getFlag("type").toUpperCase());
		} catch (Exception e) {
			log.warn("Unknown Oceanic Route type - " + msg.getFlag("type"));
		}
		
		// Get the date
		OceanicTrackMessage rspMsg = new OceanicTrackMessage(env.getOwner(), msg.getID());
		Instant dt = msg.hasFlag("date") ? StringUtils.parseInstant(msg.getFlag("date"), "MM/dd/yyyy") : null;
		try {
			GetOceanicRoute dao = new GetOceanicRoute(ctx.getConnection());
			DailyOceanicTracks tracks = dao.getOceanicTracks(rType, dt);
			rspMsg.setDate(tracks.getDate());
			rspMsg.addAll(tracks.getTracks());
			if (rType == OceanicTrackInfo.Type.NAT)
				rspMsg.addAll(dao.loadConcordeNATs());
			if (rspMsg.getDate() == null)
				rspMsg.setDate(Instant.now());
			
			ctx.push(rspMsg);
		} catch (DAOException de) {
			log.error("Error loading " + rType + " tracks for " + msg.getFlag("date") + " - " + de.getMessage(), de);
			ctx.push(new AcknowledgeMessage(env.getOwner(), msg.getID(), "Cannot load " + msg.getFlag("date") + " " + rType + " data"));
		} finally {
			ctx.release();
		}
	}
}