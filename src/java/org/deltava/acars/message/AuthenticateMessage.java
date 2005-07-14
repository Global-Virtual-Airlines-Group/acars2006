/*
 * Created on Feb 6, 2004
 */
package org.deltava.acars.message;

/**
 * @author Luke J. Kolin
 */
public final class AuthenticateMessage extends AbstractMessage {

	private String userID;
	private String pwd;
	private int protcolVersion = 1;
	private long reqConID;

	public AuthenticateMessage(String id, String password) {
		super(Message.MSG_AUTH, null);
		this.userID = id;
		this.pwd = password;
	}
	
	public String getUserID() {
		return this.userID;
	}
	
	public String getPassword() {
		return this.pwd;
	}
	
	public long getRequestedID() {
		return this.reqConID;
	}
	
	public int getProtocolVersion() {
		return this.protocolVersion;
	}
	
	public final boolean isAnonymous() {
		return true;
	}
	
	public void setProtocolVersion(int pv) {
		if ((pv > 0) && (pv <= Message.PROTOCOL_VERSION))
			this.protocolVersion = pv;
	}
	
	public void setRequestedID(long reqID) {
		this.reqConID = reqID;
	}
	
	public void setRequestedID(String reqID) {
		try {
			this.reqConID = Long.parseLong(reqID, 16);
		} catch (Exception e) { }
	}
}
