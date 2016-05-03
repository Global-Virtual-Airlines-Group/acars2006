// Copyright 2004, 2005, 2006, 2007, 2008, 2009, 2011, 2012, 2016 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.parse;

import java.time.Instant;

import org.apache.log4j.Logger;

import org.deltava.beans.Pilot;
import org.deltava.beans.acars.ClientInfo;
import org.deltava.beans.acars.ClientType;

import org.deltava.acars.message.*;
import org.deltava.acars.xml.*;

import org.deltava.util.*;

/**
 * A Parser for ACARS Authentication elements.
 * @author Luke
 * @version 7.0
 * @since 1.0
 */

class AuthParser extends XMLElementParser<AuthenticateMessage> {
	
	private static final Logger log = Logger.getLogger(AuthParser.class);

	/**
	 * Convert an XML authentication element into an AuthenticationMessage.
	 * @param e the XML element
	 * @return an AuthenticationMessage
	 * @throws XMLException if a parse error occurs 
	 */
	@Override
	public AuthenticateMessage parse(org.jdom2.Element e, Pilot user) throws XMLException {

		// Get the user ID and password and validate
		String userID = getChildText(e, "user", null);
		String pwd = getChildText(e, "password", null);
		if (StringUtils.isEmpty(userID) || StringUtils.isEmpty(pwd))
			throw new XMLException("Missing userID/password");
		
		// Parse the userID
		UserID id = new UserID(userID);
		boolean isDBID = !id.hasAirlineCode();
		isDBID |= Boolean.valueOf(getChildText(e, "isID", null)).booleanValue();
		if (isDBID && (id.getUserID() < 1))
			throw new XMLException("Invalid Database ID - " + userID);
		
		// Get version and client type
		ClientInfo info = new ClientInfo(StringUtils.parse(getChildText(e, "version", "v1.2").substring(1, 2), 2),
				StringUtils.parse(getChildText(e, "build", "0"), 0), StringUtils.parse(getChildText(e, "beta", "0"), 0));
		if (Boolean.valueOf(getChildText(e, "dispatch", null)).booleanValue())
			info.setClientType(ClientType.DISPATCH);
		else if (Boolean.valueOf(getChildText(e, "atc", null)).booleanValue())
			info.setClientType(ClientType.ATC);
		
		// Create the bean and use this protocol version for responses
		AuthenticateMessage msg = new AuthenticateMessage(userID, pwd);
		msg.setClientInfo(info);
		msg.setHidden(Boolean.valueOf(getChildText(e, "stealth", null)).booleanValue());
		msg.setHasCompression(Boolean.valueOf(getChildText(e, "compress", null)).booleanValue());
		msg.setDatabaseID(isDBID);
		
		// Get the user's local UTC time
		String utc = getChildText(e, "localUTC", null);
		if (utc != null) {
			if (utc.indexOf('.') == -1)
				utc += ".000";
			
			try {
				Instant utcDate = StringUtils.parseInstant(utc.replace('-', '/'), "MM/dd/yyyy HH:mm:ss.SSS");
				msg.setClientUTC(utcDate);
			} catch (IllegalArgumentException iae) {
				log.warn("Unparseable UTC date - " + utc);
				msg.setClientUTC(Instant.now());
			}
		} else
			msg.setClientUTC(Instant.now());

		return msg;
	}
}