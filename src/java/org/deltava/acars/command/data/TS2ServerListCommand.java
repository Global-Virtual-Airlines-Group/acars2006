// Copyright 2006, 2016, 2019 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import org.deltava.acars.beans.MessageEnvelope;

import org.deltava.acars.command.*;
import org.deltava.acars.message.data.TS2ServerMessage;

import org.deltava.dao.*;

/**
 * An ACARS Server data command to display TeamSpeak 2 server data.
 * @author Luke
 * @version 8.6
 * @since 1.0
 */

public class TS2ServerListCommand extends DataCommand {

	/**
	 * Initializes the Command.
	 */
	public TS2ServerListCommand() {
		super(TS2ServerListCommand.class);
	}

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public final void execute(CommandContext ctx, MessageEnvelope env) {
		
		TS2ServerMessage rspMsg = new TS2ServerMessage(env.getOwner(), env.getMessage().getID());
		try {
			GetTS2Data dao = new GetTS2Data(ctx.getConnection());
			rspMsg.addAll(dao.getServers(env.getOwner().getRoles()));
		} catch (DAOException de) {
			log.error("Error loading TS2 Server datas - " + de.getMessage(), de);
		} finally {
			ctx.release();
		}
		
		ctx.push(rspMsg);
	}
}