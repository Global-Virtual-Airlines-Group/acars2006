// Copyright 2005, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;

/**
 * An ACARS Flight End message bean.
 * @author Luke
 * @version 8.4
 * @since 1.0
 */

public class EndFlightMessage extends AbstractMessage {

   /**
    * Creates a new End Flight Message.
    * @param msgFrom the originating Pilot
    */
   public EndFlightMessage(Pilot msgFrom) {
      super(MessageType.ENDFLIGHT, msgFrom);
   }
}