// Copyright 2004, 2005, 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

import java.util.*;
import java.nio.channels.*;

import org.apache.log4j.Logger;

import org.deltava.acars.ACARSException;

import org.deltava.beans.Pilot;
import org.deltava.beans.acars.*;
import org.deltava.beans.servinfo.*;

import org.deltava.acars.message.*;
import org.deltava.acars.security.*;

import org.deltava.acars.util.RouteEntryHelper;

/**
 * A TCP/IP Connection Pool.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class ACARSConnectionPool implements ServInfoProvider, ACARSAdminInfo {

	private static final Logger log = Logger.getLogger(ACARSConnectionPool.class);

	// Hard-coded anonymous inactivity timeout (in ms)
	private static final long ANONYMOUS_INACTIVITY_TIMEOUT = 25000;

	// List of connections, disconnected connections and connection pool info
	private int _maxSize;
	private List<ACARSConnection> _cons;
	private List<ACARSConnection> _disCon;

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

		// Init the maximum size and connection lists
		_maxSize = (mxSize > 0) ? mxSize : -1;
		_cons = new ArrayList<ACARSConnection>();
		_disCon = new ArrayList<ACARSConnection>();
	}

	/**
	 * Returns ServIno-format connection data.
	 * @return a ServInfo network information bean
	 * @see ACARSConnectionPool#getNetworkStatus()
	 */
	public NetworkInfo getNetworkInfo() {
		NetworkInfo info = new NetworkInfo("ACARS");
		info.setVersion("1");
		info.setValidDate(new Date());
		for (Iterator i = _cons.iterator(); i.hasNext();) {
			ACARSConnection con = (ACARSConnection) i.next();
			PositionMessage pos = con.getPosition();
			InfoMessage usrInfo = con.getFlightInfo();

			// Add Pilots to servinfo data, if they're logged in and have info/position data
			if (con.isAuthenticated() && (pos != null) && (usrInfo != null)) {
				Pilot usr = con.getUser();

				// Build the pilot object
				org.deltava.beans.servinfo.Pilot p = new org.deltava.beans.servinfo.Pilot(usr.getID());
				p.setName(usr.getName() + " " + usr.getHomeAirport());
				p.setPilotID(usr.getID());

				// Pass position information
				p.setAltitude(pos.getAltitude());
				p.setGroundSpeed(pos.getGspeed());
				p.setPosition(pos.getLatitude(), pos.getLongitude());

				// Pass back user info
				p.setAirportD(usrInfo.getAirportD());
				p.setAirportA(usrInfo.getAirportA());
				p.setCallsign(usrInfo.getFlightCode());
				p.setEquipmentCode(usrInfo.getEquipmentType());
				p.setComments(usrInfo.getComments());
				p.setWayPoints(usrInfo.getAllWaypoints(' '));

				// Add the pilot object
				info.add(p);
			}
		}

		return info;
	}

	/**
	 * Returns ServIno-format network data.
	 * @return a ServInfo network bean
	 * @see ACARSConnectionPool#getNetworkInfo()
	 */
	public NetworkStatus getNetworkStatus() {
		return new NetworkStatus("ACARS");
	}

	/**
	 * Returns network data in a format suitable for Google Maps.
	 * @return a Collection of MapEntry beans
	 */
	public Collection<RouteEntry> getMapEntries() {
		Set<RouteEntry> results = new HashSet<RouteEntry>();
		for (Iterator<ACARSConnection> i = _cons.iterator(); i.hasNext();) {
			ACARSConnection con = i.next();
			RouteEntry re = RouteEntryHelper.build(con);
			if (re != null)
				results.add(re);
		}

		return results;
	}

	/**
	 * Returns Connection Pool data to a web application.
	 * @return a Collection of ACARSConnection beans
	 */
	public Collection<ACARSConnection> getPoolInfo() {
		return new ArrayList<ACARSConnection>(_cons);
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

	public Collection<ACARSConnection> getAll() {
		return _cons;
	}

	public ACARSConnection get(long cid) {
		for (Iterator<ACARSConnection> i = _cons.iterator(); i.hasNext();) {
			ACARSConnection c = i.next();
			if (c.equals(cid))
				return c;
		}

		// Return nothing if not found
		return null;
	}

	public Collection<ACARSConnection> get(String pid) {

		// Wildcard matches everyone
		if (("*".equals(pid)) || (pid == null))
			return new TreeSet<ACARSConnection>(_cons);

		// Build results
		Set<ACARSConnection> results = new TreeSet<ACARSConnection>();
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
			if (c.equals(ch))
				return c;
		}

		// Return nothing if not found
		return null;
	}

	public boolean isEmpty() {
		return _cons.isEmpty();
	}

	public Collection<Envelope> read() {
		Collection<SelectionKey> keys = _cSelector.selectedKeys();
		if (keys.isEmpty())
			return Collections.emptySet();

		// Get the list of channels waiting for input
		Collection<Envelope> results = new ArrayList<Envelope>();
		for (Iterator<SelectionKey> i = keys.iterator(); i.hasNext();) {
			SelectionKey sKey = i.next();

			// If the selection key is ready for reading, get the Connection and read
			if (sKey.isValid() && sKey.isReadable()) {
				ACARSConnection con = get((SocketChannel) sKey.channel());

				try {
					String msg = con.read();
					if (msg != null) {
						Envelope env = new Envelope(con.getUser(), msg, con.getID());
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

	public void remove(String id) {
		for (Iterator<ACARSConnection> i = _cons.iterator(); i.hasNext();) {
			ACARSConnection c = i.next();

			// Close/Remove if found
			if (c.equals(id)) {
				if (c.isConnected())
					c.close();

				return;
			}
		}
	}

	public void remove(ACARSConnection c) {

		// Find the connection
		int pos = _cons.indexOf(c);
		if (pos != -1) {
			if (c.isConnected())
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