// Copyright 2005, 2009, 2010, 2018, 2020 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;
import org.deltava.beans.flight.ACARSFlightReport;

/**
 * An ACARS Message used to pass PIREP data.
 * @author Luke
 * @version 9.0
 * @since 1.0
 */

public class FlightReportMessage extends AbstractMessage {
   
   private ACARSFlightReport _afr;
   private int _dispatcherID;
   private int _routeID;
   
   private boolean _customCabinSize;

   /**
    * Creates a new Flight Report message.
    * @param msgFrom the Pilot sending the message
    */
   public FlightReportMessage(Pilot msgFrom) {
      super(MessageType.PIREP, msgFrom);
   }
   
   /**
    * Returns the PIREP data.
    * @return the ACARSFlightReport bean
    */
   public ACARSFlightReport getPIREP() {
      return _afr;
   }
   
   /**
    * Returns whether this flight used a Dispatch-generated flight plan.
    * @return TRUE if Dispatch used, otherwise FALSE
    */
   public boolean isDispatch() {
	   return (_routeID != 0);
   }
   
   /**
    * Returns the ID of the Dispatcher used to plot this flight.
    * @return the Dispatcher's database ID, or zero for auto-dispatch
    */
   public int getDispatcherID() {
	   return _dispatcherID;
   }
   
   /**
    * Returns the ID of the Route used on this flight.
    * @return the Route's database ID
    */
   public int getRouteID() {
	   return _routeID;
   }
   
   /**
    * Returns whether the aircraft has reported a custom (smaller) cabin size than default.
    * @return TRUE if using a smaller cabin size, otherwise FALSE
    */
   public boolean hasCustomCabinSize() {
	   return _customCabinSize;
   }

   /**
    * Sets the PIREP data.
    * @param afr the ACARSFlightReport bean
    */
   public void setPIREP(ACARSFlightReport afr) {
      _afr = afr;
   }
  
   /**
    * Sets the ID of the Dispatcher used to plot this flight.
    * @param id the Dispatcher's database ID, or zero for auto-dispatch
    */
   public void setDispatcherID(int id) {
	   _dispatcherID = id;
   }
   
   /**
    * Sets the ID of the Route used on this flight.
    * @param id the Route's database ID
    */
   public void setRouteID(int id) {
	   _routeID = id;
   }
  
   /**
    * Sets whether the aircraft had a custom (smaller) cabin size than default.
    * @param isCustomCabin TRUE if a custom cabin, otherwise FALSE 
    */
   public void setCustomCabinSize(boolean isCustomCabin) {
	   _customCabinSize = isCustomCabin;
   }
}