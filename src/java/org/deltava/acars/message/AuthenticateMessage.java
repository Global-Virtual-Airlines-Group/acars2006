// Copyright 2004, 2005, 2006, 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import java.util.Date;

/**
 * An ACARS authentication message.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public final class AuthenticateMessage extends AbstractMessage {

	private String _userID;
	private String _pwd;
	private int _protocolVersion = 1;
	private int _build;
	private String _version;
	private boolean _isDispatch;
	private boolean _isHidden;
	
	private Date _clientUTC;

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
	
	public int getClientBuild() {
	   return _build;
	}
	
	public String getVersion() {
		return _version;
	}
	
	public String getUserID() {
		return _userID;
	}
	
	public String getPassword() {
		return _pwd;
	}
	
	public int getProtocolVersion() {
		return _protocolVersion;
	}
	
	public Date getClientUTC() {
		return _clientUTC; 
	}
	
	public boolean isDispatch() {
		return _isDispatch;
	}
	
	public boolean isHidden() {
		return _isHidden;
	}
	
	public final boolean isAnonymous() {
		return true;
	}
	
	public void setProtocolVersion(int pv) {
		if ((pv > 0) && (pv <= Message.PROTOCOL_VERSION))
			_protocolVersion = pv;
	}
	
	public void setClientBuild(int buildNumber) {
	   _build = buildNumber;
	}
	
	public void setVersion(String ver) {
		_version = ver;
	}
	
	public void setDispatch(boolean isDispatch) {
		_isDispatch = isDispatch;
	}
	
	public void setHidden(boolean isHidden) {
		_isHidden = isHidden;
	}
	
	public void setClientUTC(Date dt) {
		_clientUTC = dt;
	}
}