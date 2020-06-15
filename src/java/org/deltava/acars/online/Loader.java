// Copyright 2020 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.online;

import java.time.Instant;

import org.deltava.beans.OnlineNetwork;
import org.deltava.beans.servinfo.NetworkInfo;

import org.deltava.util.cache.*;

/**
 * An abstract class for common Online Network status loader tasks. 
 * @author Luke
 * @version 9.0
 * @since 9.0
 */

public abstract class Loader implements Runnable {
	
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
	
	public Instant getLastUpdate() {
		return _lastUpdate;
	}
	
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
	 * Initializes the Loader with last run/update dates.
	 * @param lastRun the last execution date/time
	 * @param lastUpdate the last data update date/time
	 */
	public void init(Instant lastRun, Instant lastUpdate) {
		_lastRun = lastRun;
		_lastUpdate = lastUpdate;
	}
	
	@Override
	public void run() {
		_lastRun = Instant.now();
		_isUpdated = false;
	}
}