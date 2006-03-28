// Copyright 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.util;

import java.io.IOException;
import java.net.*;

import org.apache.log4j.Logger;

import org.deltava.beans.servinfo.*;

import org.deltava.dao.DAOException;
import org.deltava.dao.file.GetServInfo;

import org.deltava.util.cache.Cache;
import org.deltava.util.http.HttpTimeoutHandler;

/**
 * A worker thread to asynchronously load ServInfo data.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class ServInfoLoader implements Runnable {
	
	private static final Logger log = Logger.getLogger(ServInfoLoader.class);

	private String _url;
	private String _network;

	private Cache _statusCache;
	private Cache _infoCache;
	
	private NetworkInfo _info;
	
	/**
	 * Initializes the worker.
	 * @param url the network status URL
	 * @param networkName the network name 
	 */
	public ServInfoLoader(String url, String networkName) {
		super();
		_url = url;
		_network = networkName;
	}
	
	/**
	 * Sets the data caches that the results will be stored in.
	 * @param statusCache the network status cache
	 * @param infoCache the network client cache
	 */
	public void setCaches(Cache statusCache, Cache infoCache) {
		_statusCache = statusCache;
		_infoCache = infoCache;
	}
	
	/**
	 * Helper method to open a connection to a particular URL.
	 */
	private HttpURLConnection getURL(String dataURL) {
		try {
			URL url = new URL(null, dataURL, new HttpTimeoutHandler(1750));
			return (HttpURLConnection) url.openConnection();
		} catch (IOException ie) {
			log.error("Error getting HTTP connection " + ie.getMessage(), ie);
			return null;
		}
	}
	
	/**
	 * Returns the retrieved network traffic information. 
	 * @return the NetworkInfo bean
	 */
	public NetworkInfo getInfo() {
		return _info;
	}

	/**
	 * Starts the thread.
	 * @see Runnable#run()
	 */
	public void run() {

		// Get the URL connection
		HttpURLConnection con = getURL(_url);
		if (con == null)
			return;

		// Get the network URLs
		NetworkStatus status = null;
		try {
			GetServInfo sdao = new GetServInfo(con);
			sdao.setUseCache(false);
			status = sdao.getStatus(_network);
			_statusCache.add(status);
		} catch (DAOException de) {
			log.error("Error loading " + _network.toUpperCase() + " status - " + de.getMessage(), de);
		} finally {
			con.disconnect();
		}
		
		// If we received nothing, abort
		if (status == null)
			return;

		// Get network status
		con = getURL(status.getDataURL());
		if (con == null)
			return;
		
		// Get the network info
		try {
			GetServInfo idao = new GetServInfo(con);
			idao.setUseCache(false);
			idao.setBufferSize(40960);
			_info = idao.getInfo(_network);
			_infoCache.add(_info);
		} catch (DAOException de) {
			Throwable re = de.getCause();
			if (re instanceof SocketTimeoutException) {
				log.warn("HTTP Timeout connecting to " + con.getURL().toString());
			} else {
				log.error("Error loading " + _network.toUpperCase() + " info - " + de.getMessage(), de);
			}
		} finally {
			con.disconnect();
		}
	}
}