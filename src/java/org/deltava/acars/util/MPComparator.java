// Copyright 2008, 2010, 2016 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.util;

import java.io.Serializable;
import java.util.Comparator;

import org.deltava.beans.GeoLocation;
import org.deltava.beans.schedule.GeoPosition;

import org.deltava.acars.beans.ACARSConnection;

/**
 * A Comparator for ACARS Connections comparing their position to a common location.
 * @author Luke
 * @version 7.0
 * @since 2.2
 */

public class MPComparator implements Comparator<ACARSConnection>, Serializable {

	private GeoPosition _pos;

	/**
	 * Initializes the comparator.
	 * @param loc the Location to compare against
	 */
	public MPComparator(GeoLocation loc) {
		super();
		_pos = new GeoPosition(loc);
	}

	@Override
	public int compare(ACARSConnection ac1, ACARSConnection ac2) {
		int d1 = _pos.distanceTo(ac1.getPosition());
		int d2 = _pos.distanceTo(ac2.getPosition());
		return Integer.valueOf(d1).compareTo(Integer.valueOf(d2));
	}
}