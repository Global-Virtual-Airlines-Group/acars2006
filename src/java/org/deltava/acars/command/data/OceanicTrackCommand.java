// Copyright 2008, 2009, 2010, 2011, 2016 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.time.Instant;
import java.util.*;

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
 * @version 7.0
 * @since 2.2
 */

public class OceanicTrackCommand extends DataCommand {
	
	/**
	 * Initializes the Command.
	 */
	public OceanicTrackCommand() {
		super(OceanicTrackCommand.class);
	}

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
		
		// Get the response and add the Concorde tracks if pulling down NATs
		OceanicTrackMessage rspMsg = new OceanicTrackMessage(env.getOwner(), msg.getID());
		if (rType == OceanicTrackInfo.Type.NAT) {
			for (Iterator<? extends OceanicTrack> i = OceanicTrack.CONC_ROUTES.iterator(); i.hasNext(); ) {
				OceanicTrack route = i.next();
				rspMsg.add(route);
			}
		}
		
		// Get the date
		Instant dt = msg.hasFlag("date") ? StringUtils.parseInstant(msg.getFlag("date"), "MM/dd/yyyy") : null;
		try {
			GetOceanicRoute dao = new GetOceanicRoute(ctx.getConnection());
			DailyOceanicTracks tracks = dao.getOceanicTracks(rType, dt);
			rspMsg.setDate(tracks.getDate());
			rspMsg.addAll(tracks.getTracks());
			if (rspMsg.getDate() == null)
				rspMsg.setDate(Instant.now());
		} catch (DAOException de) {
			log.error("Error loading " + rType + " tracks for " + msg.getFlag("date") + " - " + de.getMessage(), de);
			AcknowledgeMessage errMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
			errMsg.setEntry("error", "Cannot load " + msg.getFlag("date") + " " + rType + " data");
			ctx.push(errMsg, ctx.getACARSConnection().getID());
		} finally {
			ctx.release();
		}
		
		ctx.push(rspMsg, env.getConnectionID());
	}
}