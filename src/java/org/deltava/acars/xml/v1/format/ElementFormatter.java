// Copyright 2006, 2007, 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.format;

import java.util.*;

import org.jdom.Element;

import org.deltava.acars.xml.*;

import org.deltava.beans.schedule.*;

import org.deltava.util.*;
import org.deltava.util.system.SystemData;

/**
 * A formatter to create XML command elements.
 * @author Luke
 * @version 2.8
 * @since 1.0
 */

abstract class ElementFormatter extends XMLElementFormatter {

	/**
	 * Helper method to format an Airport bean.
	 */
	protected Element formatAirport(Airport a, String eName) {
		Element ae = new Element(eName);
		if (a != null) {
			ae.setAttribute("name", a.getName());
			ae.setAttribute("icao", a.getICAO());
			ae.setAttribute("iata", a.getIATA());
			ae.setAttribute("lat", StringUtils.format(a.getLatitude(), "##0.0000"));
			ae.setAttribute("lng", StringUtils.format(a.getLongitude(), "##0.0000"));
			ae.setAttribute("adse", String.valueOf(a.getADSE()));
			
			// Add UTC offset
			TimeZone tz = a.getTZ().getTimeZone();
			long ofs = tz.getOffset(System.currentTimeMillis()) / 1000;
			ae.setAttribute("utcOffset", String.valueOf(ofs));
			
			// Attach airlines
			for (Iterator<String> i = a.getAirlineCodes().iterator(); i.hasNext(); ) {
				String aCode = i.next();
				Airline al = SystemData.getAirline(aCode);
				
				// Build the airline element
				Element ale = new Element("airline");
				ale.setAttribute("code", al.getCode());
				ale.setAttribute("name", al.getName());
				ae.addContent(ale);
			}
		}

		return ae;
	}
}