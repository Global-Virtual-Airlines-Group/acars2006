// Copyright 2005, 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import org.deltava.acars.beans.MessageEnvelope;

import org.deltava.acars.command.*;
import org.deltava.acars.message.*;
import org.deltava.acars.message.data.ControllerMessage;

import org.deltava.beans.servinfo.NetworkInfo;
import org.deltava.dao.file.GetServInfo;

import org.deltava.util.ThreadUtils;
import org.deltava.util.servinfo.ServInfoLoader;

/**
 * An ACARS Server command to display online ATC data.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class ATCInfoCommand extends DataCommand {

	/**
	 * Initializes the Command.
	 */
	public ATCInfoCommand() {
		super(ATCInfoCommand.class);
	}

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	public final void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message and the network
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		String network = msg.getFlag("network").toUpperCase();
		if ("OFFLINE".equals(network))
			return;

		// Get the network info from the cache
		NetworkInfo info = GetServInfo.getCachedInfo(network);
		ServInfoLoader loader = new ServInfoLoader(network);
		
		// If we get null, then block until we can load it; if we're expired, spawn a new loader thread
		if ((info == null) && (!ServInfoLoader.isLoading(network))) {
			log.info("Loading " + network + " data in main thread");
			Thread t = null;
			synchronized (ServInfoLoader.class) {
				t = new Thread(loader, network + " ServInfo Loader");
				t.setDaemon(true);
				t.setPriority(Math.max(Thread.MIN_PRIORITY, Thread.currentThread().getPriority() - 1));
				ServInfoLoader.addLoader(network, t);
			}

			// Wait for the thread to exit
			int totalTime = 0;
			while (ThreadUtils.isAlive(t) && (totalTime < 10000)) {
				totalTime += 250;
				ThreadUtils.sleep(250);
			}

			// If the thread hasn't died, then kill it
			if (totalTime >= 10000) {
				ThreadUtils.kill(t, 1000);
				info = new NetworkInfo(network);
			} else
				info = loader.getInfo();
		} else if (info == null) {
			info = new NetworkInfo(network);
		} else if (info.getExpired()) {
			synchronized (ServInfoLoader.class) {
				if (!ServInfoLoader.isLoading(network)) {
					log.info("Spawning new ServInfo load thread");
					Thread t = new Thread(loader, network + " ServInfo Loader");
					t.setDaemon(true);
					t.setPriority(Math.max(Thread.MIN_PRIORITY, Thread.currentThread().getPriority() - 1));
					ServInfoLoader.addLoader(network, t);
				} else
					log.warn("Already loading " + network + " information");
			}
		}

		// Filter the controllers based on range from position
		ControllerMessage rspMsg = new ControllerMessage(env.getOwner(), msg.getID());
		if (info != null)
			rspMsg.addAll(info.getControllers(ctx.getACARSConnection().getPosition()));
		
		// Push the response
		ctx.push(rspMsg, env.getConnectionID());
	}
}