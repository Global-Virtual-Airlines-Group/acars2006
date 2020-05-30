// Copyright 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2014, 2016, 2017, 2019, 2020 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.stream.Collectors;
import java.nio.channels.*;
import java.time.Instant;

import org.apache.log4j.Logger;

import org.deltava.acars.ACARSException;

import org.deltava.beans.acars.*;
import org.deltava.beans.GeoLocation;

import org.deltava.acars.message.*;

import org.deltava.util.*;

import org.deltava.acars.util.RouteEntryHelper;
import org.gvagroup.acars.ACARSAdminInfo;

/**
 * A Connection Pool for ACARS Connections.
 * @author Luke
 * @version 9.0
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

	// The selector to use for non-blocking I/O reads and counters for select operations
	private transient Selector _cSelector;
	private int _selectCount;
	private int _maxSelects = Integer.MAX_VALUE;

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
				if (con.getWarningScore() > 0)
					results.put(Long.valueOf(con.getID()), Integer.valueOf(con.getWarningScore()));
			}
		}
		
		return results;
	}
	
	/**
	 * Returns network data in a serialized form for transfer between virtual machines or class loaders.
	 * @return a Collection of byte arrays
	 */
	@Override
	public Collection<byte[]> getSerializedInfo() {
		Collection<ACARSConnection> cons = getAll();
		Collection<ACARSMapEntry> results = new ArrayList<ACARSMapEntry>(cons.size() + 2);
		cons.stream().filter(ac -> ac.getIsDispatch() || !ac.getUserHidden()).map(ac -> RouteEntryHelper.build(ac)).filter(Objects::nonNull).forEach(results::add);
		return IPCUtils.serialize(results);
	}
	
	/**
	 * Returns the flight IDs of all active flights.
	 * @return a Collection of Integer flight IDs
	 */
	@Override
	public Collection<Integer> getFlightIDs() {
		Collection<ACARSConnection> cons = getAll();
		return cons.stream().filter(ac -> (ac.getFlightID() != 0)).map(ACARSConnection::getFlightID).collect(Collectors.toCollection(TreeSet::new));
	}

	/**
	 * Returns Connection Pool data to a web application.
	 * @param showHidden TRUE if stealth connections should be displayed, otherwise FALSE
	 * @return a Collection of serialized ConnectionEntry beans
	 */
	@Override
	public Collection<byte[]> getPoolInfo(boolean showHidden) {
		Collection<ACARSConnection> cons = getAll();
		Collection<ConnectionEntry> results = new ArrayList<ConnectionEntry>(cons.size() + 2);
		for (ACARSConnection ac : cons) {
			if (!showHidden && ac.getUserHidden()) continue;
			ConnectionEntry entry = ac.getIsDispatch() ? new DispatchConnectionEntry(ac.getID()) : new ConnectionEntry(ac.getID());
			entry.setClientBuild(ac.getClientBuild());
			entry.setBeta(ac.getBeta());
			entry.setRemoteAddr(ac.getRemoteAddr());
			entry.setRemoteHost(ac.getRemoteHost());
			entry.setAddressInfo(ac.getAddressInfo());
			entry.setStatistics(ac.getTCPStatistics(), ac.getUDPStatistics());
			entry.setStartTime(Instant.ofEpochMilli(ac.getStartTime()));
			entry.setUser(ac.getUser());
			entry.setUserHidden(ac.getUserHidden());
			entry.setVoice(ac.isVoiceEnabled());
			entry.setCompressed(ac.getCompression() != Compression.NONE);
				
			// Get the flight information
			InfoMessage inf = ac.getFlightInfo();
			FlightInfo info = new FlightInfo(0);
			if (inf != null) {
				info.setFlightCode(inf.getFlightCode());
				info.setAirportD(inf.getAirportD());
				info.setAirportA(inf.getAirportA());
				info.setStartTime(inf.getStartTime());
				info.setEndTime(info.getEndTime());
				info.setEquipmentType(inf.getEquipmentType());
				info.setAuthorID(ac.getUser().getID());
				info.setSimulator(inf.getSimulator());
				if (inf.getFlightID() != 0)
					info.setID(inf.getFlightID());
			}
				
			// Get the flight phase
			PositionMessage pm = ac.getPosition();
			if (pm == null)
				entry.setFlightPhase((inf == null) ? "N/A" : "Unknown");
			else
				entry.setFlightPhase(pm.getPhase().getName());
				
			// Save the flight info
			entry.setFlightInfo(info);
			results.add(entry);
		}
		
		return IPCUtils.serialize(results); 
	}
	
	/**
	 * Returns ACARS connection statistics.
	 * @return a Collection of ConnectionStats beans
	 */
	@Override
	public Collection<ConnectionStats> getStatistics() {
		Collection<ACARSConnection> cons = getAll();
		ArrayList<ConnectionStats> results = new ArrayList<ConnectionStats>(cons.size() + 8);
		cons.stream().map(ACARSConnection::getTCPStatistics).forEach(results::add);
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

	/**
	 * Checks to see whether any connections should be closed for inactivity.
	 * @return a Collection of ACARSConnections
	 */
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
		
		List<ACARSConnection> results = new ArrayList<ACARSConnection>();
		for (ACARSConnection c : getAll()) {
			int dst = loc.distanceTo(c.getMPLocation());
			if ((dst >= 0) && (dst <= c.getMPRange()))
				results.add(c);
		}
		
		return results;
	}

	@Override
	public int size() {
		return getAll().size();
	}
	
	@Override
	public boolean isDispatchOnline() {
		return getAll().stream().anyMatch(ac -> ac.getIsDispatch());
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
		Collection<TextEnvelope> results = new ArrayList<TextEnvelope>(keys.size() + 1);
		for (Iterator<SelectionKey> i = keys.iterator(); i.hasNext();) {
			SelectionKey sKey = i.next();

			// If the selection key is ready for reading, get the Connection and read
			if (sKey.isValid() && sKey.isReadable()) {
				ACARSConnection con = null;
				try {
					_r.lock();
					SocketChannel ch = (SocketChannel) sKey.channel();
					String srcAddr = NetworkUtils.getSourceAddress(ch.getRemoteAddress());
					con = _conLookup.get(srcAddr);
				} catch (IOException ie) {
					log.error("Error fetching remote address - " + sKey.channel());
				} finally {
					_r.unlock();
				}

				if (con != null) {
					try {
						String msg = con.read();
						
						// This may have multiple XML messages in it - the message parser will split them
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
	 * @param c the ACARSConnection to remove
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
	 * Recycles the read selector.
	 * @throws IOException if an I/O error occurs
	 */
	public void updateSelector() throws IOException {
		if (_cSelector != null)
			log.info("Updating Read Selector after " + _selectCount + " selects");
		
		Selector s = null;
		try {
			_w.lock();
			s = Selector.open();
			for (ACARSConnection ac : _cons.values())
				ac.register(s);
			
			if (_cSelector != null)
				_cSelector.close();
			_cSelector = s;
			_selectCount = 0;
		} catch (Exception e) {
			if (s != null) s.close();
		} finally {
			_w.unlock();
		}
	}
	
	/**
	 * Waits for data to be available for reading on any connection.
	 * @param timeout the timeout in milliseconds
	 * @return the number of connections with data ready to be read 
	 * @throws IOException if an I/O error occurs
	 */
	public int select(long timeout) throws IOException {
		if (_selectCount > _maxSelects)
			updateSelector();
		
		_selectCount++;
		return _cSelector.select(timeout);
	}
	
	/**
	 * Returns the number of select operations that have been performed on the current Selector.
	 * @return the number of selects
	 */
	@Override
	public int getSelectCount() {
		return _selectCount;
	}

	/**
	 * Sets the inactivity timeout.
	 * @param toSeconds the timeout in seconds
	 */
	public void setTimeout(int toSeconds) {
		_inactivityTimeout = (toSeconds < 60) ? -1 : (toSeconds * 1000);
	}
	
	/**
	 * Sets the maximum number of select operations to be performed on the read Selector before
	 * it is recycled and replaced with a new one. This is to get around a bug in the JVM's epoll()
	 * implementation. 
	 * @param maxSelects the maxmimum number of selects, or <= 0 for infinite
	 */
	public void setMaxSelects(int maxSelects) {
		_maxSelects = (maxSelects < 1) ? Integer.MAX_VALUE : maxSelects;
	}
}