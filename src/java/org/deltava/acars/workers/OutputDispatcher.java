package org.deltava.acars.workers;

import java.util.Iterator;

import org.deltava.acars.beans.Envelope;
import org.deltava.acars.beans.MessageStack;
import org.deltava.acars.message.Message;

import org.deltava.acars.xml.*;

/**
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public final class OutputDispatcher extends Worker {

   public OutputDispatcher() {
      super("Output Dispatcher", OutputDispatcher.class);
   }

   protected void $run0() {
      log.info("Started");

      while (!Thread.currentThread().isInterrupted()) {
         // Translate and dispatch the messages on the bean output stack
         while (MessageStack.MSG_OUTPUT.hasNext()) {
            Envelope env = MessageStack.MSG_OUTPUT.pop();
            if (env != null) {
               Message msg = (Message) env.getMessage();
               try {
                  MessageWriter.dispatch(env.getConnectionID(), msg);
               } catch (XMLException xe) {
                  log.error("Cannot dispatch - " + xe.getMessage());
               }
            }
         }

         // Dump the messages to the output stack
         if (MessageWriter.hasMessages()) {
            for (Iterator i = MessageWriter.getMessages().iterator(); i.hasNext();)
               MessageStack.RAW_OUTPUT.push((Envelope) i.next());

            // Reset the message writer's internal documents
            MessageWriter.reset();
         }

         // Wait until something is on the bean output stack or we get interrupted
         try {
            MessageStack.MSG_OUTPUT.waitForActivity();
         } catch (InterruptedException ie) {
            log.info("Interrupted");
            Thread.currentThread().interrupt();
         }
      }
   }
}