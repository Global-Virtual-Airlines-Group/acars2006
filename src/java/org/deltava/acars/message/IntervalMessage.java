// Copyright 2015 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

/**
 * An interface for messages that contain an update interval.
 * @author Luke
 * @version 6.2
 * @since 6.2
 */

public interface IntervalMessage extends Message {

	/**
	 * Returns the update interval.
	 * @return the interval in seconds
	 */
	public int getInterval();
}