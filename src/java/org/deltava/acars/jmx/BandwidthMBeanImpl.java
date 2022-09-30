// Copyright 2021, 2022 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.jmx;

import java.util.Date;

import org.deltava.beans.acars.Bandwidth;

/**
 * A JMX MBean implementation for ACARS bandwidth statistics. 
 * @author Luke
 * @version 10.3
 * @since 10.2
 */

public class BandwidthMBeanImpl implements BandwidthMBean {
	
	private Bandwidth _b;

	/**
	 * Creates the bean.
	 * @param b a Bandwidth bean 
	 */
	public BandwidthMBeanImpl(Bandwidth b) {
		super();
		_b = b;
	}
	
	/**
	 * Updates the bandwidth statistics.
	 * @param b a Bandwidth bean
	 */
	public void update(Bandwidth b) {
		_b = b;
	}

	@Override
	public Date getUpdateTime() {
		return new Date(_b.getDate().toEpochMilli());
	}

	@Override
	public Long getBytesIn() {
		return Long.valueOf(_b.getBytesIn());
	}

	@Override
	public Long getBytesOut() {
		return Long.valueOf(_b.getBytesOut());
	}

	@Override
	public Long getBytesSaved() {
		return Long.valueOf(_b.getBytesSaved());
	}

	@Override
	public Integer getConnections() {
		return Integer.valueOf(_b.getConnections());
	}

	@Override
	public Integer getMsgsIn() {
		return Integer.valueOf(_b.getMsgsIn());
	}

	@Override
	public Integer getMsgsOut() {
		return Integer.valueOf(_b.getMsgsOut());
	}

	@Override
	public Integer getMaxConnections() {
		return Integer.valueOf(_b.getMaxConnections());
	}

	@Override
	public Long getMaxBytes() {
		return Long.valueOf(_b.getMaxBytes());
	}

	@Override
	public Integer getMaxMsgs() {
		return Integer.valueOf(_b.getMaxMsgs());
	}
}