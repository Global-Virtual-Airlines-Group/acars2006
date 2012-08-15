// Copyright 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.nio.channels.*;

import org.apache.log4j.Logger;

import org.deltava.acars.ACARSException;

import org.deltava.beans.acars.*;
import org.deltava.beans.GeoLocation;
import org.deltava.beans.schedule.GeoPosition;

import org.deltava.acars.message.*;

import org.deltava.util.*;

import org.deltava.acars.util.RouteEntryHelper;
import org.gvagroup.acars.ACARSAdminInfo;

/**
 * A TCP/IP Connection Pool for ACARS Connections.
 * @author Luke
 * @version 4.2
 * @since 1.0
 */

public class ACARSConnectionPool implements ACARSAdminInfo<ACARSMapEntry>, Serializable {

	private transient static final Logger log = Logger.getLogger(ACARSConnectionPool.class);

	// Hard-coded anonymous inactivity timeout (in ms)
	private static final long ANONYMOUS_INACTIVITY_TIMEOUT = 22500;

	// List of connections, disconnected connections and connection pool info
	private final int _maxSize;
	private transient final BlockingQueue<ACARSConnection> _disCon = new LinkedBlockingQueue<ACARSConnection>();
	private transient final Collection<ConnectionStats> _disConStats = new HashSet<ConnectionStats>();
	
	// Pools
	private final Map<Long, ACARSConnection> _cons = new HashMap<Long, ACARSConnection>();
	private final NavigableMap<String, ACARSConnection> _conLookup = new TreeMap<String, ACARSConnection>();
	
	// Pool read/write locks
	private transient final ReentrantReadWriteLock _lock = new ReentrantReadWriteLock(true);
	private transient final Lock _r = _lock.readLock();
	private transient final Lock _w = _lock.writeLock();
	
	// Inactivity timeout/last run time
	private long _inactivityTimeout = -1;
	private long _inactivityLastRun = 0;

	// The selector to use for non-blocking I/O reads
	private transient Selector _cSelector;

	/**
	 * Creates a new ACARS Connection Pool.
	 * @param maxSize the maximum size of the pool
	 */
	public ACARSConnectionPool(int maxSize) {
		super();
		_maxSize = Math.max(0, maxSize);
	}

	/**
	 * Returns all ACARS Connections.
	 * @return a Collection of ACARSConnection beans
	 */
	public Collection<ACARSConnection> getAll() {
		try {
			_r.lock();
			return new LinkedHashSet<ACARSConnection>(_cons.values());
		} finally {
			_r.unlock();
		}
	}

	/**
	 * Returns warning levels for connections.
	 * @return a Map of warning levels, keyed by Connection ID.
	 */
	public Map<Long, Integer> getWarnings() {
		Collection<ACARSConnection> cons = getAll();
		Map<Long, Integer> results = new HashMap<Long, Integer>();
		for (ACARSConnection con : cons) {
			synchronized (con) {
				if (con.getWarnings() > 0)
					results.put(Long.valueOf(con.getID()), Integer.valueOf(con.getWarnings()));
			}
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
		for (ACARSConnection ac : cons) {
			if (!ac.getUserHidden()) {
				ACARSMapEntry re = RouteEntryHelper.build(ac);
				if (re != null)
					results.add(re);
			}
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
		for (ACARSConnection ac : cons) {
			int id = ac.getFlightID();
			if (id != 0)
				results.add(Integer.valueOf(id));
		}
		
		return results;
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
				entry.setClientBuild(ac.getClientBuild());
				entry.setBeta(ac.getBeta());
				entry.setRemoteAddr(ac.getRemoteAddr());
				entry.setRemoteHost(ac.getRemoteHost());
				entry.setAddressInfo(ac.getAddressInfo());
				entry.setStatistics(ac.getTCPStatistics(), ac.getUDPStatistics());
				entry.setStartTime(new Date(ac.getStartTime()));
				entry.setUser(ac.getUser());
				entry.setUserHidden(ac.getUserHidden());
				entry.setVoice(ac.isVoiceEnabled());
				
				// Get the flight phase
				PositionMessage pm = ac.getPosition();
				entry.setFlightPhase((pm == null) ? "N/A" : pm.getPhaseName());
				
				// Get the flight information
				InfoMessage inf = ac.getFlightInfo();
				FlightInfo info = new FlightInfo(0);
				if (inf != null) {
					info.setFlightCode(inf.getFlightCode());
					info.setAirportD(inf.getAirportD());
					info.setAirportA(inf.getAirportA());
					info.setEquipmentType(inf.getEquipmentType());
					info.setAuthorID(ac.getUser().getID());
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
	 * @return a Collection of ConnectionStats beans
	 */
	public Collection<ConnectionStats> getStatistics() {
		Collection<ACARSConnection> cons = getAll();
		ArrayList<ConnectionStats> results = new ArrayList<ConnectionStats>(cons.size() + 8);
		for (ACARSConnection ac : cons)
			results.add(ac.getTCPStatistics());
		
		synchronized (_disConStats) {
			results.addAll(_disConStats);
			_disConStats.clear();
		}
		
		return results;
	}
	
	/**
	 * Adds a new connection to the pool.
	 * @param c the connection to add
	 * @throws ACARSException if the connection exists, the pool is full or a network error occurs
	 */
	public void add(ACARSConnection c) throws ACARSException {
		try {
			_w.lock();
			
			// Remove existing entries
			while (_conLookup.containsValue(c))
				_conLookup.values().remove(c);
			
			// Check size
			int size = (_maxSize == 0) ? -1 : size();
			if (size >= _maxSize)
				throw new ACARSException("Connection Pool full - " + size + " connections");
			
			// Register the SocketChannel with the selector, wake it up if it's sleeping
			_cSelector.wakeup();
			c.register(_cSelector);
			
			// Add with different keys
			_cons.put(Long.valueOf(c.getID()), c);
			_conLookup.put(c.getDataSourceAddr(), c);
			if (c.isAuthenticated() && !StringUtils.isEmpty(c.getUserID()))
				_conLookup.put(c.getUserID(), c);
			if (c.isVoiceEnabled())
				_conLookup.put(c.getVoiceSourceAddr(), c);
		} catch (ClosedChannelException cce) {
			throw new ACARSException(cce);
		} finally {
			_w.unlock();
		}
	}

	public Collection<ACARSConnection> checkConnections() {

		// Start with the list of dropped connections
		List<ACARSConnection> disCons = new ArrayList<ACARSConnection>(_disCon.size() + 4);
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
			boolean isAuth = con.isAuthenticated();
			long timeout = isAuth ? _inactivityTimeout : ANONYMOUS_INACTIVITY_TIMEOUT;
			long idleTime = now - (isAuth ? con.getLastActivity() : con.getStartTime());

			// Have we exceeded the timeout interval
			if (idleTime > timeout) {
				log.warn(con.getUserID() + " logged out after " + idleTime + "ms of inactivity");
				con.setMuted(con.isVoiceEnabled());
				con.close();
				remove(con);
				
				// Add statistics
				disCons.add(con);
				synchronized (_disConStats) {
					_disConStats.add(new ACARSConnectionStats(con.getTCPStatistics()));
				}
			}
		}
		
		return disCons;
	}
	
	/**
	 * Returns an ACARS Connection based on a connection ID.
	 * @param id the connection ID
	 * @return an ACARSConnection, or null if not found
	 */
	public ACARSConnection get(long id) {
		try {
			_r.lock();
			return _cons.get(Long.valueOf(id));
		} finally {
			_r.unlock();
		}
	}
	
	/**
	 * Returns an ACARS Connection based on a pilot ID or IP address.
	 * @param id the pilot ID or IP address
	 * @return an ACARSConnection, or null if not found
	 */
	public ACARSConnection get(String id) {
		try {
			_r.lock();
			ACARSConnection ac = _conLookup.get(id);
			if (ac != null)
				return ac;
			
			// Do a search for IP address without port
			String key = _conLookup.ceilingKey(id);
			if ((key != null) && key.startsWith(id))
				return _conLookup.get(key);
			
			return null;
		} finally {
			_r.unlock();
		}
	}
	
	/**
	 * Returns multi-player (or radar scope) connections that can see a particular update.
	 * @param loc the Location
	 * @return a List of ACARSConnection beans
	 */
	public List<ACARSConnection> getMP(GeoLocation loc) {
		if (loc == null)
			return Collections.emptyList();
		
		GeoPosition gp = new GeoPosition(loc);
		List<ACARSConnection> results = new ArrayList<ACARSConnection>();
		for (Iterator<ACARSConnection> i = getAll().iterator(); i.hasNext();) {
			ACARSConnection c = i.next();
			int dst = gp.distanceTo(c.getMPLocation());
			if ((dst >= 0) && (dst <= c.getMPRange()))
				results.add(c);
		}
		
		return results;
	}

	/**
	 * Returns the size of the connection pool.
	 */
	public int size() {
		return getAll().size();
	}
	
	/**
	 * Returns if a Dispatcher is online.
	 */
	public boolean isDispatchOnline() {
		for (Iterator<ACARSConnection> i = getAll().iterator(); i.hasNext();) {
			ACARSConnection c = i.next();
			if (c.getIsDispatch())
				return true;
		}
		
		return false;
	}

	/**
	 * Reads data from the connection pool.
	 * @return a Collection of TextEnvelope beans
	 */
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
				ACARSConnection con = null;
				try {
					SocketChannel ch = (SocketChannel) sKey.channel();
					String srcAddr = NetworkUtils.getSourceAddress(ch.getRemoteAddress());
					_r.lock();
					con = _conLookup.get(srcAddr);
				} catch (IOException ie) {
					log.error("Error fetching remote address - " + sKey.channel());
				} finally {
					_r.unlock();
				}

				if (con != null) {
					try {
						String msg = con.read();
						if (msg != null) {
							TextEnvelope env = new TextEnvelope(con.getUser(), msg, con.getID());
							env.setVersion(con.getProtocolVersion());
							results.add(env);
						}
					} catch (IOException ie) {
						con.setMuted(con.isVoiceEnabled());
						con.close();
						remove(con);
						
						// Add statistics
						_disCon.add(con);
						synchronized (_disConStats) {
							_disConStats.add(new ACARSConnectionStats(con.getTCPStatistics()));
						}
					}
				} else {
					try {
						log.warn("Cannot read from unknown source address " + sKey.channel());
						sKey.channel().close();
					} catch (IOException ie) {
						// empty
					}
				}
			}

			// Remove from the selected keys list
			i.remove();
		}

		return results;
	}

	/**
	 * Removes a connection from the pool.
	 * @param c the ACARSConnection
	 */
	public void remove(ACARSConnection c) {
		try {
			_w.lock();
			c.close();
			VoiceChannels.getInstance().remove(c.getID());
			while (_cons.containsValue(c))
				_cons.values().remove(c);
			while (_conLookup.containsValue(c))
				_conLookup.values().remove(c);
		} finally {
			_w.unlock();
		}
	}

	/**
	 * Sets the selector to use for this pool.
	 * @param cs the Selector
	 */
	public void setSelector(Selector cs) {
		_cSelector = cs;
	}

	/**
	 * Sets the inactivity timeout.
	 * @param toSeconds the timeout in seconds
	 */
	public void setTimeout(int toSeconds) {
		_inactivityTimeout = (toSeconds < 60) ? -1 : (toSeconds * 1000);
	}
}