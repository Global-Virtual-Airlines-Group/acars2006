// Copyright 2005 Luke J. Kolin. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;

/**
 * An ACARS Flight End message bean.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class EndFlightMessage extends AbstractMessage {

   /**
    * Creates a new End Flight Message.
    * @param msgFrom the originating Pilot
    */
   public EndFlightMessage(Pilot msgFrom) {
      super(Message.MSG_ENDFLIGHT, msgFrom);
   }
}