// Copyright 2022 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import org.deltava.acars.beans.MessageEnvelope;
import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.ATISMessage;

import org.deltava.beans.navdata.*;
import org.deltava.beans.schedule.Airport;

import org.deltava.dao.DAOException;
import org.deltava.dao.http.*;

import org.deltava.util.system.SystemData;

/**
 * An ACARS data command to retrieve Airport ATIS information.
 * @author Luke
 * @version 10.3
 * @since 10.3
 */

public class ATISCommand extends DataCommand {

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public final void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message and the airport
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		Airport ap = SystemData.getAirport(msg.getFlag("airport"));
		if (ap == null) {
			ctx.push(new AcknowledgeMessage(env.getOwner(), msg.getID(), "Invalid Airport -  " + msg.getFlag("airport")));
			return;
		}
		
		// Get the ATIS
		try {
			ATISMessage rspMsg = new ATISMessage(env.getOwner(), msg.getID());
			GetATIS dao = new GetATIS();
			dao.setCompression(Compression.GZIP);
			ATIS a = dao.get(ap, ATISType.DEP);
			if (a != null) {
				rspMsg.add(a);
				if (a.getType() == ATISType.DEP) {
					a = dao.get(ap, ATISType.ARR);
					if (a != null)
						rspMsg.add(a);
				}
			}
			
			ctx.push(rspMsg);
		} catch (DAOException de) {
			ctx.push(new AcknowledgeMessage(env.getOwner(), msg.getID(), "Cannot load ATIS data - " + de.getMessage()));
		}
	}
}