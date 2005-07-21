package org.deltava.acars.beans;

import java.net.*;
import java.util.*;
import java.nio.channels.*;

import org.apache.log4j.Logger;

import org.deltava.acars.ACARSException;

import org.deltava.beans.Pilot;
import org.deltava.beans.acars.*;
import org.deltava.beans.servinfo.*;

import org.deltava.acars.message.*;

/**
 * A TCP/IP Connection Pool.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class ACARSConnectionPool implements ServInfoProvider, ACARSAdminInfo {

	private static final Logger log = Logger.getLogger(ACARSConnectionPool.class);

	// Hard-coded anonymous inactivity timeout (in ms)
	private static final long ANONYMOUS_INACTIVITY_TIMEOUT = 15000;

	// List of connections, disconnected connections and connection pool info
	private int _maxSize;
	private List _cons;
	private List _disCon;

	// Inactivity timeout
	private long _inactivityTimeout = -1;

	// The selector to use for non-blocking I/O reads
	private Selector _cSelector;

	// The input/output stacks in raw text
	private MessageStack _inputStack;
	private MessageStack _outputStack;

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
			PositionMessage pos = (PositionMessage) con.getInfo(ACARSConnection.POSITION_INFO);
			InfoMessage usrInfo = (InfoMessage) con.getInfo(ACARSConnection.FLIGHT_INFO);

			// Add Pilots to servinfo data, if they're logged in and have info/position data
			if (con.isAuthenticated() && (pos != null) && (usrInfo != null)) {
				Pilot usr = con.getUser();

				// Build the pilot object
				org.deltava.beans.servinfo.Pilot p = new org.deltava.beans.servinfo.Pilot(usr.getID());
				p.setName(usr.getName());
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
		ArrayList disCons = new ArrayList(_disCon);
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
			return _cons;

		// Build results
		Set results = new HashSet();
		for (Iterator i = _cons.iterator(); i.hasNext();) {
			ACARSConnection c = (ACARSConnection) i.next();
			if (c.getUserID().equals(pid))
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

	public void read() {

		// Get the list of channels waiting for input
		for (Iterator i = _cSelector.selectedKeys().iterator(); i.hasNext();) {
			SelectionKey sKey = (SelectionKey) i.next();

			// If the selection key is ready for reading, get the Connection and read
			if (sKey.isReadable()) {
				ACARSConnection con = get((SocketChannel) sKey.channel());

				try {
					String msg = con.read();
					if (msg != null) {
						Envelope env = new Envelope(con.getUser(), msg, con.getID());
						_inputStack.push(env);
					}
				} catch (SocketException se) {
					log.error(se.getMessage(), se);
					con.close();
					_cons.remove(con);
				} catch (ProtocolException pe) {
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

	public void setStacks(MessageStack is, MessageStack os) throws ACARSException {

		// Check that the stacks are not null
		if ((is == null) || (os == null))
			throw new ACARSException("Input/Output stacks are NULL");

		// Check that we're not resetting anything
		if ((_inputStack == null) && (_outputStack == null)) {
			_inputStack = is;
			_outputStack = os;
		}
	}

	public void setTimeout(int toSeconds) {
		_inactivityTimeout = (toSeconds < 60) ? -1 : (toSeconds * 1000);
	}

	public void write() {
		// Loop through the raw output stack
		while (_outputStack.hasNext()) {
			Envelope env = _outputStack.pop();

			// Get the connection and write the message
			ACARSConnection c = get(env.getConnectionID());
			if (c != null)
				c.write((String) env.getMessage());
		}
	}
}