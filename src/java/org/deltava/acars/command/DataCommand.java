// Copyright (c) 2005 Luke J. Kolin. All Rights Reserved.
package org.deltava.acars.command;

import java.sql.Connection;
import java.util.Iterator;

import org.apache.log4j.Logger;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;

import org.deltava.beans.Pilot;
import org.deltava.beans.navdata.NavigationDataBean;

import org.deltava.dao.GetNavData;
import org.deltava.dao.DAOException;

import org.deltava.util.system.SystemData;

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

			// Get private voice info
			case DataMessage.REQ_PVTVOX:
			   dataRsp.addResponse("url", SystemData.get("airline.voice.url"));
				break;
				
			// Get flight information
			case DataMessage.REQ_ILIST:
				while (i.hasNext()) {
					ACARSConnection ac = (ACARSConnection) i.next();
					dataRsp.addResponse((InfoMessage) ac.getInfo(ACARSConnection.FLIGHT_INFO));
				}

				break;
				
			// Get navaid info
			case DataMessage.REQ_NAVAIDINFO :
				Pilot usr = null;
				try {
					Connection con = ctx.getConnection();
					
					// Get the DAO and find the Navaid in the DAFIF database
					GetNavData dao = new GetNavData(con);
					NavigationDataBean nav = dao.get(msg.getFlag("id"));
					if (nav != null) {
						log.info("Loaded Navigation data for " + nav.getCode());
						dataRsp.addResponse(new NavigationRadioBean(msg.getFlag("radio"), nav, msg.getFlag("hdg")));
					}
				} catch (DAOException de) {
					log.error("Error loading navaid " + msg.getFlag("id") + " - " + de.getMessage(), de);
					AcknowledgeMessage errMsg = new AcknowledgeMessage(usr, msg.getID());
					errMsg.setEntry("error", "Cannot load navaid " + msg.getFlag("id"));
				} finally {
					ctx.release();
				}

				break;
				
			default:
				log.error("Unsupported Data Request Messasge - " + msg.getRequestType());
		}

		// Push the response
		ctx.push(dataRsp, env.getConnectionID());
	}
}