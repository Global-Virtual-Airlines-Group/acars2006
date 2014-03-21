// Copyright 2013, 2014 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.util.*;

import org.deltava.dao.*;

import org.deltava.beans.UserData;
import org.deltava.beans.acars.IATACodes;
import org.deltava.acars.beans.MessageEnvelope;

import org.deltava.acars.command.*;
import org.deltava.dao.acars.GetACARSIATACodes;

import org.deltava.acars.message.DataRequestMessage;
import org.deltava.acars.message.data.IATACodeMessage;

/**
 * An ACARS Command to return a list of IATA aircraft codes.
 * @author Luke
 * @version 5.4
 * @since 5.1
 */

public class IATACodeCommand extends DataCommand {

	/**
	 * Initializes the Command.
	 */
	public IATACodeCommand() {
		super(IATACodeCommand.class);
	}

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		IATACodeMessage rspMsg = new IATACodeMessage(env.getOwner(), msg.getID());
		UserData ud = ctx.getACARSConnection().getUserData();

		try {
			GetACARSIATACodes dao = new GetACARSIATACodes(ctx.getConnection());
			Map<String, IATACodes> codes = dao.getAll(ud.getDB());
			rspMsg.addAll(codes.values());
		} catch (DAOException de) {
			log.error("Error loading FDE codes - " + de.getMessage(), de);
		} finally {
			ctx.release();
		}
		
		ctx.push(rspMsg, env.getConnectionID());
	}
}