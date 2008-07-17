// Copyright 2008 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import java.util.*;

import org.apache.log4j.Logger;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.mp.*;

import org.deltava.acars.util.MPComparator;

import org.deltava.dao.DAOException;
import org.deltava.dao.acars.SetConnection;

import org.deltava.util.system.SystemData;

/**
 * An ACARS command to download position data for all multi-player aircraft in range.
 * @author Luke
 * @version 2.2
 * @since 2.2
 */

public class MPInitCommand extends ACARSCommand {
	
	private static final Logger log = Logger.getLogger(MPInitCommand.class);

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the ACARS Connection
		ACARSConnection ac = ctx.getACARSConnection();
		if (ac == null) {
			log.warn("Missing Connection for " + env.getOwnerID());
			return;
		} else if (ac.getPosition() == null) {
			log.warn("Missing Position for " + env.getOwnerID());
			return;
		} else if (ac.getIsDispatch()) {
			log.warn("Dispatch Client sending MP Init Request!");
			return;
		}

		// Get connections within a set distance
		int maxDistance = SystemData.getInt("mp.max_range", 40);
		List<ACARSConnection> cons = ctx.getACARSConnectionPool().getMP(ac.getPosition(), maxDistance);
		cons.remove(ac);
		
		// If we have too many aircraft, filter by distance
		int maxAircraft = SystemData.getInt("mp.max_aircraft", 30);
		if (cons.size() > maxAircraft) {
			Collections.sort(cons, new MPComparator(ac.getPosition()));
			cons = cons.subList(0, maxAircraft);
		}
		
		// Build the the position info message
		MPUpdateMessage mpmsg = new MPUpdateMessage(true);
		for (Iterator<ACARSConnection> i = cons.iterator(); i.hasNext(); ) {
			ACARSConnection c = i.next();
			mpmsg.add(c.getPosition());
		}
		
		// Return the message
		ctx.push(mpmsg, ac.getID());
		
		// Convert to a multi-player connection if not already
		boolean isMP = ac.getIsMP();
		if (!isMP) {
			ac.setMP(true);
			try {
				SetConnection wdao = new SetConnection(ctx.getConnection());
				wdao.toggleMP(ac.getID());
			} catch (DAOException de) {
				log.error(de.getMessage(), de);
			} finally {
				ctx.release();
			}
		}
	}
}