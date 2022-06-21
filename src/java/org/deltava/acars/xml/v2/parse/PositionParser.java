// Copyright 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2014, 2016, 2017, 2018, 2019, 2020, 2021, 2022 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.parse;

import java.time.*;
import java.time.format.*;
import java.time.temporal.ChronoField;

import org.apache.log4j.Logger;

import org.jdom2.*;

import org.deltava.beans.Pilot;
import org.deltava.beans.acars.FlightPhase;
import org.deltava.beans.navdata.AirspaceType;
import org.deltava.beans.servinfo.Controller;

import org.deltava.util.StringUtils;

import org.deltava.acars.message.*;
import org.deltava.acars.xml.*;

/**
 * A Parser for v2 Pilot Client position elements.
 * @author Luke
 * @version 10.2
 * @since 1.0
 */

class PositionParser extends XMLElementParser<PositionMessage> {
	
	private static final Logger log = Logger.getLogger(PositionParser.class);
	private final DateTimeFormatter _mdtf = new DateTimeFormatterBuilder().appendPattern("MM/dd/yyyy HH:mm:ss").appendFraction(ChronoField.MILLI_OF_SECOND, 0, 3, true).toFormatter();
	
	/**
	 * Convert an XML position element into a PositionMessage.
	 * @param e the XML element
	 * @return a PositionMessage
	 * @throws XMLException if a parse error occurs 
	 */
	@Override
	public PositionMessage parse(Element e, Pilot user) throws XMLException {
		
		// Parse the dates
		PositionMessage msg = new PositionMessage(user);
		try {
			String msgDE = getChildText(e, "date", null); String simDE = getChildText(e, "simDate", null);
			if (msgDE != null)
				msg.setDate(LocalDateTime.parse(msgDE.replace('-', '/'), _mdtf).toInstant(ZoneOffset.UTC));
			msg.setSimTime((simDE != null) ? LocalDateTime.parse(simDE.replace('-', '/'), _mdtf).toInstant(ZoneOffset.UTC) : msg.getDate());
		} catch (Exception ex) {
			log.warn("Unparseable date from " + user + " - " + ex.getMessage());
		}

		// Get the basic information
		try {
			msg.setHeading(Integer.parseInt(getChildText(e, "hdg", "0")));
			msg.setPitch(Double.parseDouble(getChildText(e, "pitch", "0")));
			msg.setBank(Double.parseDouble(getChildText(e, "bank", "0")));
			msg.setLatitude(Double.parseDouble(getChildText(e, "lat", "0")));
			msg.setLongitude(Double.parseDouble(getChildText(e, "lon", "0")));
			msg.setRadarAltitude(Integer.parseInt(getChildText(e, "agl", "0")));
			msg.setAspeed(Integer.parseInt(getChildText(e, "aSpeed", "0")));
			msg.setGspeed(Integer.parseInt(getChildText(e, "gSpeed", "0")));
			msg.setVspeed(Integer.parseInt(getChildText(e, "vSpeed", "0")));
			msg.setMach(Double.parseDouble(getChildText(e, "mach", "0")));
			msg.setFuelRemaining(Integer.parseInt(getChildText(e, "fuel", "0")));
			msg.setWeight(Integer.parseInt(getChildText(e, "weight", "0")));
			msg.setFlaps(Integer.parseInt(getChildText(e, "flaps", "0")));
			msg.setFlags(Integer.parseInt(getChildText(e, "flags", "0")));
			msg.setLights(Integer.parseInt(getChildText(e, "lights", "0")));
			msg.setAngleOfAttack(Double.parseDouble(getChildText(e, "aoa", "0")));
			msg.setCG(Double.parseDouble(getChildText(e, "cg", "0")));
			msg.setG(Double.parseDouble(getChildText(e, "g", "1")));
			int engCount = Integer.parseInt(getChildText(e, "ec", "-1"));
			if (engCount > 0) {
				Element n1e = e.getChild("n1");
				Element n2e = e.getChild("n2");
				for (int x = 1; x <= engCount; x++) {
					msg.setN1(x, Double.parseDouble(n1e.getAttributeValue("e" + x, "0")));
					msg.setN2(x, Double.parseDouble(n2e.getAttributeValue("e" + x, "0")));
				}
			}
			
			msg.setAvgN1(Double.parseDouble(getChildText(e, "n1", "0")));
			msg.setAvgN2(Double.parseDouble(getChildText(e, "n2", "0")));
			msg.setWindHeading(Integer.parseInt(getChildText(e, "wHdg", "0")));
			msg.setWindSpeed(Integer.parseInt(getChildText(e, "wSpeed", "0")));
			msg.setVisibility(Double.parseDouble(getChildText(e, "viz", "9999")));
			msg.setCeiling(Integer.parseInt(getChildText(e, "ceiling", "9999")));
			msg.setTemperature(Integer.parseInt(getChildText(e, "temp", "-100")));
			msg.setPressure(Integer.parseInt(getChildText(e, "pressure", "0")));
			msg.setFuelFlow(Integer.parseInt(getChildText(e, "fuelFlow", "0")));
			msg.setPhase(FlightPhase.fromString(getChildText(e, "phase", "?")));
			msg.setSimRate(Integer.parseInt(getChildText(e, "simrate", "256")));
			msg.setLogged(Boolean.parseBoolean(getChildText(e, "isLogged", "true")));
			msg.setReplay(Boolean.parseBoolean(getChildText(e, "isReplay", "false")));
			msg.setRestoreCount(Integer.parseInt(getChildText(e, "restoreCount", "0")));
			msg.setFrameRate(Integer.parseInt(getChildText(e, "frameRate", "0")));
			msg.setTXActive(Boolean.parseBoolean(getChildText(e, "txActive", "true")));
			msg.setTXCode(Integer.parseInt(getChildText(e, "txCode", "2200")));
			msg.setNAV1(getChildText(e, "nav1", "109.90"));
			msg.setNAV2(getChildText(e, "nav2", "109.90"));
			msg.setADF1(getChildText(e, "adf1", null));
			msg.setVASFree(Integer.parseInt(getChildText(e, "vasFree", "0")));
			msg.setGroundOperations(Integer.parseInt(getChildText(e, "groundOps", "0")));
			msg.setNetworkConnected(Boolean.parseBoolean(getChildText(e, "networkConnected", "false")));
			msg.setACARSConnected(Boolean.parseBoolean(getChildText(e, "acarsConnected", "true")));
			double alt = Double.parseDouble(getChildText(e, "msl", "0"));
			msg.setAltitude((int)Math.floor(alt));
			double a2 =(Math.floor(alt) - alt);
			msg.setFractionalAltitude(Math.abs((int)(a2 * 1000)));
			msg.setAirspaceType(AirspaceType.fromAltitude(msg.getRadarAltitude(), msg.getAltitude()));
		} catch (Exception ex) {
			throw new XMLException("Error parsing v2 Position data - " + ex.getMessage(), ex);
		}
		
		// Parse ATC info
		try {
			msg.setCOM1(getChildText(e, "com1", "122.8"));
			msg.setCOM2(getChildText(e, "com2", "122.8"));
			String atcID = getChildText(e, "atc", null);
			if (!StringUtils.isEmpty(atcID)) {
				Element ce = e.getChild("atc");
				Controller ctr = new Controller(Integer.parseInt(ce.getAttributeValue("id")), null);
				ctr.setCallsign(atcID);
				ctr.setPosition(StringUtils.parse(ce.getAttributeValue("lat"), 0.0), StringUtils.parse(ce.getAttributeValue("lon"), 0.0));
				msg.setATC1(ctr);
			}
			
			atcID = getChildText(e, "atc2", null);
			if (!StringUtils.isEmpty(atcID)) {
				Element ce = e.getChild("atc2");
				Controller ctr = new Controller(Integer.parseInt(ce.getAttributeValue("id")), null);
				ctr.setCallsign(atcID);
				ctr.setPosition(StringUtils.parse(ce.getAttributeValue("lat"), 0.0d), StringUtils.parse(ce.getAttributeValue("lon"), 0.0d));
				msg.setATC2(ctr);
			}
		} catch (Exception ex) {
			throw new XMLException("Error parsing ATC data - " + ex.getMessage(), ex);
		}

		return msg;
	}
}