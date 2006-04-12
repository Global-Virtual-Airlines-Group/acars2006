// Copyright 2005, 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.util;

import java.util.*;

import org.deltava.beans.*;
import org.deltava.beans.acars.*;
import org.deltava.beans.schedule.Airport;

import org.deltava.acars.beans.ACARSConnection;
import org.deltava.acars.message.InfoMessage;
import org.deltava.acars.message.PositionMessage;

import org.deltava.util.StringUtils;

/**
 * A utility class to turn PositionMessages into RouteEntry beans.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class RouteEntryHelper {

	static class NamedRouteEntry extends RouteEntry implements TabbedMapEntry {
		
		private static final List<String> TAB_NAMES = Arrays.asList(new String[] { "Pilot", "Flight Data"});

		private Pilot _usr;
		private int _clientBuild;
		private String _eqType;
		private String _flightNumber;
		private Airport _airportD;
		private Airport _airportA;

		NamedRouteEntry(Date dt, GeoLocation gl, Pilot usr, String eqType) {
			super(dt, gl.getLatitude(), gl.getLongitude());
			_usr = usr;
			_eqType = eqType;
		}

		public void setClientBuild(int buildNumber) {
			_clientBuild = buildNumber;
		}

		public void setFlightNumber(String flightNumber) {
			_flightNumber = flightNumber;
		}

		public void setAirportD(Airport a) {
			_airportD = a;
		}
		
		public void setAirportA(Airport a) {
			_airportA = a;
		}

		public final String getIconColor() {
			if (isFlagSet(ACARSFlags.FLAG_PAUSED) || isWarning()) {
				return RED;
			} else if (isFlagSet(ACARSFlags.FLAG_ONGROUND)) {
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
			StringBuilder buf = new StringBuilder(_usr.getRank());
			buf.append(", ");
			buf.append(_usr.getEquipmentType());
			buf.append("<br />");
			if (!StringUtils.isEmpty(_flightNumber)) {
				buf.append("<br />Flight <b>");
				buf.append(_flightNumber);
				buf.append("</b> - <span class=\"sec bld\">");
			}

			buf.append(_eqType);
			buf.append("</span> (Build ");
			buf.append(String.valueOf(_clientBuild));
			buf.append(")<br />From: ");
			buf.append(_airportD.getName());
			buf.append(" (");
			buf.append(_airportD.getICAO());
			buf.append(")<br />To: ");
			buf.append(_airportA.getName());
			buf.append(" (");
			buf.append(_airportA.getICAO());
			buf.append(")<br /><br />");
			buf.append(super.getInfoBox());
			return buf.toString();
		}

		public final List<String> getTabNames() {
			return TAB_NAMES;
		}

		public final List<String> getTabContents() {
			List<String> results = new ArrayList<String>();

			// Build Pilot information
			StringBuilder buf = new StringBuilder("<div class=\"mapInfoBox\"><span class=\"pri bld\">");
			buf.append(_usr.getName());
			buf.append("</span> (");
			buf.append(_usr.getPilotCode());
			buf.append(")<br />");
			buf.append(_usr.getRank());
			buf.append(", ");
			buf.append(_usr.getEquipmentType());
			buf.append("<br />");
			if (!StringUtils.isEmpty(_flightNumber)) {
				buf.append("<br />Flight <b>");
				buf.append(_flightNumber);
				buf.append("</b> - <span class=\"sec bld\">");
			}

			buf.append(_eqType);
			buf.append("</span> (Build ");
			buf.append(String.valueOf(_clientBuild));
			buf.append(")<br />From: ");
			buf.append(_airportD.getName());
			buf.append(" (");
			buf.append(_airportD.getICAO());
			buf.append(")<br />To: ");
			buf.append(_airportA.getName());
			buf.append(" (");
			buf.append(_airportA.getICAO());
			buf.append(")<br /><br />ACARS Flight <b>");
			buf.append(StringUtils.format(getID(), "#,##0"));
			buf.append("</b></div>");
			results.add(buf.toString());
			
			// Add Flight information
			results.add(super.getInfoBox());
			return results;
		}
	}

	/**
	 * Builds a route Entry from the current connection data.
	 * @param con the ACARS connection
	 * @return a RouteEntry bean
	 */
	public static RouteEntry build(ACARSConnection con) {

		// Extract data from the connection
		Pilot usr = con.getUser();
		PositionMessage msg = con.getPosition();
		InfoMessage imsg = con.getFlightInfo();
		if ((usr == null) || (msg == null) || (imsg == null))
			return null;

		// Build the NamedRouteEntry
		NamedRouteEntry result = new NamedRouteEntry(new Date(), msg, usr, imsg.getEquipmentType());
		result.setClientBuild(con.getClientVersion());
		result.setID(imsg.getFlightID());
		result.setFlightNumber(imsg.getFlightCode());
		result.setAirSpeed(msg.getAspeed());
		result.setGroundSpeed(msg.getGspeed());
		result.setVerticalSpeed(msg.getVspeed());
		result.setAltitude(msg.getAltitude());
		result.setRadarAltitude(msg.getRadarAltitude());
		result.setFlags(msg.getFlags());
		result.setFlaps(msg.getFlaps());
		result.setHeading(msg.getHeading());
		result.setN1(msg.getN1());
		result.setN2(msg.getN2());
		result.setMach(msg.getMach());
		result.setFuelFlow(msg.getFuelFlow());
		result.setAirportD(imsg.getAirportD());
		result.setAirportA(imsg.getAirportA());
		return result;
	}
}