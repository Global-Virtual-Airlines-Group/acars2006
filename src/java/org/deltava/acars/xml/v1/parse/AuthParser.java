// Copyright 2004, 2005, 2006, 2007, 2008, 2009, 2011, 2012, 2016, 2021, 2022 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.parse;

import java.util.Base64;
import java.time.*;
import java.time.format.*;
import java.time.temporal.ChronoField;
import java.nio.charset.StandardCharsets;

import org.apache.log4j.Logger;

import org.deltava.beans.Pilot;
import org.deltava.beans.acars.*;

import org.deltava.crypt.AESEncryptor;
import org.deltava.crypt.CryptoException;
import org.deltava.acars.message.*;
import org.deltava.acars.xml.*;

import org.deltava.util.*;

/**
 * A Parser for ACARS Authentication elements.
 * @author Luke
 * @version 10.2
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
		isDBID |= Boolean.parseBoolean(getChildText(e, "isID", null));
		if (isDBID && (id.getUserID() < 1))
			throw new XMLException("Invalid Database ID - " + userID);
		
		// Get version and client type
		ClientInfo info = new ClientInfo(StringUtils.parse(getChildText(e, "version", "v1.2").substring(1, 2), 2), StringUtils.parse(getChildText(e, "build", "0"), 0), StringUtils.parse(getChildText(e, "beta", "0"), 0));
		if (Boolean.parseBoolean(getChildText(e, "dispatch", null)))
			info.setClientType(ClientType.DISPATCH);
		else if (Boolean.parseBoolean(getChildText(e, "atc", null)))
			info.setClientType(ClientType.ATC);
		
		// Handle encrypted password if present
		String alg = getChildText(e, "algorithm", null);
		if ("aes".equalsIgnoreCase(alg)) {
			byte[] pwdData = Base64.getDecoder().decode(pwd);
			
			// Build the key/salt
			byte[] iv = new byte[16]; byte[] k = new byte[16];
			byte[] uid = userID.getBytes(StandardCharsets.UTF_8);
			byte[] vs = getChildText(e, "version", "v3.4").getBytes(StandardCharsets.US_ASCII);
			System.arraycopy(uid, 0, iv, 0, Math.min(iv.length, uid.length));
			System.arraycopy(uid, 0, k, 0, Math.min(12, uid.length));
			System.arraycopy(vs, 0, k, 12, Math.min(4, vs.length));
			
			// Decrypt
			try {
				AESEncryptor aes = new AESEncryptor(k, iv);
				byte[] rawPwd = aes.decrypt(pwdData);
				pwd = new String(rawPwd, StandardCharsets.UTF_8);
			} catch (CryptoException ce) {
				log.warn(String.format("Error decrypting password for %s - %s", userID, ce.getMessage()));
			}
		}
		
		// Create the bean and use this protocol version for responses
		AuthenticateMessage msg = new AuthenticateMessage(userID, pwd);
		msg.setClientInfo(info);
		msg.setHidden(Boolean.parseBoolean(getChildText(e, "stealth", null)));
		msg.setHasCompression(Boolean.parseBoolean(getChildText(e, "compress", null)));
		msg.setDatabaseID(isDBID);
		
		// Get the user's local UTC time
		String utc = getChildText(e, "localUTC", null);
		if (utc != null) {
			if (utc.indexOf('.') == -1)
				utc += ".000";
			
			try {
				DateTimeFormatter mdtf = new DateTimeFormatterBuilder().appendPattern("MM/dd/yyyy HH:mm:ss").appendFraction(ChronoField.MILLI_OF_SECOND, 0, 3, true).toFormatter();
				Instant utcDate = LocalDateTime.parse(utc.replace('-', '/'), mdtf).toInstant(ZoneOffset.UTC);
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