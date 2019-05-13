// Copyright 2011, 2019 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import java.util.*;

import org.apache.log4j.Logger;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.ChannelListMessage;

import org.deltava.beans.mvs.*;

import org.deltava.util.*;
import org.deltava.util.system.SystemData;

/**
 * An ACARS command to handle switchng voice channels.
 * @author Luke
 * @version 8.6
 * @since 4.0
 */

public class SwitchChannelCommand extends ACARSCommand {
	
	private static final Logger log = Logger.getLogger(SwitchChannelCommand.class);
	private final Collection<String> _ncRoles = new HashSet<String>() {{
		add("Admin");
	}};
	
	private final VoiceChannels _vc = VoiceChannels.getInstance();
	
	/**
	 * Creates the Command.
	 */
	@SuppressWarnings("unchecked")
	public SwitchChannelCommand() {
		super();
		if (SystemData.getBoolean("acars.voice.enabled")) {
			Collection<String> ncRoles = (Collection<String>) SystemData.getObject("acars.voice.newChannelRoles");
			if (ncRoles == null)
				_ncRoles.addAll(ncRoles);
		}
	}
	
	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {

		// Get the message and the channel name
		SwitchChannelMessage msg = (SwitchChannelMessage) env.getMessage();
		String chName = msg.getChannel();
		if (StringUtils.isEmpty(chName)) {
			log.warn("Empty channel from " + env.getOwnerID());
			return;
		}
		
		// Add support for moving others
		ACARSConnection ac = ctx.getACARSConnection();
		if (!StringUtils.isEmpty(msg.getRecipient()) && env.getOwner().isInRole("HR")) {
			ac = ctx.getACARSConnection(msg.getRecipient());
			if (ac == null) {
				log.warn(msg.getRecipient() + " not logged in!");
				return;
			}
			
			log.info(env.getOwner().getName() + " moving " + ac.getUserID() + " to " + chName);
		}
		
		// Try to join the channel
		try {
			PopulatedChannel oldChannel = _vc.get(ac.getID());
			PopulatedChannel pc = _vc.add(ac, chName);
			
			// If we didn't add the channel, create a new one
			if ((pc == null) && (!StringUtils.isEmpty(msg.getRecipient()))) {
				if (!RoleUtils.hasAccess(ac.getUser().getRoles(), _ncRoles))
					throw new SecurityException("Cannot create temporary channel");

				Channel c = new Channel(chName);
				c.setDescription(StringUtils.isEmpty(msg.getDescription()) ? "Temporary Voice Channel" : msg.getDescription());
				c.setOwner(ac.getUser());
				c.setSampleRate(SampleRate.SR8K);
				c.setFrequency(msg.getFrequency());
				c.addRoles(Channel.Access.TALK, ac.getUser().getRoles());
				c.addRoles(Channel.Access.VIEW, ac.getUser().getRoles());
				pc = _vc.add(ac, c);
				log.info(ac.getUserID() + " created temporary channel " + chName);
			} else if (pc != null)
				log.info(ac.getUserID() + " swithcing to " + pc.getChannel().getName());
			
			// Return the channel info, to all voice users
			ChannelListMessage clmsg = new ChannelListMessage(ac.getUser(), msg.getID());
			clmsg.setWarnings(ctx.getACARSConnectionPool().getWarnings());
			clmsg.setClearList(false);
			clmsg.add(pc);
			if (oldChannel != null)
				clmsg.add(oldChannel);
			
			ctx.pushVoice(clmsg, -1);
		} catch (SecurityException se) {
			log.warn("Cannot join/create channel " + chName + " - " + se.getMessage());
			ctx.push(new ErrorMessage(ac.getUser(), se.getMessage(), msg.getID()));
		}
	}
}