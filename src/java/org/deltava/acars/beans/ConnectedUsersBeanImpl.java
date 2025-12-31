// Copyright 2024 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

import org.deltava.util.jmx.UserCountMXBean;

/**
 * A JMX bean to track ACARS connection pool size.
 * @author Luke
 * @version 12.4
 * @since 12.4
 */

public class ConnectedUsersBeanImpl implements UserCountMXBean {
	
	private final ACARSConnectionPool _pool;
	private int _maxUsers;

	/**
	 * Initializes the bean.
	 * @param pool the ACARS Connection Pool  
	 */
	public ConnectedUsersBeanImpl(ACARSConnectionPool pool) {
		super();
		_pool = pool;
	}

	@Override
	public String getCode() {
		return "ACARS";
	}

	@Override
	public Integer getUsers() {
		int size = _pool.size();
		_maxUsers = Math.max(size, _maxUsers);
		return Integer.valueOf(size);
	}

	@Override
	public Integer getMaxUsers() {
		return Integer.valueOf(_maxUsers);
	}
}