// Copyright 2008, 2009, 2010 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.mp;

import java.util.List;

import org.apache.log4j.Logger;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.mp.*;

import org.deltava.util.CalendarUtils;
import org.deltava.util.system.SystemData;

import static org.gvagroup.acars.ACARSFlags.*;

/**
 * An ACARS server command to process multi-player position updates.
 * @author Luke
 * @version 2.8
 * @since 2.2
 */

public class MPInfoCommand extends ACARSCommand {
	
	private static final Logger log = Logger.getLogger(MPInfoCommand.class);

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	public void execute(CommandContext ctx, MessageEnvelope env) {

		// Get the Message and the ACARS Connection
		MPMessage msg = (MPMessage) env.getMessage();
		ACARSConnection ac = ctx.getACARSConnection();
		if (ac == null) {
			log.warn("Missing Connection for " + env.getOwnerID());
			return;
		} else if (ac.getIsDispatch()) {
			log.warn("Dispatch Client sending MP Position Report!");
			return;
		}
		
		// Get the last position report and its age
		InfoMessage info = ac.getFlightInfo();
		PositionMessage oldPM = ac.getPosition();
		if (info == null) {
			log.warn("No Flight Information for " + ac.getUserID());
			AcknowledgeMessage ackMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
			ackMsg.setEntry("sendInfo", "true");
			ctx.push(ackMsg, env.getConnectionID());
			return;
		} else if (oldPM == null) {
			log.info("No position data for " + ac.getUserID());
			return;
		}
		
		// Adjust the message date
		msg.setDate(CalendarUtils.adjustMS(msg.getDate(), ac.getTimeOffset()));

		// Adjust the current position coordinates
		oldPM.setLatitude(msg.getLatitude());
		oldPM.setLongitude(msg.getLongitude());
		oldPM.setHeading(msg.getHeading());
		oldPM.setAltitude(msg.getAltitude());
		oldPM.setAspeed(msg.getAspeed());
		oldPM.setBank(msg.getBank());
		oldPM.setPitch(msg.getPitch());
		oldPM.setFlag(FLAG_AFTERBURNER, msg.isFlagSet(FLAG_AFTERBURNER));
		oldPM.setFlag(FLAG_GEARDOWN, msg.isFlagSet(FLAG_GEARDOWN));
		oldPM.setFlag(FLAG_SPARMED, msg.isFlagSet(FLAG_SPARMED));
		oldPM.setFlaps(msg.getFlaps());
		oldPM.setLights(msg.getLights());

		// Build the update message
		MPUpdateMessage updmsg = new MPUpdateMessage(false);
		MPUpdate upd = new MPUpdate(ac.getFlightID(), oldPM);
		updmsg.add(upd);
		
		// If the message had a recipient, send it just to that connection
		if (ac.getViewerID() != 0)
			ctx.push(updmsg, ac.getViewerID());
		else {
			// Get the connections to notify
			int maxDistance = SystemData.getInt("mp.max_range", 40);
			List<ACARSConnection> cons = ctx.getACARSConnectionPool().getMP(ac.getPosition(), maxDistance);
			cons.remove(ac);
		
			// Send the message
			for (ACARSConnection c : cons)
				ctx.push(updmsg, c.getID());
		}
	}
	
	public final boolean isLogged() {
		return false;
	}
}