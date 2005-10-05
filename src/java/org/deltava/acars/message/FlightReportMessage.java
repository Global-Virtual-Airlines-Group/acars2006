// Copyright 2005 Luke J. Kolin. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.*;

/**
 * An ACARS Message used to pass PIREP data.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class FlightReportMessage extends AbstractMessage {
   
   private ACARSFlightReport _afr;
   private String _errorMsg;

   /**
    * Creates a new Flight Report message.
    * @param msgFrom the Pilot sending the message
    */
   public FlightReportMessage(Pilot msgFrom) {
      super(MSG_PIREP, msgFrom);
   }

   public void setPIREP(ACARSFlightReport afr) {
      _afr = afr;
   }
   
   public void setError(String msg) {
	   _errorMsg = msg;
   }
   
   public ACARSFlightReport getPIREP() {
      return _afr;
   }
   
   public String getError() {
	   return _errorMsg;
   }
}