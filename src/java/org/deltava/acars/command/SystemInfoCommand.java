// Copyright 2016, 2019, 2023 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import org.apache.logging.log4j.*;

import org.deltava.acars.beans.MessageEnvelope;
import org.deltava.acars.message.*;

import org.deltava.dao.DAOException;
import org.deltava.dao.acars.SetSystemInfo;

/**
 * An ACARS server command to log client system data.
 * @author Luke
 * @version 11.0
 * @since 6.4
 */

public class SystemInfoCommand extends ACARSCommand {

	private static final Logger log = LogManager.getLogger(SystemInfoCommand.class);
	
	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message
		SystemInfoMessage msg = (SystemInfoMessage) env.getMessage();
		try {
			SetSystemInfo dao = new SetSystemInfo(ctx.getConnection());
			dao.write(msg);
			ctx.push(new AcknowledgeMessage(env.getOwner(), msg.getID()));
		} catch (DAOException de) {
			log.error(de.getMessage(), de);
		} finally {
			ctx.release();
		}
	}
}