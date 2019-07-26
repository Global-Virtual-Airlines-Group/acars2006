// Copyright 2019 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

import org.deltava.beans.DatabaseBean;

/**
 * A bean to store ACARS Flight position counts.
 * @author Luke
 * @version 8.6
 * @since 8.6
 */

public class PositionCount extends DatabaseBean {
	
	private final int _cnt;

	/**
	 * Creates the bean.
	 * @param flightID the ACARS Flight ID
	 * @param positionCount the number of positions
	 */
	public PositionCount(int flightID, int positionCount) {
		super();
		setID(flightID);
		_cnt = Math.max(0, positionCount);
	}

	/**
	 * Returns the number of position records associated with this Flight.
	 * @return the number of positions
	 */
	public int getPositionCount() {
		return _cnt;
	}
}