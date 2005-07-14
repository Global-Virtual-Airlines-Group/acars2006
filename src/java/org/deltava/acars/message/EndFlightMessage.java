// Copyright 2005 Luke J. Kolin. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;

/**
 * 
 * @author Luke
 * @version 1.0
 * @since 1.0
 */
public class EndFlightMessage extends AbstractMessage {

   /**
    * @param type
    * @param msgFrom
    */
   public EndFlightMessage(Pilot msgFrom) {
      super(Message.MSG_ENDFLIGHT, msgFrom);
   }
}