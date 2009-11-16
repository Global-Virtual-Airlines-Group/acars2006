// Copyright 2005, 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;
import org.deltava.beans.flight.ACARSFlightReport;

/**
 * An ACARS Message used to pass PIREP data.
 * @author Luke
 * @version 2.7
 * @since 1.0
 */

public class FlightReportMessage extends AbstractMessage {
   
   private ACARSFlightReport _afr;

   /**
    * Creates a new Flight Report message.
    * @param msgFrom the Pilot sending the message
    */
   public FlightReportMessage(Pilot msgFrom) {
      super(MSG_PIREP, msgFrom);
   }

   /**
    * Sets the PIREP data.
    * @param afr the ACARSFlightReport bean
    */
   public void setPIREP(ACARSFlightReport afr) {
      _afr = afr;
   }
   
   /**
    * Returns the PIREP data.
    * @return the ACARSFlightReport bean
    */
   public ACARSFlightReport getPIREP() {
      return _afr;
   }
}