// Copyright 2006 Global Virtual Airlines Group. All Rights Reseved.
package org.deltava.acars.security;

import java.util.*;

import org.apache.log4j.Logger;

import org.deltava.beans.Person;

import org.deltava.util.system.SystemData;

/**
 * A utility class to track user bans.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class UserBlocker {

	private static final Logger log = Logger.getLogger(UserBlocker.class);
	private static final Collection<BannedUser> _users = new TreeSet<BannedUser>();
	
	// Singleton
	private UserBlocker() {
	}
	
	public static synchronized void ban(String addr, Person p) {
		if (isBanned(addr))
			log.warn(addr + " already banned");
		else if (isBanned(p))
			log.warn(p.getName() + " already banned");
		
		// Create the new banned user entry
		Calendar cld = Calendar.getInstance();
		cld.add(Calendar.MINUTE, SystemData.getInt("acars.ban_length", 15));
		BannedUser usr = new BannedUser(addr, p);
		usr.setExpiryDate(cld.getTime());
		_users.add(usr);
	}

	/**
	 * Checks if a particular IP address is banned.
	 * @param addr the IP address
	 * @return TRUE if the IP address has an active ban, otherwise FALSE
	 */
	public static synchronized boolean isBanned(String addr) {
		long now = System.currentTimeMillis();
		
		// Loop through the banned user list
		for (Iterator<BannedUser> i = _users.iterator(); i.hasNext(); ) {
			BannedUser usr = i.next();
			if (usr.getExpiryDate().getTime() > now)
				i.remove();
			else if (usr.getRemoteAddr().equals(addr))
				return true;
		}
		
		return false;
	}
	
	/**
	 * Checks wether a particular user is banned.
	 * @param p the Person bean
	 * @return TRUE if the user has an active ban, otherwise FALSE
	 */
	public static synchronized boolean isBanned(Person p) {
		long now = System.currentTimeMillis();
		if (p == null)
			return false;
		
		// Loop through the banned user list
		for (Iterator<BannedUser> i = _users.iterator(); i.hasNext(); ) {
			BannedUser usr = i.next();
			if (usr.getExpiryDate().getTime() > now)
				i.remove();
			else if (usr.getID() == p.getID())
				return true;
		}
		
		return false;
	}
	
	/**
	 * Returns a list of banned users.
	 * @return a Collection of BannedUser beans
	 */
	public static Collection<BannedUser> getBans() {
		return new LinkedHashSet<BannedUser>(_users);
	}
}