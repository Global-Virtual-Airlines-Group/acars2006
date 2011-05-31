// Copyright 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

import java.util.*;
import java.util.concurrent.locks.*;

import org.deltava.beans.mvs.*;
import org.deltava.util.*;

/**
 * A bean to store voice channel information.
 * @author Luke
 * @version 4.0
 * @since 4.0
 */

public class VoiceChannels {
	
	private static final Map<String, PopulatedChannel> _channels = new TreeMap<String, PopulatedChannel>();
	
	private static final ReentrantReadWriteLock _rwLock = new ReentrantReadWriteLock(true);
	private static final Lock _rLock = _rwLock.readLock();
	private static final ReentrantReadWriteLock.WriteLock _wLock = _rwLock.writeLock();

	// singleton
	private VoiceChannels() {
		super();
	}
	
	/**
	 * Retrieves a voice channel.
	 * @param name the channel name
	 * @return a Channel, or null if not found
	 * @throws NullPointerException if name is null 
	 */
	public static PopulatedChannel get(String name) {
		try {
			_rLock.lock();
			return _channels.get(name.toLowerCase());
		} finally {
			_rLock.unlock();
		}
	}

	/**
	 * Adds a voice channel to the list.
	 * @param c the Channel
	 * @return the PopulatedChannel which the user was added to
	 * @throws IllegalStateException if the channel name is not unique
	 */
	public static PopulatedChannel add(ACARSConnection ac, Channel c) {
		try {
			_wLock.lock();
			String name = c.getName().toLowerCase();
			if (_channels.containsKey(name))
				throw new IllegalStateException(name + " already exists");

			// Add the user to the channel if present
			PopulatedChannel pc = new PopulatedChannel(c);
			_channels.put(name, pc);
			if (ac != null) {
				remove(ac);
				pc.add(ac.getID(), ac.getUser());
			}
				
			return pc;
		} finally {
			_wLock.unlock();
		}
	}
	
	/**
	 * Retrieves the channel with a particular connection.
	 * @param conID the connection ID
	 * @return a Channel, or null if not found
	 */
	public static PopulatedChannel get(long conID) {
		try {
			_rLock.lock();
			for (PopulatedChannel pc : _channels.values()) {
				if (pc.contains(conID))
					return pc;
			}
			
			return null;
		} finally {
			_rLock.unlock();
		}
	}
	
	/**
	 * Adds a user to an existing channel, removing from all other channels.
	 * @param ac the ACARSConnection
	 * @param c the Channel to add to
	 * @return the PopulatedChannel which the user was added to
	 * @throws NullPointerException if ac or c are null
	 * @throws SecurityException if the user cannot view the channel
	 */
	public static PopulatedChannel add(ACARSConnection ac, String c) {
		try {
			_wLock.lock();
			remove(ac);
			PopulatedChannel pc = _channels.get(c.toLowerCase());
			if (pc != null) {
				if (!RoleUtils.hasAccess(ac.getUser().getRoles(), pc.getChannel().getViewRoles()))
					throw new SecurityException("Cannot view Channel");
			
				pc.add(ac.getID(), ac.getUser());
			}
		
			return pc;
		} finally {
			_wLock.unlock();
		}
	}
	
	/**
	 * Removes a user from all voice channels.
	 * @param p the Pilot to remove
	 * @return if the user was removed from any channels
	 */
	public static boolean remove(ACARSConnection ac) {
		boolean isLocked = _wLock.isHeldByCurrentThread();
		try {
			if (!isLocked)
				_wLock.lock();
			
			boolean wasRemoved = false;
			for (PopulatedChannel pc : _channels.values())
				wasRemoved |= pc.remove(ac.getID());
		
			return wasRemoved;
		} finally {
			if (!isLocked) 
				_wLock.unlock();
		}
	}
	
	/**
	 * Returns all active voice channels.
	 * @return a List of PopulatedChannel beans
	 */
	public static Collection<PopulatedChannel> getChannels() {
		try {
			_rLock.lock();
			return new ArrayList<PopulatedChannel>(_channels.values());
		} finally {
			_rLock.unlock();
		}
	}
	
	/**
	 * Removes empty transient voice channels.
	 */
	public static void removeEmpty() {
		try {
			_wLock.lock();
			for (Iterator<Map.Entry<String, PopulatedChannel>> i = _channels.entrySet().iterator(); i.hasNext(); ) {
				Map.Entry<String, PopulatedChannel> me = i.next();
				PopulatedChannel pc = me.getValue(); Channel ch = pc.getChannel();
				if (ch.getIsTemporary() && !pc.contains(ch.getOwner()))
					i.remove();
			}
		} finally {
			_wLock.unlock();
		}
	}

	/**
	 * Returns the number of voice channels.
	 * @return the number of channels
	 */
	public static int size() {
		try {
			_rLock.lock();
			return _channels.size();
		} finally {
			_rLock.unlock();
		}
	}
}