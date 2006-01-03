// Copyright (c) 2005 Luke J. Kolin. All Rights Reserved.
package org.deltava.acars.command;

import org.deltava.acars.beans.Envelope;

import org.deltava.acars.message.*;

import org.deltava.util.system.SystemData;

/**
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class AcknowledgeCommand extends ACARSCommand {

   private String ackType;

   /**
    * Intiailizes the Command.
    * @param ackType the original message type
    */
   public AcknowledgeCommand(String ackType) {
      super();
      ackType = ackType.toLowerCase();
   }

   /**
    * Executes the command.
    * @param ctx the Command cContext
    * @param env
    */
   public void execute(CommandContext ctx, Envelope env) {

      // Check if we should acknowledge this message
      if (SystemData.getBoolean("acars.ack." + ackType)) {
         Message msg = (Message) env.getMessage();
         AcknowledgeMessage aMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
         ctx.push(aMsg, env.getConnectionID());
      }
   }
}