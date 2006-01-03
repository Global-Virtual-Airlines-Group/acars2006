// Copyright 2005 Luke J. Kolin. All Rights Reserved.
package org.deltava.acars.command;

import org.deltava.acars.beans.Envelope;

/**
 * An ACARS command to swallow messages.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */
public class DummyCommand extends ACARSCommand {

   /**
    * Executes the command.
    * @param ctx the Command context
    * @param env the Message envelope
    */
   public void execute(CommandContext ctx, Envelope env) {
      // NOOP
   }
}