// Copyright 2006 Global Virtual Airlines Group. All Rights Reseved.
package org.deltava.acars.security;

import java.util.Date;

import org.deltava.beans.Person;
import org.deltava.beans.system.UserData;

import org.deltava.acars.beans.ACARSConnection;

/**
 * A bean to track temporarily banned ACARS users.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class BannedUser implements Comparable {
	
	private String _remoteAddr;
	private String _remoteHost;
	private Date _expiryTime;
	private Person _usr;
	private UserData _usrData;

	/**
	 * Creates a new banned user bean.
	 * @param remoteAddr the remote IP address
	 * @param usr the Person bean, or null
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
	 * @see BannedUser#setExpiryDate(Date)
	 */
	public Date getExpiryDate() {
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
	public void setExpiryDate(Date dt) {
		_expiryTime = (dt == null) ? new Date() : dt;
	}

	/**
	 * Checks equality by comparing remote Addresses and database ID.
	 */
	public boolean equals(Object o) {
		BannedUser usr2 = (BannedUser) o;
		return (_remoteAddr.equals(usr2._remoteAddr) && (_usrData.getID() == usr2._usrData.getID()));
	}

	/**
	 * Compare two banned users by comparing the expiration date.
	 * @see Comparable#compareTo(Object)
	 */
	public int compareTo(Object o) {
		BannedUser usr2 = (BannedUser) o;
		return _expiryTime.compareTo(usr2._expiryTime);
	}
}