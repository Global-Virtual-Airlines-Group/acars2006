// Copyright 2004, 2005, 2006, 2007, 2008 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.parse;

import java.util.Date;

import org.deltava.beans.Pilot;

import org.deltava.acars.message.*;
import org.deltava.acars.xml.XMLException;

import org.deltava.util.StringUtils;

/**
 * A Parser for Authentication elements.
 * @author Luke
 * @version 2.1
 * @since 1.0
 */

class AuthParser extends ElementParser {

	/**
	 * Convert an XML authentication element into an AuthenticationMessage.
	 * @param e the XML element
	 * @return a AuthenticationMessage
	 * @throws XMLException if a parse error occurs 
	 */
	public Message parse(org.jdom.Element e, Pilot user) throws XMLException {

		// Get the user ID and password and validate
		String userID = getChildText(e, "user", null);
		String pwd = getChildText(e, "password", null);
		if (StringUtils.isEmpty(userID) || StringUtils.isEmpty(pwd))
			throw new XMLException("Missing userID/password");

		// Create the bean and use this protocol version for responses
		AuthenticateMessage msg = new AuthenticateMessage(userID, pwd);
		msg.setVersion(getChildText(e, "version", "v1.2"));
		msg.setDispatch(Boolean.valueOf(getChildText(e, "dispatch", null)).booleanValue());
		msg.setHidden(Boolean.valueOf(getChildText(e, "stealth", null)).booleanValue());
		msg.setDatabaseID(Boolean.valueOf(getChildText(e, "isID", null)).booleanValue());
		msg.setClientBuild(StringUtils.parse(getChildText(e, "build", "0"), 0));
		msg.setBeta(StringUtils.parse(getChildText(e, "beta", "0"), 0));
		
		// Validate the database ID
		if (msg.isID() && (StringUtils.parse(userID, 0) < 1))
			throw new XMLException("Invalid Database ID - " + userID);
		
		// Get the user's local UTC time
		String utc = getChildText(e, "localUTC", null);
		if (utc != null) {
			if (utc.indexOf('.') == -1)
				utc = utc + ".000";
			
			try {
				Date utcDate = StringUtils.parseDate(utc, "MM/dd/yyyy HH:mm:ss.SSS");
				msg.setClientUTC(utcDate);
			} catch (IllegalArgumentException iae) {
				log.warn("Unparseable UTC date - " + utc);
				msg.setClientUTC(new Date());
			}
		} else
			msg.setClientUTC(new Date());

		// Return the bean
		return msg;
	}
}