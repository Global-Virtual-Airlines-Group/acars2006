// Copyright 2021, 2022 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.jmx;

import java.time.Instant;

import javax.management.MXBean;

/**
 * A JMX MBean for ACARS bandwidth statistics. 
 * @author Luke
 * @version 10.3
 * @since 10.2
 */

@MXBean
public interface BandwidthMBean {
	
	/**
	 * The last update time.
	 * @return the update date/time
	 */
	public Instant getUpdateTime();

	/**
	 * Returns the inbound bandwidth during the time period.
	 * @return the inbound number of bytes
	 */
	Long getBytesIn();
	
	/**
	 * Returns the outbound bandwidth during the time period.
	 * @return the outbound number of bytes
	 */
	Long getBytesOut();
	
	/**
	 * Returns the bandwidth saved via compression during the time period.
	 * @return the reduced number of bytes
	 */
	Long getBytesSaved();
	
	/**
	 * Returns the number of ACARS connections.
	 * @return the number of connections
	 */
	Integer getConnections();

	/**
	 * Returns the number of inbound ACARS protocol messages during the time period.
	 * @return the inbound number of messages
	 */
	Integer getMsgsIn();
	
	/**
	 * Returns the number of outbound ACARS protocol messages during the time period.
	 * @return the outbound number of messages
	 */
	Integer getMsgsOut();
	
	/**
	 * Returns the maximum number of ACARS connections during the time period.
	 * @return the maximum number of connections
	 */
	Integer getMaxConnections();

	/**
	 * Returns the maximum amount of bandwidth during the time period.
	 * @return the maximum number of bytes
	 */
	Long getMaxBytes();

	/**
	 * Returns the maximum number of outbound bandwidth during the time period.
	 * @return the maximum outbound number of bytes
	 */
	Integer getMaxMsgs();
}