// Copyright 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.util;

import java.util.*;

import org.apache.log4j.Logger;

import org.deltava.beans.*;
import org.deltava.beans.flight.*;
import org.deltava.beans.schedule.*;

import org.deltava.util.StringUtils;
import org.deltava.util.system.SystemData;

/**
 * A utility class to convert XML request data into an ACARS Flight Report.
 * @author Luke
 * @version 5.0
 * @since 1.0
 */

@Helper(FlightReport.class)
public final class ACARSHelper {
	
	private static final Logger log = Logger.getLogger(ACARSHelper.class);

	// singleton
	private ACARSHelper() {
		super();
	}

	/**
	 * Creates a new ACARS Flight Report from a Flight code.
	 * @param flightCode the flight Code
	 * @return the ACARS Flight Report
	 */
	public static ACARSFlightReport create(String flightCode) {

		StringBuilder aCode = new StringBuilder();
		StringBuilder fCode = new StringBuilder();
		for (int x = 0; x < flightCode.length(); x++) {
			char c = flightCode.charAt(x);
			if (Character.isDigit(c))
				fCode.append(c);
			else if (Character.isLetter(c))
				aCode.append(c);
		}

		// Check the flight code
		if (fCode.length() == 0) {
			log.warn("Bad Flight Code - " + flightCode);
			fCode.append('1');
		}

		// Get the airline
		Airline a = SystemData.getAirline(aCode.toString());
		if (a == null) {
			log.warn("Bad Flight Code - " + flightCode);
			
			// Look it up
			Map<?, ?> aCodes = (Map<?, ?>) SystemData.getObject("airline.defaultCodes");
			a = SystemData.getAirline((String) aCodes.get(aCode.toString().toLowerCase()));
		}

		int flightNum = Math.min(9999, StringUtils.parse(fCode.toString(), 1));
		return new ACARSFlightReport(a, flightNum, 1);
	}
}