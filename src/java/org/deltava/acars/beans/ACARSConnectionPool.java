// Copyright 2005 Luke J. Kolin. All Rights Reserved.
package org.deltava.acars.beans;

import java.util.*;
import java.nio.channels.*;

import org.deltava.acars.ACARSException;

import org.deltava.beans.Pilot;
import org.deltava.beans.acars.*;
import org.deltava.beans.servinfo.*;

import org.deltava.acars.message.*;
import org.deltava.acars.util.RouteEntryHelper;

/**
 * A TCP/IP Connection Pool.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class ACARSConnectionPool implements ServInfoProvider, ACARSAdminInfo {

	// Hard-coded anonymous inactivity timeout (in ms)
	private static final long ANONYMOUS_INACTIVITY_TIMEOUT = 25000;

	// List of connections, disconnected connections and connection pool info
	private int _maxSize;
	private List _cons;
	private List _disCon;

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
		_cons = new ArrayList();
		_disCon = new ArrayList();
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
	public Collection getMapEntries() {
	   Set results = new HashSet();
	   for (Iterator i = _cons.iterator(); i.hasNext(); ) {
			ACARSConnection con = (ACARSConnection) i.next();
			PositionMessage posInfo = con.getPosition();
			InfoMessage usrInfo = con.getFlightInfo();
			if ((usrInfo != null) && (posInfo != null))
			   results.add(RouteEntryHelper.build(con.getUser(), posInfo, usrInfo));
	   }
	   
	   return results;
	}
	
	/**
	 * Returns Connection Pool data to a web application.
	 * @return a Collection of ACARSConnection beans
	 */
	public Collection getPoolInfo() {
	   return new ArrayList(_cons);
	}

	/**
	 * Adds a new connection to the pool.
	 * @param c the connection to add
	 * @throws ACARSException if the connection exists, the pool is fool or a network
	 * error occurs
	 */
	public void add(ACARSConnection c) throws ACARSException {

		// Check if we're already there
		if (_cons.contains(c))
			throw new ACARSException("Connection already in pool");

		if (_cons.size() >= _maxSize)
			throw new ACARSException("Connection Pool full");

		// Register the SocketChannel with the selector
		try {
			c.getChannel().register(_cSelector, SelectionKey.OP_READ);
			_cons.add(c);
		} catch (ClosedChannelException cce) {
			throw new ACARSException(cce.getMessage());
		}
	}

	public Collection checkConnections() {

		// Start with the list of dropped connections
		List disCons = new ArrayList(_disCon);
		_disCon.clear();

		// Build list of dropped connections; return it with just the dropped connections if we have no timeout
		if (_inactivityTimeout == -1)
			return disCons;

		// Get current time to save repeated system calls
		long now = System.currentTimeMillis();

		// Loop through the channels
		for (Iterator i = _cons.iterator(); i.hasNext();) {
			ACARSConnection con = (ACARSConnection) i.next();

			// Calculate the inactivity timeout
			long timeout = con.isAuthenticated() ? _inactivityTimeout : ANONYMOUS_INACTIVITY_TIMEOUT;

			// Have we exceeded the timeout interval
			if ((now - con.getLastActivity()) > timeout) {
				con.close();
				i.remove();
				disCons.add(con);
			}
		}

		// Return the list of dropped connections
		return disCons;
	}

	public boolean hasConnection(String remoteAddr) {

		// Loop through the connections
		Iterator i = _cons.iterator();
		while (i.hasNext()) {
			ACARSConnection c = (ACARSConnection) i.next();
			if (c.getRemoteAddr().equals(remoteAddr))
				return true;
		}

		// No connection from that address found, return false
		return false;
	}

	public Collection getAll() {
		return _cons;
	}

	public ACARSConnection get(long cid) {
		for (Iterator i = _cons.iterator(); i.hasNext();) {
			ACARSConnection c = (ACARSConnection) i.next();
			if (c.equals(cid))
				return c;
		}

		// Return nothing if not found
		return null;
	}

	public Collection get(String pid) {

		// Wildcard matches everyone
		if (("*".equals(pid)) || (pid == null))
			return new TreeSet(_cons);

		// Build results
		Set results = new TreeSet();
		for (Iterator i = _cons.iterator(); i.hasNext();) {
			ACARSConnection c = (ACARSConnection) i.next();
			if (c.getUserID().equalsIgnoreCase(pid))
				results.add(c);
		}

		return results;
	}

	public Iterator get(Pilot userInfo) {

		// Build results list
		ArrayList results = new ArrayList();

		// Loop through the connections
		for (Iterator i = _cons.iterator(); i.hasNext();) {
			ACARSConnection c = (ACARSConnection) i.next();
			if ((c.isAuthenticated()) && (c.getUser().equals(userInfo)))
				results.add(c);
		}

		// Return the iterator
		return results.iterator();
	}

	public ACARSConnection get(SocketChannel ch) {

		// Loop through the connections
		Iterator i = _cons.iterator();
		while (i.hasNext()) {
			ACARSConnection c = (ACARSConnection) i.next();
			if (c.equals(ch))
				return c;
		}

		// Return nothing if not found
		return null;
	}
	
	public boolean isEmpty() {
		return _cons.isEmpty();
	}

	public void read() {
		Collection keys = _cSelector.selectedKeys();
		if (keys.isEmpty())
			return;

		// Get the list of channels waiting for input
		for (Iterator i = keys.iterator(); i.hasNext();) {
			SelectionKey sKey = (SelectionKey) i.next();

			// If the selection key is ready for reading, get the Connection and read
			if (sKey.isReadable()) {
				ACARSConnection con = get((SocketChannel) sKey.channel());

				try {
					String msg = con.read();
					if (msg != null) {
						Envelope env = new Envelope(con.getUser(), msg, con.getID());
						MessageStack.RAW_INPUT.push(env);
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
	}

	public void remove(String id) {

		for (Iterator i = _cons.iterator(); i.hasNext();) {
			ACARSConnection c = (ACARSConnection) i.next();

			// Close/Remove if found
			if (c.equals(id)) {
				if (c.isConnected())
					c.close();
				i.remove();
				break;
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

	public void write() {
		// Loop through the raw output stack
		while (MessageStack.RAW_OUTPUT.hasNext()) {
			Envelope env = MessageStack.RAW_OUTPUT.pop();

			// Get the connection and write the message
			ACARSConnection c = get(env.getConnectionID());
			if (c != null)
				c.write((String) env.getMessage());
		}
	}
}