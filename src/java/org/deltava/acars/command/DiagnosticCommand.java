// Copyright (c) 2005 Luke J. Kolin. All Rights Reserved.
package org.deltava.acars.command;

import org.apache.log4j.Logger;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;

import org.deltava.acars.xml.MessageWriter;

import org.deltava.util.StringUtils;

/**
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class DiagnosticCommand implements ACARSCommand {

   private static final Logger log = Logger.getLogger(DiagnosticCommand.class);

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
   public void execute(CommandContext ctx, Envelope env) {

      // Get the message
      DiagnosticMessage msg = (DiagnosticMessage) env.getMessage();

      // Create the ACK in advance
      AcknowledgeMessage daMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
      switch (msg.getRequestType()) {
         // Send statistics

         // Kick a user based on connection ID
         case DiagnosticMessage.REQ_KICK:
            long cid = 0;
            try {
               cid = Long.parseLong(msg.getRequestData(), 16);
            } catch (Exception e) {
               log.error("Invalid KICK connection ID - " + msg.getRequestData());
            }

            // Try and get the connection
            ACARSConnection ac = ctx.getACARSConnectionPool().get(cid);
            if (ac != null) {
               MessageWriter.remove(cid);
               ctx.getACARSConnectionPool().remove(ac);
               log.warn("Connection " + StringUtils.formatHex(ac.getID()) + " (" + ac.getUserID() + ") KICKED");
               daMsg.setEntry("cid", StringUtils.formatHex(ac.getID()));
               daMsg.setEntry("user", ac.getUserID());
               daMsg.setEntry("addr", ac.getRemoteAddr());
            } else {
               daMsg.setEntry("kick", "0");
            }

            ctx.push(daMsg, env.getConnectionID());
            break;

         case DiagnosticMessage.REQ_SHUTDOWN:
            Thread.currentThread().interrupt();
            break;

         // Kick the user and block his IP address
         default:
            log.error("Unsupported Diagnostic Message - " + msg.getRequestType());
      }
   }
}