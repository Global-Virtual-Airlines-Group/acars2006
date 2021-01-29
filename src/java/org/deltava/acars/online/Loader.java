// Copyright 2020, 2021 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.online;

import java.time.Instant;
import java.io.IOException;

import org.apache.log4j.Logger;

import org.deltava.beans.OnlineNetwork;
import org.deltava.beans.servinfo.NetworkInfo;

import org.deltava.dao.DAOException;
import org.deltava.dao.http.HTTPDAOException;

import org.deltava.util.cache.*;

/**
 * An abstract class for common Online Network status loader tasks. 
 * @author Luke
 * @version 9.1
 * @since 9.0
 */

public abstract class Loader implements Runnable {
	
	private final Logger log = Logger.getLogger(getClass());
	
	private Instant _lastUpdate = Instant.ofEpochMilli(0);
	private Instant _lastRun = Instant.ofEpochMilli(0);
	
	protected final OnlineNetwork _network;
	private final Cache<NetworkInfo> _cache = CacheManager.get(NetworkInfo.class, "ServInfoData");
	
	private final int _updateInterval;
	private boolean _isUpdated;

	/**
	 * Creates the loader.
	 * @param net the OnlineNetwork
	 * @param updateInterval the update interval in seconds
	 */
	protected Loader(OnlineNetwork net, int updateInterval) {
		super();
		_network = net;
		_updateInterval = Math.max(5, updateInterval);
	}

	/**
	 * Updates the network data.
	 * @param inf a NetworkInfo bean
	 */
	protected void update(NetworkInfo inf) {
		_lastUpdate = inf.getValidDate();
		_cache.add(inf);
		_isUpdated = true;
	}
	
	/**
	 * Returns the Online Network.
	 * @return the OnlineNetwork
	 */
	public OnlineNetwork getNetwork() {
		return _network;
	}
	
	/**
	 * Returns the last time the network data was successfully updated.
	 * @return the last update date/time
	 */
	public Instant getLastUpdate() {
		return _lastUpdate;
	}
	
	/**
	 * Returns the last time the load was executed.
	 * @return the last execution date/time
	 */
	public Instant getLastRun() {
		return _lastRun;
	}
	
	public boolean isUpdated() {
		return _isUpdated;
	}
	
	public boolean isEligible() {
		return Instant.now().isAfter(_lastRun.plusSeconds(_updateInterval));
	}
	
	/**
	 * Downloads the data. Subclasses need to extend this method.
	 */
	protected abstract void execute() throws IOException, DAOException;
	
	/**
	 * Initializes the Loader with last run/update dates.
	 * @param lastRun the last execution date/time
	 * @param lastUpdate the last data update date/time
	 */
	public void init(Instant lastRun, Instant lastUpdate) {
		_lastRun = lastRun;
		_lastUpdate = lastUpdate;
	}
	
	@Override
	public final void run() {
		_lastRun = Instant.now();
		_isUpdated = false;
		try {
			execute();
		} catch (HTTPDAOException hde) {
			log.error("Error " + hde.getStatusCode() + " loading " + hde.getMessage());
		} catch (Exception e) {
			boolean isTimeout = e.getCause() instanceof java.net.SocketTimeoutException;
			if (isTimeout)
				log.warn("Timeout loading " + _network + " data");
			else
				log.error(e.getMessage(), e);
		}
	}
}