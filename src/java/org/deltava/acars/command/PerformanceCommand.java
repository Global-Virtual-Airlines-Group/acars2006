// Copyright 2019 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import org.apache.log4j.Logger;

import org.deltava.acars.message.*;

import org.deltava.acars.beans.MessageEnvelope;
import org.deltava.dao.DAOException;
import org.deltava.dao.acars.SetSystemInfo;

/**
 * An ACARS server command to handle client performance counter messages.
 * @author Luke
 * @version 8.6
 * @since 8.6
 */

public class PerformanceCommand extends ACARSCommand {
	
	private static final Logger log = Logger.getLogger(PerformanceCommand.class); 

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		PerformanceMessage msg = (PerformanceMessage) env.getMessage();
		try {
			SetSystemInfo dao = new SetSystemInfo(ctx.getConnection());
			dao.write(msg);
			ctx.push(new AcknowledgeMessage(ctx.getUser(), msg.getID()));
		} catch (DAOException de) {
			log.error(de.getMessage(), de);
			ctx.push(new AcknowledgeMessage(ctx.getUser(), msg.getID(), de.getMessage()));
		} finally {
			ctx.release();
		}
	}
}