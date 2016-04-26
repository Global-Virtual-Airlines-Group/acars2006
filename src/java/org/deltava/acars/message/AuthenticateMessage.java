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
	private boolean _isATC;
	private boolean _isID;
	
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
	
	public ClientInfo getClientInfo() {
	   return _info;
	}
	
	public String getUserID() {
		return _userID;
	}
	
	public String getPassword() {
		return _pwd;
	}
	
	public Instant getClientUTC() {
		return _clientUTC; 
	}
	
	public boolean isATC() {
		return _isATC;
	}
	
	public boolean isHidden() {
		return _isHidden;
	}
	
	public boolean isID() {
		return _isID;
	}
	
	@Override
	public final boolean isAnonymous() {
		return true;
	}
	
	public void setClientInfo(ClientInfo info) {
	   _info = info;
	}
	
	public void setATC(boolean isATC) {
		_isATC = isATC;
	}
	
	public void setHidden(boolean isHidden) {
		_isHidden = isHidden;
	}
	
	public void setClientUTC(Instant dt) {
		_clientUTC = dt;
	}
	
	public void setDatabaseID(boolean isID) {
		_isID = isID;
	}
	
	public void setRequestedProtocolVersion(int pVersion) {
		setProtocolVersion(pVersion);
	}
}