// Copyright 2005, 2006, 2016 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import org.deltava.acars.beans.MessageEnvelope;

/**
 * An ACARS command to swallow messages.
 * @author Luke
 * @version 7.0
 * @since 1.0
 */

public class DummyCommand extends ACARSCommand {

   /**
    * Executes the command.
    * @param ctx the Command context
    * @param env the Message envelope
    */
	@Override
   public void execute(CommandContext ctx, MessageEnvelope env) {
      // NOOP
   }
}