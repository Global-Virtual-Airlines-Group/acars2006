// Copyright 2005 Luke J. Kolin. All Rights Reserved.
package org.deltava.acars.util;

import java.util.Date;

import org.deltava.beans.Pilot;
import org.deltava.beans.GeoLocation;

import org.deltava.beans.acars.ACARSFlags;
import org.deltava.beans.acars.RouteEntry;

import org.deltava.acars.message.InfoMessage;
import org.deltava.acars.message.PositionMessage;

/**
 * A utility class to turn PositionMessages into RouteEntry beans.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class RouteEntryHelper {

   static class NamedRouteEntry extends RouteEntry {
      
      private Pilot _usr;
      private String _airports;
      
      NamedRouteEntry(Date dt, GeoLocation gl, Pilot usr) {
         super(dt, gl.getLatitude(), gl.getLongitude());
         _usr = usr;
      }
      
      public void setAirports(String airports) {
         _airports = airports;
      }
      
      public final String getIconColor() {
         if (isFlagSet(ACARSFlags.FLAG_ONGROUND)) {
            return WHITE;
         } else if (getVerticalSpeed() > 100) {
            return ORANGE;
         } else if (getVerticalSpeed() < -100) {
            return YELLOW;
         } else {
            return BLUE;
         }
      }
      
      public final String getInfoBox() {
         StringBuffer buf = new StringBuffer("<span class=\"small pri bld\">");
         buf.append(_usr.getName());
         buf.append("</span> <span class=\"small\">(");
         buf.append(_usr.getPilotCode() + ")<br />");
         buf.append(_airports);
         buf.append("</span><br />");
         buf.append(super.getInfoBox());
         return buf.toString();
      }
   }
   
   /**
    * Builds a route Entry from the current connection data.
    * @param usr the Pilot bean
    * @param msg the latest Position data
    * @param imsg the flight Information data
    * @return a RouteEntry bean
    */
   public static RouteEntry build(Pilot usr, PositionMessage msg, InfoMessage imsg) {
      
      // Build the NamedRouteEntry
      NamedRouteEntry result = new NamedRouteEntry(new Date(), msg, usr);
      result.setAirSpeed(msg.getAspeed());
      result.setGroundSpeed(msg.getGspeed());
      result.setVerticalSpeed(msg.getVspeed());
      result.setAltitude(msg.getAltitude());
      result.setFlags(msg.getFlags());
      result.setFlags(msg.getFlaps());
      result.setHeading(msg.getHeading());
      result.setN1(msg.getN1());
      result.setN2(msg.getN2());
      result.setMach(msg.getMach());
      
      // Build airports
      StringBuffer buf = new StringBuffer(imsg.getAirportD().getName());
      buf.append(" (");
      buf.append(imsg.getAirportD().getICAO());
      buf.append(") - ");
      buf.append(imsg.getAirportA().getName());
      buf.append(" (");
      buf.append(imsg.getAirportA().getICAO());
      buf.append(')');
      
      // Set airport names and return
      result.setAirports(buf.toString());
      return result;
   }
}