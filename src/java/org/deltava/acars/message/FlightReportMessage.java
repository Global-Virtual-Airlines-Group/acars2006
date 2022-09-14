// Copyright 2005, 2009, 2010, 2018, 2020, 2021, 2022 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;
import org.deltava.beans.acars.DispatchType;
import org.deltava.beans.flight.ACARSFlightReport;

/**
 * An ACARS Message used to pass PIREP data.
 * @author Luke
 * @version 10.3
 * @since 1.0
 */

public class FlightReportMessage extends AbstractMessage {
	
	public static final int DEFAULT_PAX_WEIGHT = 170;
   
   private ACARSFlightReport _afr;
   private DispatchType _dsp;
   private int _dispatcherID;
   private int _routeID;
   
   private boolean _customCabinSize;
   private int _paxWeight = DEFAULT_PAX_WEIGHT;
   
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
    * Returns the dispatch type used on this PIREP.
    * @return the DispatchType
    */
   public DispatchType getDispatcher() {
	   return _dsp;
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
    * Returns the aircraft's weight per passenger.
    * @return the weight in pounds 
    */
   public int getPaxWeight() {
	   return _paxWeight;
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
	   if (id > 0)
		   _dsp = DispatchType.DISPATCH;
   }
   
   /**
    * Updates the dispatcher type used on this flight.
    * @param dsp the DispatchType
    */
   public void setDispatcher(DispatchType dsp) {
	   _dsp = dsp;
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
   
   /**
    * Updates the weight per passenger.
    * @param w the weight in pounds
    */
   public void setPaxWeight(int w) {
	   _paxWeight = w;
   }
}