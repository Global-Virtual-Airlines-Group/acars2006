// Copyright 2011, 2012, 2016 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

import java.util.*;
import java.util.concurrent.locks.*;

import org.deltava.beans.mvs.*;
import org.deltava.util.*;

import org.gvagroup.ipc.IPCInfo;

/**
 * A bean to store voice channel information.
 * @author Luke
 * @version 7.0
 * @since 4.0
 */

public class VoiceChannels implements java.io.Serializable, IPCInfo<PopulatedChannel> {
	
	private final Map<String, PopulatedChannel> _channels = new TreeMap<String, PopulatedChannel>();
	
	private final ReentrantReadWriteLock _rwLock = new ReentrantReadWriteLock(true);
	private final Lock _rLock = _rwLock.readLock();
	private final ReentrantReadWriteLock.WriteLock _wLock = _rwLock.writeLock();
	
	private static final VoiceChannels _instance = new VoiceChannels();
	
	private final Channel LOBBY = new Channel(Channel.DEFAULT_NAME) {{
		setDescription("The MVS Lobby");
		setIsDefault(true);
		setSampleRate(SampleRate.SR6K);
		addRole(Access.VIEW, "*");
		addRole(Access.TALK, "PIREP");
		addRole(Access.TALK, "Instructor");
		addRole(Access.TALK, "Dispatch");
		addRole(Access.TALK_IF_PRESENT, "HR");
		addRole(Access.TALK_IF_PRESENT, "Dispatch");
		addRole(Access.TALK_IF_PRESENT, "Operations");
		addRole(Access.ADMIN, "HR");
	}};

	// singleton
	private VoiceChannels() {
		super();
		_channels.put("lobby", new PopulatedChannel(LOBBY));
	}
	
	public static VoiceChannels getInstance() {
		return _instance;
	}
	
	/**
	 * Retrieves a voice channel.
	 * @param name the channel name
	 * @return a Channel, or null if not found
	 * @throws NullPointerException if name is null 
	 */
	public PopulatedChannel get(String name) {
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
	 * @return the PopulatedChannel which the user was added to, or null
	 * @throws IllegalStateException if the channel name is not unique
	 */
	public PopulatedChannel add(ACARSConnection ac, Channel c) {
		try {
			_wLock.lock();
			String name = c.getName().toLowerCase();
			if (_channels.containsKey(name))
				throw new IllegalStateException(name + " already exists");

			// Add the user to the channel if present
			PopulatedChannel pc = new PopulatedChannel(c);
			_channels.put(name, pc);
			if (ac != null) {
				remove(ac.getID());
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
	public PopulatedChannel get(long conID) {
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
	 * @throws SecurityException if the user cannot view the channel, or cannot create a new one 
	 */
	public PopulatedChannel add(ACARSConnection ac, String c) {
		try {
			_wLock.lock();
			PopulatedChannel pc = _channels.get(c.toLowerCase());
			if (pc != null) {
				if (!RoleUtils.hasAccess(ac.getUser().getRoles(), pc.getChannel().getViewRoles()))
					throw new SecurityException("Cannot view Channel");
			
				remove(ac.getID());
				pc.add(ac.getID(), ac.getUser());
			}
		
			return pc;
		} finally {
			_wLock.unlock();
		}
	}
	
	/**
	 * Removes a user from all voice channels.
	 * @param id the Connection ID
	 * @return if the user was removed from any channels
	 */
	public boolean remove(long id) {
		boolean isLocked = _wLock.isHeldByCurrentThread();
		try {
			if (!isLocked)
				_wLock.lock();
			
			boolean wasRemoved = false;
			for (PopulatedChannel pc : _channels.values())
				wasRemoved |= pc.remove(id);
		
			return wasRemoved;
		} finally {
			if (!isLocked) 
				_wLock.unlock();
		}
	}
	
	/**
	 * Removes a voice channel.
	 * @param name the channel name
	 * @return TRUE if removed, otherwise FALSE
	 */
	public boolean remove(String name) {
		try {
			_wLock.lock();
			PopulatedChannel pc = _channels.get(name.toLowerCase());
			if (pc == null)
				return false;
			
			// Remove only if empty
			if (pc.size() == 0)
				return (_channels.remove(name.toLowerCase()) != null);
		
			return false;
		} finally {
			_wLock.unlock();
		}
	}
	
	/**
	 * Returns all active voice channels.
	 * @return a List of PopulatedChannel beans
	 */
	public Collection<PopulatedChannel> getChannels() {
		try {
			_rLock.lock();
			return new ArrayList<PopulatedChannel>(_channels.values());
		} finally {
			_rLock.unlock();
		}
	}
	
	/**
	 * Finds empty transient voice channels. If a channel is completely empty
	 * it is removed. If its owner is no longer present but users are, it is returned
	 * for the caller to do somethig intelligent.
	 * @return a Collection of PopulatedChannel beans
	 */
	public Collection<PopulatedChannel> findEmpty() {
		try {
			_wLock.lock();
			Collection<PopulatedChannel> results = new ArrayList<PopulatedChannel>();
			for (Iterator<Map.Entry<String, PopulatedChannel>> i = _channels.entrySet().iterator(); i.hasNext(); ) {
				Map.Entry<String, PopulatedChannel> me = i.next();
				PopulatedChannel pc = me.getValue(); Channel ch = pc.getChannel();
				if (ch.getIsTemporary() && !pc.contains(ch.getOwner())) {
					if (pc.size() == 0)
						i.remove();
					else
						results.add(pc);
				}
			}
			
			return results;
		} finally {
			_wLock.unlock();
		}
	}

	/**
	 * Returns the number of voice channels.
	 * @return the number of channels
	 */
	public int size() {
		try {
			_rLock.lock();
			return _channels.size();
		} finally {
			_rLock.unlock();
		}
	}

	@Override
	public Collection<byte[]> getSerializedInfo() {
		return IPCUtils.serialize(getChannels());
	}
}