// Copyright 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2014, 2016, 2017, 2020, 2022, 2023 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.parse;

import java.time.*;
import java.time.format.*;
import java.time.temporal.ChronoField;

import org.apache.logging.log4j.*;

import org.jdom2.Element;

import org.deltava.beans.Pilot;
import org.deltava.beans.acars.FlightPhase;
import org.deltava.beans.navdata.AirspaceType;
import org.deltava.beans.servinfo.Controller;

import org.deltava.util.StringUtils;

import org.deltava.acars.message.*;
import org.deltava.acars.xml.*;

/**
 * A Parser for Pilot Client position elements.
 * @author Luke
 * @version 11.1
 * @since 1.0
 */

class PositionParser extends XMLElementParser<PositionMessage> {
	
	private static final Logger log = LogManager.getLogger(PositionParser.class);
	private final DateTimeFormatter _mdtf = new DateTimeFormatterBuilder().appendPattern("MM/dd/yyyy HH:mm:ss").appendFraction(ChronoField.MILLI_OF_SECOND, 0, 3, true).toFormatter();
	
	/**
	 * Convert an XML position element into a PositionMessage.
	 * @param e the XML element
	 * @return a PositionMessage
	 * @throws XMLException if a parse error occurs 
	 */
	@Override
	public PositionMessage parse(Element e, Pilot user) throws XMLException {
		
		// Create the bean
		PositionMessage msg = new PositionMessage(user);
		
		// Parse the date
		String de = getChildText(e, "date", null);
		try {
			if ((de != null) && (!de.contains("."))) {
				int ms  = Instant.now().get(ChronoField.MILLI_OF_SECOND);
				de = de + "." + String.valueOf(ms);
			}
			
			if (de != null) {
				de = de.replace('-', '/');
				msg.setDate(LocalDateTime.parse(de, _mdtf).toInstant(ZoneOffset.UTC));
				msg.setSimTime(msg.getDate());
			}
		} catch (Exception ex) {
			log.warn("Unparseable date from {} - {}", user, de);
		}

		// Get the basic information
		try {
			msg.setPhase(FlightPhase.fromString(getChildText(e, "phase", "?")));
			if (msg.getPhase() == FlightPhase.UNKNOWN)
				log.warn("Unknown flight phase from {} - {}", user.getPilotCode(), getChildText(e, "phase", "?"));

			msg.setHeading(Integer.parseInt(getChildText(e, "hdg", "0")));
			msg.setPitch(Double.parseDouble(getChildText(e, "pitch", "0")));
			msg.setBank(Double.parseDouble(getChildText(e, "bank", "0")));
			msg.setLatitude(Double.parseDouble(getChildText(e, "lat", "0")));
			msg.setLongitude(Double.parseDouble(getChildText(e, "lon", "0")));
			msg.setAltitude((int) Double.parseDouble(getChildText(e, "msl", "0")));
			msg.setRadarAltitude(Integer.parseInt(getChildText(e, "agl", "0")));
			msg.setAspeed(Integer.parseInt(getChildText(e, "aSpeed", "0")));
			msg.setGspeed(Integer.parseInt(getChildText(e, "gSpeed", "0")));
			msg.setVspeed(Integer.parseInt(getChildText(e, "vSpeed", "0")));
			msg.setMach(Double.parseDouble(getChildText(e, "mach", "0")));
			msg.setFuelRemaining(Integer.parseInt(getChildText(e, "fuel", "0")));
			msg.setFlaps(Integer.parseInt(getChildText(e, "flaps", "0")));
			msg.setFlags(Integer.parseInt(getChildText(e, "flags", "0")));
			msg.setLights(Integer.parseInt(getChildText(e, "lights", "0")));
			msg.setAngleOfAttack(Double.parseDouble(getChildText(e, "aoa", "0")));
			msg.setG(Double.parseDouble(getChildText(e, "g", "1")));
			msg.setAvgN1(Double.parseDouble(getChildText(e, "n1", "0")));
			msg.setAvgN2(Double.parseDouble(getChildText(e, "n2", "0")));
			msg.setWindHeading(Integer.parseInt(getChildText(e, "wHdg", "0")));
			msg.setWindSpeed(Integer.parseInt(getChildText(e, "wSpeed", "0")));
			msg.setVisibility(Double.parseDouble(getChildText(e, "viz", "9999")) * 2.56);	// Fix because ACARS2 multiplied by 100 instead of 256
			msg.setFuelFlow(Integer.parseInt(getChildText(e, "fuelFlow", "0")));
			msg.setSimRate(Integer.parseInt(getChildText(e, "simrate", "256")));
			msg.setLogged(Boolean.parseBoolean(getChildText(e, "isLogged", "true")));
			msg.setReplay(Boolean.parseBoolean(getChildText(e, "noFlood", "false")));
			msg.setFrameRate(Integer.parseInt(getChildText(e, "frameRate", "0")));
			msg.setTXActive(Boolean.parseBoolean(getChildText(e, "txActive", "true")));
			msg.setTXCode(Integer.parseInt(getChildText(e, "txCode", "2200")));
			msg.setAirspaceType(AirspaceType.fromAltitude(msg.getRadarAltitude(), msg.getAltitude()));
		} catch (Exception ex) {
			throw new XMLException("Error parsing Position data - " + ex.getMessage(), ex);
		}
		
		// Parse ATC info
		try {
			msg.setCOM1(getChildText(e, "com1", "122.8"));
			String atcID = getChildText(e, "atc", null);
			if (!StringUtils.isEmpty(atcID)) {
				Element ce = e.getChild("atc");
				Controller ctr = new Controller(StringUtils.parse(ce.getAttributeValue("id"), 0), null);
				ctr.setCallsign(atcID);
				ctr.setPosition(StringUtils.parse(ce.getAttributeValue("lat"), 0.0d), StringUtils.parse(ce.getAttributeValue("lon"), 0.0d));
				msg.setATC1(ctr);
			}
		} catch (Exception ex) {
			throw new XMLException("Error parsing ATC data - " + ex.getMessage(), ex);
		}

		// Return the bean
		return msg;
	}
}