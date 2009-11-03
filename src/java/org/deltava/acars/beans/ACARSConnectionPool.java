// Copyright 2004, 2005, 2006, 2007, 2008, 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

import java.util.*;
import java.util.concurrent.*;
import java.nio.channels.*;

import org.apache.log4j.Logger;

import org.deltava.acars.ACARSException;

import org.deltava.beans.acars.*;
import org.deltava.beans.GeoLocation;
import org.deltava.beans.schedule.GeoPosition;

import org.deltava.acars.message.InfoMessage;

import org.deltava.util.IPCUtils;

import org.deltava.acars.util.RouteEntryHelper;

import org.gvagroup.acars.ACARSAdminInfo;

/**
 * A TCP/IP Connection Pool for ACARS Connections.
 * @author Luke
 * @version 2.7
 * @since 1.0
 */

public class ACARSConnectionPool implements ACARSAdminInfo<ACARSMapEntry> {

	private static final Logger log = Logger.getLogger(ACARSConnectionPool.class);

	// Hard-coded anonymous inactivity timeout (in ms)
	private static final long ANONYMOUS_INACTIVITY_TIMEOUT = 22500;

	// List of connections, disconnected connections and connection pool info
	private int _maxSize;
	private final ConcurrentMap<Object, ACARSConnection> _cons = new ConcurrentHashMap<Object, ACARSConnection>();
	private transient final BlockingQueue<ACARSConnection> _disCon = new LinkedBlockingQueue<ACARSConnection>();
	private transient final Collection<ConnectionStats> _disConStats = new HashSet<ConnectionStats>();
	
	// Inactivity timeout
	private long _inactivityTimeout = -1;
	
	// Last inactivity check time
	private long _inactivityLastRun = 0;

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
	 * Returns all ACARS Connections.
	 * @return a Collection of ACARSConnection beans
	 */
	public Collection<ACARSConnection> getAll() {
		return new LinkedHashSet<ACARSConnection>(_cons.values());
	}

	/**
	 * Returns network data in a format suitable for Google Maps.
	 * @return a Collection of MapEntry beans
	 */
	public Collection<ACARSMapEntry> getMapEntries() {
		Collection<ACARSConnection> cons = getAll();
		Collection<ACARSMapEntry> results = new ArrayList<ACARSMapEntry>(cons.size());
		for (Iterator<ACARSConnection> i = cons.iterator(); i.hasNext();) {
			ACARSConnection con = i.next();
			ACARSMapEntry re = RouteEntryHelper.build(con);
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
		Collection<ACARSConnection> cons = getAll();
		Collection<ACARSMapEntry> results = new ArrayList<ACARSMapEntry>(cons.size());
		for (Iterator<ACARSConnection> i = cons.iterator(); i.hasNext();) {
			ACARSMapEntry re = RouteEntryHelper.build(i.next());
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
		Collection<ACARSConnection> cons = getAll();
		Collection<Integer> results = new TreeSet<Integer>();
		for (Iterator<ACARSConnection> i = cons.iterator(); i.hasNext();) {
			ACARSConnection con = i.next();
			int id = con.getFlightID();
			if (id != 0)
				results.add(Integer.valueOf(id));
		}
		
		return results;
	}

	/**
	 * Returns the time of the last connection inacitivity check.
	 * @return the date/time of the last inactivity check
	 */
	public Date getLastInactivityCheck() {
		return new Date(_inactivityLastRun);
	}
	
	/**
	 * Returns Connection Pool data to a web application.
	 * @param showHidden TRUE if stealth connections should be displayed, otherwise FALSE
	 * @return a Collection of serialized ConnectionEntry beans
	 */
	public Collection<byte[]> getPoolInfo(boolean showHidden) {
		Collection<ACARSConnection> cons = getAll();
		Collection<ConnectionEntry> results = new ArrayList<ConnectionEntry>(cons.size());
		for (Iterator<ACARSConnection> i = cons.iterator(); i.hasNext(); ) {
			ACARSConnection ac = i.next();
			if (showHidden || !ac.getUserHidden()) {
				ConnectionEntry entry = ac.getIsDispatch() ? new DispatchConnectionEntry(ac.getID()) : new ConnectionEntry(ac.getID());
				entry.setClientBuild(ac.getClientVersion());
				entry.setBeta(ac.getBeta());
				entry.setRemoteAddr(ac.getRemoteAddr());
				entry.setRemoteHost(ac.getRemoteHost());
				entry.setAddressInfo(ac.getAddressInfo());
				entry.setMessages(ac.getMsgsIn(), ac.getMsgsOut());
				entry.setBytes(ac.getBytesIn(), ac.getBytesOut());
				entry.setBufferReads(ac.getBufferReads());
				entry.setBufferWrites(ac.getBufferWrites());
				entry.setStartTime(new Date(ac.getStartTime()));
				entry.setUser(ac.getUser());
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
	 * Returns ACARS connection statistics.
	 * @return a Collection of CollectionStats beans
	 */
	public Collection<ConnectionStats> getStatistics() {
		ArrayList<ConnectionStats> results = new ArrayList<ConnectionStats>();
		results.addAll(getAll());
		results.addAll(_disConStats);
		_disConStats.clear();
		return results;
	}
	
	/**
	 * Adds a new connection to the pool.
	 * @param c the connection to add
	 * @throws ACARSException if the connection exists, the pool is full or a network error occurs
	 */
	public synchronized void add(ACARSConnection c) throws ACARSException {

		// Check if we're already there, and just adding a USER ID
		if (_cons.containsValue(c)) {
			_cons.putIfAbsent(c.getUserID(), c);
			return;
		} else if (size() >= _maxSize)
			throw new ACARSException("Connection Pool full - " + size() + " connections");

		// Register the SocketChannel with the selector
		try {
			_cSelector.wakeup();
			SocketChannel sc = c.getChannel();
			sc.register(_cSelector, SelectionKey.OP_READ);
			_cons.put(Long.valueOf(c.getID()), c);
			_cons.put(sc, c);
			_cons.putIfAbsent(c.getRemoteAddr(), c);
		} catch (ClosedChannelException cce) {
			throw new ACARSException(cce);
		}
	}

	public Collection<ACARSConnection> checkConnections() {

		// Start with the list of dropped connections
		List<ACARSConnection> disCons = new ArrayList<ACARSConnection>(_disCon.size() + 2);
		_disCon.drainTo(disCons);

		// Build list of dropped connections; return it with just the dropped connections if we have no timeout
		if (_inactivityTimeout == -1)
			return disCons;
		
		// Check if we need to do another check
		long now = System.currentTimeMillis();
		if ((now - _inactivityLastRun) <= 2500)
			return disCons;

		// Loop through the channels
		_inactivityLastRun = now;
		for (Iterator<ACARSConnection> i = getAll().iterator(); i.hasNext();) {
			ACARSConnection con = i.next();

			// Calculate the inactivity timeout
			long timeout = con.isAuthenticated() ? _inactivityTimeout : ANONYMOUS_INACTIVITY_TIMEOUT;
			long idleTime = now - con.getLastActivity();

			// Have we exceeded the timeout interval
			if (idleTime > timeout) {
				log.warn(con.getUserID() + " logged out after " + idleTime + "ms of inactivity");
				con.close();
				remove(con);
				
				// Add statistics
				ACARSConnectionStats ds = new ACARSConnectionStats(con.getID());
				ds.setMessages(con.getMsgsIn(), con.getMsgsOut());
				ds.setBytes(con.getBytesIn(), con.getBytesOut());
				_disConStats.add(ds);
				disCons.add(con);
			}
		}
		
		// Return the list of dropped connections
		return disCons;
	}
	
	/**
	 * Returns an ACARS Connection based on a key. The key may be a {@link SocketChannel}, an IP
	 * address, a pilot ID or a Connection ID.
	 * @param o the key
	 * @return an ACARSConnection, or null if not found
	 */
	public ACARSConnection get(Object o) {
		return _cons.get(o);
	}
	
	/**
	 * Returns multi-player connections within a certain distance of a location
	 * @param loc the Location
	 * @param distance the distance in miles
	 * @return a List of ACARSConnection beans
	 */
	public List<ACARSConnection> getMP(GeoLocation loc, int distance) {
		GeoPosition gp = new GeoPosition(loc);
		List<ACARSConnection> results = new ArrayList<ACARSConnection>();
		for (Iterator<ACARSConnection> i = getAll().iterator(); i.hasNext();) {
			ACARSConnection c = i.next();
			if (c.getIsMP()) {
				int d = gp.distanceTo(c.getPosition());
				if ((d >= 0) && (d <= distance))
					results.add(c);
				}
		}
		
		return results;
	}

	/**
	 * Returns the size of the connection pool.
	 */
	public int size() {
		return getAll().size();
	}
	
	public boolean isDispatchOnline() {
		for (Iterator<ACARSConnection> i = getAll().iterator(); i.hasNext();) {
			ACARSConnection c = i.next();
			if (c.getIsDispatch())
				return true;
		}
		
		return false;
	}

	public Collection<TextEnvelope> read() {
		Collection<SelectionKey> keys = _cSelector.selectedKeys();
		if ((keys == null) || keys.isEmpty())
			return Collections.emptySet();

		// Get the list of channels waiting for input
		Collection<TextEnvelope> results = new ArrayList<TextEnvelope>(keys.size());
		for (Iterator<SelectionKey> i = keys.iterator(); i.hasNext();) {
			SelectionKey sKey = i.next();

			// If the selection key is ready for reading, get the Connection and read
			if (sKey.isValid() && sKey.isReadable()) {
				ACARSConnection con = _cons.get(sKey.channel());
				if (con != null) {
					try {
						String msg = con.read();
						if (msg != null) {
							TextEnvelope env = new TextEnvelope(con.getUser(), msg, con.getID());
							env.setVersion(con.getProtocolVersion());
							results.add(env);
						}
					} catch (Exception e) {
						con.close();
						remove(con);
						
						// Add statistics
						ACARSConnectionStats ds = new ACARSConnectionStats(con.getID()); 
						ds.setMessages(con.getMsgsIn(), con.getMsgsOut());
						ds.setBytes(con.getBytesIn(), con.getBytesOut());
						_disConStats.add(ds);
						_disCon.add(con);
					}
				}
			}

			// Remove from the selected keys list
			i.remove();
		}

		// Return messages
		return results;
	}

	public void remove(ACARSConnection c) {
		while (_cons.containsValue(c))
			_cons.values().remove(c);
	}

	public void setSelector(Selector cs) {
		_cSelector = cs;
	}

	public void setTimeout(int toSeconds) {
		_inactivityTimeout = (toSeconds < 60) ? -1 : (toSeconds * 1000);
	}
}