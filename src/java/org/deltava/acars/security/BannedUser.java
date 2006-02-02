// Copyright 2006 Global Virtual Airlines Group. All Rights Reseved.
package org.deltava.acars.security;

import java.util.Date;

import org.deltava.beans.Person;

import org.deltava.util.cache.ExpiringCacheable;

/**
 * A bean to track temporarily banned ACARS users.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class BannedUser implements ExpiringCacheable, Comparable {
	
	private String _remoteAddr;
	private int _dbID;
	private Date _expiryTime;

	/**
	 * Creates a new banned user bean.
	 * @param remoteAddr the remote IP address
	 * @param usr the Person bean, or null
	 */
	public BannedUser(String remoteAddr, Person usr) {
		super();
		_remoteAddr = remoteAddr;
		_dbID = (usr == null) ? 0 : usr.getID();
	}
	
	/**
	 * Returns the IP address of the banned user.
	 * @return the IP address
	 */
	public String getRemoteAddr() {
		return _remoteAddr;
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
	 * Returns the database ID of the banned user.
	 * @return the database ID, or zero
	 */
	public int getID() {
		return _dbID;
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
	 * Returns the database ID, or the IP address if no user specified.
	 * @see Cacheable#cacheKey()
	 */
	public Object cacheKey() {
		return (_dbID == 0) ? _remoteAddr : new Integer(_dbID);
	}
	
	/**
	 * Checks equality by comparing remote Addresses and database ID.
	 */
	public boolean equals(Object o) {
		BannedUser usr2 = (BannedUser) o;
		return (_remoteAddr.equals(usr2._remoteAddr) && (_dbID == usr2._dbID));
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