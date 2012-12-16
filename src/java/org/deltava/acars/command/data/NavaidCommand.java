// Copyright 2005, 2006, 2010 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.sql.Connection;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.NavigationDataMessage;

import org.deltava.beans.navdata.*;
import org.deltava.beans.schedule.Airport;

import org.deltava.dao.*;

import org.deltava.util.system.SystemData;

/**
 * An ACARS data command to display Navigation Data information.  
 * @author Luke
 * @version 5.1
 * @since 1.0
 */

public class NavaidCommand extends DataCommand {

	/**
	 * Initializes the Command.
	 */
	public NavaidCommand() {
		super(NavaidCommand.class);
	}
	
	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public final void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();

		boolean isRunway = (msg.getFlag("runway") != null);
		NavigationDataMessage rspMsg = new NavigationDataMessage(env.getOwner(), msg.getID());
		try {
			Connection con = ctx.getConnection();

			// Get the DAO and find the Navaid in the DAFIF database
			GetNavData dao = new GetNavData(con);
			if (isRunway) {
				Airport ap = SystemData.getAirport(msg.getFlag("id"));

				// Add a leading zero to the runway if required
				if (ap != null) {
					String runway = msg.getFlag("runway");
					if (Character.isLetter(runway.charAt(runway.length() - 1)) && (runway.length() == 2))
						runway = "0" + runway;
					else if (runway.length() == 1)
						runway = "0" + runway;

					Runway nav = dao.getRunway(ap, runway);
					if (nav != null) {
						log.info("Loaded Runway data for " + nav.getCode() + " " + runway);
						
						// Adjust for magnetic variation
						nav.setHeading(nav.getHeading() + (int)ap.getMagVar());
						rspMsg.add(nav);
					}
				}
			} else {
				NavigationDataMap ndMap = dao.get(msg.getFlag("id"));
				if (!ndMap.isEmpty()) {
					ACARSConnection ac = ctx.getACARSConnection();
					NavigationDataBean nav = ndMap.get(msg.getFlag("id"), ac.getPosition());
					log.info("Loaded Navigation data for " + nav.getCode());
					rspMsg.add(new NavigationRadioBean(msg.getFlag("radio"), nav, msg.getFlag("hdg")));
				}
			}
			
			// Push the response
			ctx.push(rspMsg, env.getConnectionID());
		} catch (DAOException de) {
			log.error("Error loading navaid " + msg.getFlag("id") + " - " + de.getMessage(), de);
			AcknowledgeMessage errorMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
			errorMsg.setEntry("error", "Cannot load navaid " + msg.getFlag("id"));
			ctx.push(errorMsg, env.getConnectionID());
		} finally {
			ctx.release();
		}
	}
}