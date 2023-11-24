// Copyright 2007, 2008, 2009, 2016, 2020, 2023 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.dispatch;

import java.util.*;

import org.deltava.acars.beans.*;
import org.deltava.acars.command.*;
import org.deltava.acars.message.dispatch.CancelMessage;

/**
 * An ACARS Command to cancel Dispatch service requests.
 * @author Luke
 * @version 11.1
 * @since 2.0
 */

public class CancelCommand extends DispatchCommand {

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the inbound message
		CancelMessage msg = (CancelMessage) env.getMessage();
		
		// Determine which direction this is going in, Dispatcher->Pilot or vice versa
		ACARSConnection con = ctx.getACARSConnection();
		ACARSConnection dstC = ctx.getACARSConnection(msg.getRecipient());
		
		// Cancel this connection's dispatch status
		if (con.getIsDispatch() && (dstC != null)) {
			log.info("Dispatcher {} canceling service to {}", con.getUserID(), dstC.getUserID());
			dstC.setDispatcherID(0);
		} else if (!con.getIsDispatch() && (con.getDispatcherID() != 0)) {
			log.info("Pilot {} canceling dispatch services", con.getUserID());
			con.setDispatcherID(0);
		}
		
		// Get the recipients
		if (dstC == null) {
			Collection<ACARSConnection> cons = ctx.getACARSConnectionPool().getAll(ac -> ac.getIsDispatch());
			cons.forEach(ac -> ctx.push(msg, ac.getID(), true));
			cons.add(dstC);
		} else
			ctx.push(msg, dstC.getID(), true);
	}
}