// Copyright (c) 2005 Luke J. Kolin. All Rights Reserved.
package org.deltava.acars.command;

import java.util.Iterator;

import org.apache.log4j.Logger;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;

/**
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class DataCommand implements ACARSCommand {

	private static final Logger log = Logger.getLogger(DataCommand.class);

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	public void execute(CommandContext ctx, Envelope env) {

		// Get the message
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();

		// Get the connections to process
		Iterator i = ctx.getACARSConnections(msg.getFilter()).iterator();

		// Create the response
		DataResponseMessage dataRsp = new DataResponseMessage(env.getOwner(), msg.getRequestType());

		switch (msg.getRequestType()) {
			// Get all of the pilot info stuff
			case DataMessage.REQ_PLIST:
				while (i.hasNext()) {
					ACARSConnection ac = (ACARSConnection) i.next();
					dataRsp.addResponse((PositionMessage) ac.getInfo(ACARSConnection.POSITION_INFO));
				}

				break;

			// Get position info
			case DataMessage.REQ_ALIST:
				while (i.hasNext())
					dataRsp.addResponse((ACARSConnection) i.next());

				break;

			// Get Pilot Info
			case DataMessage.REQ_PILOTINFO:
				dataRsp.addResponse((ACARSConnection) i.next());
				break;

			// Get flight information
			case DataMessage.REQ_ILIST:
				while (i.hasNext()) {
					ACARSConnection ac = (ACARSConnection) i.next();
					dataRsp.addResponse((InfoMessage) ac.getInfo(ACARSConnection.FLIGHT_INFO));
				}

				break;

			default:
				log.error("Unsupported Data Request Messasge - " + msg.getRequestType());
		}

		// Push the response
		ctx.push(dataRsp, env.getConnectionID());
	}
}