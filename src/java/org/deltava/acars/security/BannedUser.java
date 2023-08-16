// Copyright 2006, 2007, 2016, 2022 Global Virtual Airlines Group. All Rights Reseved.
package org.deltava.acars.security;

import java.time.Instant;

import org.deltava.beans.Person;
import org.deltava.beans.UserData;

import org.deltava.acars.beans.ACARSConnection;

/**
 * A bean to track temporarily banned ACARS users.
 * @author Luke
 * @version 11.1
 * @since 1.0
 */

public class BannedUser implements Comparable<BannedUser> {
	
	private final String _remoteAddr;
	private final String _remoteHost;
	private Instant _expiryTime;
	private final Person _usr;
	private final UserData _usrData;

	/**
	 * Creates a new banned user bean.
	 * @param ac the ACARS connection to ban
	 */
	public BannedUser(ACARSConnection ac) {
		super();
		_remoteAddr = ac.getRemoteAddr();
		_remoteHost = ac.getRemoteHost();
		_usr = ac.getUser();
		_usrData = ac.getUserData();
	}
	
	/**
	 * Returns the IP address of the banned user.
	 * @return the IP address
	 * @see BannedUser#getRemoteHost()
	 */
	public String getRemoteAddr() {
		return _remoteAddr;
	}
	
	/**
	 * Returns the host name of the banned user.
	 * @return the host name
	 * @see BannedUser#getRemoteAddr()
	 */
	public String getRemoteHost() {
		return _remoteHost;
	}
	
	/**
	 * Returns the ban's expiration time.
	 * @return the date/time the ban expires
	 * @see BannedUser#setExpiryDate(Instant)
	 */
	public Instant getExpiryDate() {
		return _expiryTime;
	}
	
	/**
	 * Returns the user bean of the banned user.
	 * @return the user bean, or null
	 * @see BannedUser#getUserData()
	 */
	public Person getUser() {
		return _usr;
	}
	
	/**
	 * Returns the cross-database location bean of the banned user.
	 * @return the userData bean, or null
	 * @see BannedUser#getUser()
	 */
	public UserData getUserData() {
		return _usrData;
	}
	
	/**
	 * Returns the database ID of the banned user.
	 * @return the user's database ID, or 0
	 */
	public int getID() {
		return (_usrData == null) ? 0 : _usrData.getID();
	}
	
	/**
	 * Updates the ban's expiration time.
	 * @param dt the date/time the ban expires
	 * @see BannedUser#getExpiryDate()
	 */
	public void setExpiryDate(Instant dt) {
		_expiryTime = (dt == null) ? Instant.now() : dt;
	}

	@Override
	public boolean equals(Object o) {
		return (o instanceof BannedUser bu) && (compareTo(bu) == 0);
	}
	
	@Override
	public String toString() {
		return _usr.getHexID() + "$" + _remoteAddr;
	}
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public int compareTo(BannedUser usr2) {
		int tmpResult = _usrData.compareTo(usr2._usrData);
		if (tmpResult == 0)
			tmpResult = _remoteAddr.compareTo(usr2._remoteAddr);
		
		return tmpResult;
	}
}