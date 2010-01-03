// Copyright 2008, 2010 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.mp;

import java.util.*;

import org.apache.log4j.Logger;

import org.deltava.beans.Flight;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;
import org.deltava.acars.util.*;

import org.deltava.acars.message.InfoMessage;
import org.deltava.acars.message.mp.*;

import org.deltava.util.system.SystemData;

/**
 * An ACARS command to download position data for all multi-player aircraft in range.
 * @author Luke
 * @version 2.8
 * @since 2.2
 */

public class InitCommand extends ACARSCommand {
	
	private static final Logger log = Logger.getLogger(InitCommand.class);

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message and the ACARS Connection
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
		if (ac.getIsMP())
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
			InfoMessage inf = c.getFlightInfo();
			MPUpdate upd = new MPUpdate(c.getFlightID(), c.getPosition());
			upd.setEquipmentType(inf.getEquipmentType());
			upd.setLiveryCode(inf.getLivery());
			Flight f = ACARSHelper.create(inf.getFlightCode());
			upd.setAirlineCode(f.getAirline().getCode());
			mpmsg.add(upd);
		}
		
		// Return the message
		mpmsg.setShowLivery(true);
		ctx.push(mpmsg, ac.getID());
	}
}