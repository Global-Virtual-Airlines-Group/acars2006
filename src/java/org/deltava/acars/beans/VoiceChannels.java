// Copyright 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

import java.util.*;

import org.deltava.beans.Pilot;
import org.deltava.beans.mvs.*;

import org.deltava.util.IPCUtils;

import org.gvagroup.ipc.IPCInfo;

/**
 * A bean to store voice channel information.
 * @author Luke
 * @version 4.0
 * @since 4.0
 */

public class VoiceChannels implements IPCInfo<Channel> {
	
	private static final Map<String, PopulatedChannel> _channels = new TreeMap<String, PopulatedChannel>();

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
	public static synchronized Channel get(String name) {
		PopulatedChannel pc = _channels.get(name.toLowerCase());
		return (pc == null) ? null : pc.getChannel();
	}

	/**
	 * Adds a voice channel to the list.
	 * @param c the Channel
	 * @throws IllegalStateException if the channel name is not unique
	 */
	public static synchronized void add(Channel c) {
		String name = c.getName().toLowerCase();
		if (_channels.containsKey(name))
			throw new IllegalStateException(name + " already exists");
		
		_channels.put(name, new PopulatedChannel(c));
	}
	
	/**
	 * Adds a user to a channel, removing from all other channels.
	 * @param p the Pilot to add
	 * @param c the Channel to add to
	 * @throws NullPointerException if p or c are null
	 */
	public static synchronized void add(Pilot p, String c) {
		remove(p);
		PopulatedChannel pc = _channels.get(c.toLowerCase());
		if (pc != null)
			pc.add(p);
	}
	
	/**
	 * Removes a user from all voice channels.
	 * @param p the Pilot to remove
	 * @return if the user was removed from any channels
	 */
	public static synchronized boolean remove(Pilot p) {
		boolean wasRemoved = false;
		for (PopulatedChannel pc : _channels.values())
			wasRemoved |= pc.remove(p);
		
		return wasRemoved;
	}
	
	/**
	 * Returns all active voice channels.
	 * @return a List of Channel beans
	 */
	public static synchronized Collection<Channel> getChannels() {
		Collection<Channel> results = new ArrayList<Channel>(_channels.size());
		for (PopulatedChannel pc : _channels.values())
			results.add(pc.getChannel());
		
		return results;
	}
	
	/**
	 * Removes empty transient voice channels.
	 */
	public static synchronized void removeEmpty() {
		for (Iterator<Map.Entry<String, PopulatedChannel>> i = _channels.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry<String, PopulatedChannel> me = i.next();
			PopulatedChannel pc = me.getValue();
			if ((pc.size() == 0) && pc.getChannel().getIsTemporary())
				i.remove();
		}
	}

	public synchronized Collection<byte[]> getSerializedInfo() {
		return IPCUtils.serialize(_channels.values());
	}
}