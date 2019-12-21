// Copyright 2005, 2006, 2007, 2008, 2011, 2012, 2018, 2019 Global Virtual Airline Group. All Rights Reserved.
package org.deltava.acars.command;

import static org.deltava.acars.workers.Worker.MSG_INPUT;

import java.util.*;

import org.apache.log4j.Logger;

import org.deltava.beans.mvs.*;
import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.*;
import org.deltava.acars.message.dispatch.CancelMessage;
import org.deltava.acars.message.mp.RemoveMessage;

import org.deltava.dao.*;
import org.deltava.dao.acars.SetInfo;

/**
 * An ACARS command to handle disconnections by authenticated users.
 * @author Luke
 * @version 9.0
 * @since 1.0
 */

public class QuitCommand extends ACARSCommand {

	private static final Logger log = Logger.getLogger(QuitCommand.class);

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {

		// Get the message
		QuitMessage msg = (QuitMessage) env.getMessage();

		// Mark the flight as closed
		if (msg.getFlightID() != 0) {
			try {
				SetInfo infoDAO = new SetInfo(ctx.getConnection());
				infoDAO.close(msg.getFlightID(), false);
			} catch (DAOException de) {
				log.error(de.getMessage(), de);
			} finally {
				ctx.release();
			}
		}

		// If a multi-player connection, create a remove message
		if (msg.isMP()) {
			RemoveMessage mrmsg = new RemoveMessage(env.getOwner(), msg.getFlightID());
			MSG_INPUT.add(new MessageEnvelope(mrmsg, env.getConnectionID()));
		}

		// If a voice connection, remove user and refresh the channel list
		if (msg.isVoice()) {
			VoiceChannels vc = VoiceChannels.getInstance();
			Collection<PopulatedChannel> emptyChannels = vc.findEmpty();
			for (PopulatedChannel pc : emptyChannels) {
				for (Long ID : pc.getConnectionIDs()) {
					ACARSConnection avc = ctx.getACARSConnection(ID.longValue());
					vc.add(avc, Channel.DEFAULT_NAME);
				}
			}

			// Remove empty channels now that we have switched users to lobby
			vc.findEmpty();

			// Refresh channel list
			ChannelListMessage clmsg = new ChannelListMessage(env.getOwner(), msg.getID());
			clmsg.setWarnings(ctx.getACARSConnectionPool().getWarnings());
			clmsg.addAll(vc.getChannels());
			ctx.pushVoice(clmsg, env.getConnectionID());
		}

		// Create a deletepilots message
		PilotMessage drmsg = new PilotMessage(env.getOwner(), DataRequest.REMOVEUSER, msg.getID());
		drmsg.add(env.getOwner());
		drmsg.setDispatch(msg.isDispatch());

		// Get all connections
		Collection<ACARSConnection> cons = ctx.getACARSConnectionPool().getAll();
		if (msg.isHidden()) {
			for (ACARSConnection ac : cons) {
				if ((ac.getID() != env.getConnectionID()) && ac.isAuthenticated() && ac.getUser().isInRole("HR"))
					ctx.push(drmsg, ac.getID(), false);
			}
		} else {
			for (ACARSConnection ac : cons) {
				if (ac.getID() != env.getConnectionID()) ctx.push(drmsg, ac.getID(), false);
				if (msg.isDispatch() && (ac.getDispatcherID() == env.getConnectionID()))
					ctx.push(new CancelMessage(env.getOwner()), ac.getID(), false);
			}
		}
	}
}