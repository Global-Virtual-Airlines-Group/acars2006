// Copyright 2005, 2006, 2010, 2016, 2018, 2019, 2020, 2021 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import org.deltava.beans.*;
import org.deltava.beans.navdata.*;
import org.deltava.beans.schedule.*;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.*;

import org.deltava.dao.*;
import org.deltava.util.*;
import org.deltava.util.system.SystemData;

/**
 * An ACARS data command to display Navigation Data information.  
 * @author Luke
 * @version 10.0
 * @since 1.0
 */

public class NavaidCommand extends DataCommand {

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public final void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message and the current sim
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		InfoMessage inf = ctx.getACARSConnection().getFlightInfo();
		Simulator sim = (inf == null) ? Simulator.FSX : inf.getSimulator();

		boolean isRunway = (msg.getFlag("runway") != null); String id = msg.getFlag("id"); 
		try {
			// Get the DAO and find the Navaid in the DAFIF database
			GetNavData dao = new GetNavData(ctx.getConnection());
			if (isRunway) {
				RunwayListMessage rspMsg = new RunwayListMessage(env.getOwner(), msg.getID(), false);
				Airport ap = SystemData.getAirport(id);
				if (ap != null) {
					rspMsg.setAirportD(ap);
					StringBuilder runway = new StringBuilder(msg.getFlag("runway"));
					// Add a leading zero to the runway if required
					if ((Character.isLetter(runway.charAt(runway.length() - 1)) && (runway.length() == 2)) || (runway.length() == 1)) 
						runway.insert(0, '0');

					Runway nav = dao.getRunway(ap, runway.toString(), sim);
					if (nav != null) {
						log.info("Loaded Runway data for " + nav.getCode() + " " + runway);
						
						// Adjust for magnetic variation
						nav.setHeading(nav.getHeading() + (int)nav.getMagVar());
						rspMsg.add(nav);
					}
					
					ctx.push(rspMsg);
				}
			} else {
				NavigationDataMessage rspMsg = new NavigationDataMessage(env.getOwner(), msg.getID());
				NavigationDataMap ndMap = dao.get(id);
				if (!ndMap.isEmpty()) {
					ACARSConnection ac = ctx.getACARSConnection();
					GeoLocation loc = ((ac.getPosition() == null) || msg.hasFlag("lat")) ? new GeoPosition(StringUtils.parse(msg.getFlag("lat"), 0d), StringUtils.parse(msg.getFlag("lng"), 0d)) : ac.getPosition();
					String radioName = msg.getFlag("radio");
					NavigationDataBean nav = ndMap.get(id, GeoUtils.isValid(loc) ? loc : ac.getPosition());
					rspMsg.add(new NavigationRadioBean(StringUtils.isEmpty(radioName) ? "NAV1" : radioName, nav, msg.getFlag("hdg")));
					log.info("Loaded Navigation data for " + id);
				}
				
				ctx.push(rspMsg);
			}
		} catch (DAOException de) {
			log.error("Error loading navaid " + id + " - " + de.getMessage(), de);
			ctx.push(new AcknowledgeMessage(env.getOwner(), msg.getID(), "Cannot load navaid " + msg.getFlag("id") + " - " + de.getMessage()));
		} finally {
			ctx.release();
		}
	}
}