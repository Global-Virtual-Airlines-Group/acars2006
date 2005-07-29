// Copyright 2005 Luke J. Kolin. All Rights Reserved.
package org.deltava.acars.message;

import java.util.*;

import org.deltava.beans.*;

/**
 * An ACARS Message used to pass PIREP data.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class FlightReportMessage extends AbstractMessage {
   
   private ACARSFlightReport _afr;
   private InfoMessage _flightInfo;
   
   private Set _positions = new TreeSet();

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
   
   public ACARSFlightReport getPIREP() {
      return _afr;
   }
   
   public boolean isOffline() {
      return (_afr.getDatabaseID(FlightReport.DBID_ACARS) == 0);
   }
   
   public void setInfo(InfoMessage msg) {
      _flightInfo = msg;
   }
   
   public InfoMessage getInfo() {
      return _flightInfo;
   }
   
   public void addPosition(PositionMessage msg) {
      _positions.add(msg);
   }
   
   public Collection getPositions() {
      return _positions;
   }
}