// Copyright 2008, 2009, 2010, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message.data;

import java.time.Instant;

import org.deltava.beans.Pilot;
import org.deltava.beans.navdata.OceanicTrack;

import org.deltava.acars.message.*;

/**
 * An ACARS data response message to store Oceanic track data.
 * @author Luke
 * @version 8.4
 * @since 2.2
 */

public class OceanicTrackMessage extends DataResponseMessage<OceanicTrack> {
	
	private Instant _dt;

	/**
	 * Instantiates the Message.
	 * @param msgFrom the originating Pilot
	 * @param parentID the parent message ID
	 */
	public OceanicTrackMessage(Pilot msgFrom, long parentID) {
		super(msgFrom, DataRequest.NATS, parentID);
	}
	
	/**
	 * Returns the effective date of this track data.
	 * @return the effective date
	 */
	public Instant getDate() {
		return _dt;
	}
	
	/**
	 * Sets the effective date of these tracks. The time can be ignored.
	 * @param dt the track date
	 */
	public void setDate(Instant dt) {
		_dt = dt;
	}
}