// Copyright 2004, 2005, 2006, 2007, 2008, 2009, 2011, 2016 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import java.time.Instant;

import org.deltava.beans.acars.ClientInfo;

/**
 * An ACARS authentication message.
 * @author Luke
 * @version 7.0
 * @since 1.0
 */

public final class AuthenticateMessage extends AbstractMessage {

	private final String _userID;
	private final String _pwd;
	private boolean _isHidden;
	private boolean _isID;
	private boolean _hasCompression;
	
	private Instant _clientUTC;
	private ClientInfo _info;

	/**
	 * Creates a new Authentication message.
	 * @param id the user ID
	 * @param password the password
	 */
	public AuthenticateMessage(String id, String password) {
		super(Message.MSG_AUTH, null);
		_userID = id;
		_pwd = password;
	}
	
	/**
	 * Returns client build information.
	 * @return a ClientInfo bean
	 */
	public ClientInfo getClientInfo() {
	   return _info;
	}
	
	/**
	 * Returns the user ID.
	 * @return the user ID
	 */
	public String getUserID() {
		return _userID;
	}
	
	/**
	 * Returns the provided password.
	 * @return the password
	 */
	public String getPassword() {
		return _pwd;
	}
	
	/**
	 * Returns the client's local time.
	 * @return the time in UTC
	 */
	public Instant getClientUTC() {
		return _clientUTC; 
	}
	
	/**
	 * Returns whether the client supports data compression.
	 * @return TRUE if compression supported, otherwise FALSE
	 */
	public boolean getHasCompression() {
		return _hasCompression;
	}
	
	/**
	 * Returns whether the client is requesting stealth mode.
	 * @return TRUE if stealth mode, otherwise FALSE
	 */
	public boolean isHidden() {
		return _isHidden;
	}
	
	/**
	 * Returns whether the user ID is a database ID or string ID.
	 * @return TRUE if a database ID, otherwise FALSE
	 */
	public boolean isID() {
		return _isID;
	}
	
	@Override
	public final boolean isAnonymous() {
		return true;
	}
	
	/**
	 * Updates the client build information.
	 * @param info a ClientInfo bean
	 */
	public void setClientInfo(ClientInfo info) {
	   _info = info;
	}

	/**
	 * Updates whether the client is requesting stealth mode.
	 * @param isHidden TRUE if stealth mode, otherwise FALSE
	 */
	public void setHidden(boolean isHidden) {
		_isHidden = isHidden;
	}
	
	/**
	 * Updates whether the client supports  
	 * @param hasCompress
	 */
	public void setHasCompression(boolean hasCompress) {
		_hasCompression = hasCompress;
	}
	
	/**
	 * Updates the client's local time.
	 * @param dt the local time in UTC
	 */
	public void setClientUTC(Instant dt) {
		_clientUTC = dt;
	}
	
	/**
	 * Updates whether the user is logging in using a user ID or database ID.
	 * @param isID TRUE if using a database ID, otherwise FALSE
	 */
	public void setDatabaseID(boolean isID) {
		_isID = isID;
	}
	
	public void setRequestedProtocolVersion(int pVersion) {
		setProtocolVersion(pVersion);
	}
}