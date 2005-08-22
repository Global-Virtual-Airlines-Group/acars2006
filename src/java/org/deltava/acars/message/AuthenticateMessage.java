/*
 * Created on Feb 6, 2004
 */
package org.deltava.acars.message;

/**
 * @author Luke J. Kolin
 */
public final class AuthenticateMessage extends AbstractMessage {

	private String _userID;
	private String _pwd;
	private int _protocolVersion = 1;
	private int _build;

	public AuthenticateMessage(String id, String password) {
		super(Message.MSG_AUTH, null);
		_userID = id;
		_pwd = password;
	}
	
	public int getClientBuild() {
	   return _build;
	}
	
	public String getUserID() {
		return _userID;
	}
	
	public String getPassword() {
		return this._pwd;
	}
	
	public int getProtocolVersion() {
		return _protocolVersion;
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
}