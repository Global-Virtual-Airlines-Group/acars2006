// Copyright 2004, 2005, 2006, 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

import java.util.*;
import java.nio.channels.*;

import org.apache.log4j.Logger;

import org.deltava.acars.ACARSException;

import org.deltava.beans.Pilot;
import org.deltava.beans.acars.*;
import org.deltava.acars.message.InfoMessage;

import org.deltava.util.IPCUtils;

import org.deltava.acars.security.*;
import org.deltava.acars.util.RouteEntryHelper;

import org.gvagroup.acars.ACARSAdminInfo;

/**
 * A TCP/IP Connection Pool for ACARS Connections.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class ACARSConnectionPool implements ACARSAdminInfo<RouteEntry> {

	private static final Logger log = Logger.getLogger(ACARSConnectionPool.class);

	// Hard-coded anonymous inactivity timeout (in ms)
	private static final long ANONYMOUS_INACTIVITY_TIMEOUT = 25000;

	// List of connections, disconnected connections and connection pool info
	private int _maxSize;
	private final List<ACARSConnection> _cons = new ArrayList<ACARSConnection>();
	private final Collection<ACARSConnection> _disCon = new ArrayList<ACARSConnection>();

	// Inactivity timeout
	private long _inactivityTimeout = -1;

	// The selector to use for non-blocking I/O reads
	private Selector _cSelector;

	/**
	 * Creates a new ACARS Connection Pool.
	 * @param mxSize the maximum size of the pool
	 */
	public ACARSConnectionPool(int mxSize) {
		super();
		_maxSize = (mxSize > 0) ? mxSize : -1;
	}

	/**
	 * Returns network data in a format suitable for Google Maps.
	 * @return a Collection of MapEntry beans
	 */
	public Collection<RouteEntry> getMapEntries() {
		Collection<ACARSConnection> cons = new ArrayList<ACARSConnection>(_cons);
		Collection<RouteEntry> results = new LinkedHashSet<RouteEntry>(cons.size());
		for (Iterator<ACARSConnection> i = cons.iterator(); i.hasNext();) {
			ACARSConnection con = i.next();
			RouteEntry re = RouteEntryHelper.build(con);
			if (re != null)
				results.add(re);
		}

		return results;
	}
	
	/**
	 * Returns network data in a serialized form for transfer between virtual machines or class loaders.
	 * @return a Collection of byte arrays
	 */
	public Collection<byte[]> getSerializedInfo() {
		Collection<ACARSConnection> cons = new ArrayList<ACARSConnection>(_cons);
		Collection<RouteEntry> results = new ArrayList<RouteEntry>(cons.size());
		for (Iterator<ACARSConnection> i = cons.iterator(); i.hasNext();) {
			RouteEntry re = RouteEntryHelper.build(i.next());
			if (re != null)
				results.add(re);
		}
		
		return IPCUtils.serialize(results);
	}
	
	/**
	 * Returns the flight IDs of all active flights.
	 * @return a Collection of Integer flight IDs
	 */
	public Collection<Integer> getFlightIDs() {
		Collection<Integer> results = new TreeSet<Integer>();
		Collection<ACARSConnection> cons = new ArrayList<ACARSConnection>(_cons);
		for (Iterator<ACARSConnection> i = cons.iterator(); i.hasNext();) {
			ACARSConnection con = i.next();
			int id = con.getFlightID();
			if (id != 0)
				results.add(new Integer(id));
		}
		
		return results;
	}

	/**
	 * Returns Connection Pool data to a web application.
	 * @param showHidden TRUE if stealth connections should be displayed, otherwise FALSE
	 * @return a Collection of serialized ConnectionEntry beans
	 */
	public Collection<byte[]> getPoolInfo(boolean showHidden) {
		Collection<ACARSConnection> cons = new ArrayList<ACARSConnection>(_cons);
		Collection<ConnectionEntry> results = new ArrayList<ConnectionEntry>(cons.size());
		for (Iterator<ACARSConnection> i = cons.iterator(); i.hasNext(); ) {
			ACARSConnection ac = i.next();
			if (showHidden || !ac.getUserHidden()) {
				ConnectionEntry entry = new ConnectionEntry(ac.getID());
				entry.setClientBuild(ac.getClientVersion());
				entry.setRemoteAddr(ac.getRemoteAddr());
				entry.setRemoteHost(ac.getRemoteHost());
				entry.setMessages(ac.getMsgsIn(), ac.getMsgsOut());
				entry.setBytes(ac.getBytesIn(), ac.getBytesOut());
				entry.setBufferWrites(ac.getBufferWrites());
				entry.setStartTime(new Date(ac.getStartTime()));
				entry.setUser(ac.getUser());
				entry.setDispatch(ac.getIsDispatch());
				entry.setUserHidden(ac.getUserHidden());
				
				// Get the flight information
				InfoMessage inf = ac.getFlightInfo();
				FlightInfo info = new FlightInfo(ac.getID());
				if (inf != null) {
					info.setFlightCode(inf.getFlightCode());
					info.setAirportD(inf.getAirportD());
					info.setAirportA(inf.getAirportA());
					info.setEquipmentType(inf.getEquipmentType());
					info.setConnectionID(ac.getID());
					info.setPilotID(ac.getUser().getID());
					info.setFSVersion(inf.getFSVersion());
					if (inf.getFlightID() != 0)
						info.setID(inf.getFlightID());
				}
				
				// Save the flight info
				entry.setFlightInfo(info);
				results.add(entry);
			}
		}
		
		return IPCUtils.serialize(results); 
	}
	
	/**
	 * Returns banned user data to a web application.
	 * @return a Collection of BannedUser beans
	 */
	public Collection<BannedUser> getBanInfo() {
		return UserBlocker.getBans();
	}

	/**
	 * Adds a new connection to the pool.
	 * @param c the connection to add
	 * @throws ACARSException if the connection exists, the pool is fool or a network error occurs
	 */
	public void add(ACARSConnection c) throws ACARSException {

		// Check if we're already there
		if (_cons.contains(c))
			throw new ACARSException("Connection already in pool");
		else if (_cons.size() >= _maxSize)
			throw new ACARSException("Connection Pool full");

		// Register the SocketChannel with the selector
		try {
			c.getChannel().register(_cSelector, SelectionKey.OP_READ);
			_cons.add(c);
		} catch (ClosedChannelException cce) {
			throw new ACARSException(cce.getMessage());
		}
	}

	public Collection<ACARSConnection> checkConnections() {

		// Start with the list of dropped connections
		List<ACARSConnection> disCons = new ArrayList<ACARSConnection>(_disCon);
		_disCon.clear();

		// Build list of dropped connections; return it with just the dropped connections if we have no timeout
		if (_inactivityTimeout == -1)
			return disCons;

		// Get current time to save repeated system calls
		long now = System.currentTimeMillis();

		// Loop through the channels
		for (Iterator<ACARSConnection> i = _cons.iterator(); i.hasNext();) {
			ACARSConnection con = i.next();

			// Calculate the inactivity timeout
			long timeout = con.isAuthenticated() ? _inactivityTimeout : ANONYMOUS_INACTIVITY_TIMEOUT;
			long idleTime = now - con.getLastActivity();

			// Have we exceeded the timeout interval
			if (idleTime > timeout) {
				log.warn(con.getUserID() + " logged out after " + idleTime + "ms of inactivity");
				con.close();
				i.remove();
				disCons.add(con);
			}
		}

		// Return the list of dropped connections
		return disCons;
	}

	public ACARSConnection getFrom(String remoteAddr) {

		// Loop through the connections
		Iterator<ACARSConnection> i = _cons.iterator();
		while (i.hasNext()) {
			ACARSConnection c = i.next();
			if (c.getRemoteAddr().equals(remoteAddr))
				return c;
		}

		// No connection from that address found, return false
		return null;
	}

	public ACARSConnection get(long cid) {
		for (Iterator<ACARSConnection> i = new ArrayList<ACARSConnection>(_cons).iterator(); i.hasNext();) {
			ACARSConnection c = i.next();
			if (c.getID() == cid)
				return c;
		}

		// Return nothing if not found
		return null;
	}

	public Collection<ACARSConnection> get(String pid) {

		// Wildcard matches everyone
		if (("*".equals(pid)) || (pid == null))
			return new ArrayList<ACARSConnection>(_cons);

		// Build results
		Collection<ACARSConnection> results = new ArrayList<ACARSConnection>();
		for (Iterator<ACARSConnection> i = _cons.iterator(); i.hasNext();) {
			ACARSConnection c = i.next();
			if (c.getUserID().equalsIgnoreCase(pid))
				results.add(c);
		}

		return results;
	}

	public Iterator<ACARSConnection> get(Pilot userInfo) {

		// Build results list
		ArrayList<ACARSConnection> results = new ArrayList<ACARSConnection>();

		// Loop through the connections
		for (Iterator<ACARSConnection> i = _cons.iterator(); i.hasNext();) {
			ACARSConnection c = i.next();
			if ((c.isAuthenticated()) && (c.getUser().equals(userInfo)))
				results.add(c);
		}

		// Return the iterator
		return results.iterator();
	}

	public ACARSConnection get(SocketChannel ch) {

		// Loop through the connections
		for (Iterator<ACARSConnection> i = _cons.iterator(); i.hasNext();) {
			ACARSConnection c = i.next();
			if (c.getChannel().equals(ch))
				return c;
		}

		// Return nothing if not found
		return null;
	}

	public boolean isEmpty() {
		return _cons.isEmpty();
	}
	
	public boolean isDispatchOnline() {
		for (Iterator<ACARSConnection> i = _cons.iterator(); i.hasNext();) {
			ACARSConnection c = i.next();
			if (c.getIsDispatch())
				return true;
		}
		
		return false;
	}

	public Collection<TextEnvelope> read() {
		Collection<SelectionKey> keys = _cSelector.selectedKeys();
		if ((keys == null) || (keys.isEmpty()))
			return Collections.emptySet();

		// Get the list of channels waiting for input
		Collection<TextEnvelope> results = new ArrayList<TextEnvelope>();
		for (Iterator<SelectionKey> i = keys.iterator(); i.hasNext();) {
			SelectionKey sKey = i.next();

			// If the selection key is ready for reading, get the Connection and read
			if (sKey.isValid() && sKey.isReadable()) {
				ACARSConnection con = get((SocketChannel) sKey.channel());

				try {
					String msg = con.read();
					if (msg != null) {
						TextEnvelope env = new TextEnvelope(con.getUser(), msg, con.getID());
						env.setVersion(con.getProtocolVersion());
						results.add(env);
					}
				} catch (Exception e) {
					con.close();
					_cons.remove(con);
					_disCon.add(con);
				}
			}

			// Remove from the selected keys list
			i.remove();
		}

		// Return messages
		return results;
	}

	public void remove(ACARSConnection c) {

		// Find the connection
		int pos = _cons.indexOf(c);
		if (pos != -1) {
			c.close();
			_cons.remove(pos);
		}
	}

	public void setSelector(Selector cs) {
		_cSelector = cs;
	}

	public void setTimeout(int toSeconds) {
		_inactivityTimeout = (toSeconds < 60) ? -1 : (toSeconds * 1000);
	}
}