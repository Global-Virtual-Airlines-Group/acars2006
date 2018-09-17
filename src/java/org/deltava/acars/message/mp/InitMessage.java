// Copyright 2008, 2009, 2010, 2011, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.mp;

import org.deltava.acars.message.*;

import org.deltava.beans.*;

/**
 * An ACARS multi-player position initialization message.
 * @author Luke
 * @version 8.4
 * @since 2.2
 */

public class InitMessage extends AbstractMessage {
	
	private GeoLocation _ctr;
	private int _range;
	private String _livery;
	
	/**
	 * Initializes the Message.
	 * @param msgFrom the originating Pilot
	 */
	public InitMessage(Pilot msgFrom) {
		super(MessageType.MPINIT, msgFrom);
		setProtocolVersion(2);
	}
	
	/**
	 * Returns the aircraft's current location.
	 * @return a GeoLocation
	 */
	public GeoLocation getLocation() {
		return _ctr;
	}
	
	/**
	 * Returns the multi-player range.
	 * @return the range in miles
	 */
	public int getRange() {
		return _range;
	}
	
	/**
	 * Returns the livery code to use.
	 * @return the livery code
	 */
	public String getLivery() {
		return _livery;
	}
	
	/**
	 * Sets the initiali multi-player range.
	 * @param range the range in miles
	 */
	public void setRange(int range) {
		_range = Math.max(0, range);
	}
	
	/**
	 * Sets the initial aircraft location.
	 * @param loc
	 */
	public void setLocation(GeoLocation loc) {
		_ctr = loc;
	}
	
	/**
	 * Updates the livery code.
	 * @param liveryCode the livery code
	 */
	public void setLivery(String liveryCode) {
		_livery = liveryCode;
	}
}