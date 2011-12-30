// Copyright 2008, 2010, 2011 Global Virtual Airlines Group. All Rights Reserved.
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
 * @version 4.1
 * @since 2.2
 */

public class InitCommand extends ACARSCommand {
	
	private static final Logger log = Logger.getLogger(InitCommand.class);

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message and the ACARS Connection
		InitMessage imsg = (InitMessage) env.getMessage();
		ACARSConnection ac = ctx.getACARSConnection();
		InfoMessage infMsg = ac.getFlightInfo();
		if (infMsg == null) {
			log.warn("No flight information for " + ac.getUserID());
			return;
		}
		
		// Set the range and center
		infMsg.setLivery(imsg.getLivery());
		ac.setRange(imsg.getLocation(), imsg.getRange());

		// Get connections within a set distance
		List<ACARSConnection> cons = ctx.getACARSConnectionPool().getMP(imsg.getLocation());
		cons.remove(ac);
		
		// If we have too many aircraft, filter by distance
		int maxAircraft = SystemData.getInt("mp.max_aircraft", 32);
		if (!ac.getIsDispatch() && (cons.size() > maxAircraft)) {
			Collections.sort(cons, new MPComparator(ac.getMPLocation()));
			cons = cons.subList(0, maxAircraft);
		}
		
		// Build the the position info message
		MPUpdateMessage mpmsg = new MPUpdateMessage(true);
		mpmsg.setShowLivery(true);
		for (Iterator<ACARSConnection> i = cons.iterator(); i.hasNext(); ) {
			ACARSConnection c = i.next();
			if (!c.getIsDispatch() && !c.getIsATC()) {
				InfoMessage inf = c.getFlightInfo();
				MPUpdate upd = new MPUpdate(c.getUser().getID(), c.getPosition());
				upd.setFlightID(inf.getFlightID());
				upd.setEquipmentType(inf.getEquipmentType());
				upd.setLiveryCode(inf.getLivery());
				upd.setCallsign(inf.getFlightCode());
				Flight f = ACARSHelper.create(inf.getFlightCode());
				upd.setAirlineCode(f.getAirline().getCode());
				mpmsg.add(upd);
			}
		}
		
		ctx.push(mpmsg, ac.getID());
	}
}