// Copyright 2008, 2010, 2016 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import java.util.Date;

import org.deltava.beans.*;

import org.gvagroup.acars.ACARSFlags;

/**
 * An abstract message class used to support messages that store basic aircraft information. This
 * type of message is used for standard position reporting and/or multiplayer data transfer. 
 * @author Luke
 * @version 7.0
 * @since 2.2
 */

public abstract class LocationMessage extends AbstractMessage implements GeospaceLocation, ACARSFlags, Comparable<LocationMessage> {
	
	private Date _dt;
	
	private double latitude;
	private double longitude;
	private int altitude;
	private int heading;
	private int gspeed;
	private int vspeed;
	
	private int _fractionalAlt;
	
	private double pitch;
	private double bank;
	
	private int flaps;
	private int flags;
	private int lights;

	/**
	 * Initializes the Message
	 * @param type the message type
	 * @param msgFrom the originating Pilot
	 */
	protected LocationMessage(int type, Pilot msgFrom) {
		super(type, msgFrom);
		_dt = new Date();
	}
	
	public Date getDate() {
		return _dt;
	}

	@Override
	public int getAltitude() {
		return altitude;
	}
	
	/**
	 * Returns the fractional altitude.
	 * @return the altitude in feet MSL
	 */
	public double getDoubleAltitude() {
		return getAltitude() + (_fractionalAlt / 1000.0);
	}

	public int getGspeed() {
		return gspeed;
	}
	
	public int getVspeed() {
		return vspeed;
	}
	
	public int getHeading() {
		return heading;
	}

	public double getPitch() {
		return pitch;
	}

	public double getBank() {
		return bank;
	}

	public int getFlaps() {
		return flaps;
	}

	@Override
	public double getLatitude() {
		return latitude;
	}

	@Override
	public double getLongitude() {
		return longitude;
	}
	
	public int getLights() {
		return lights;
	}
	
	public int getFlags() {
		return flags;
	}

	public boolean isFlagSet(int mask) {
		return ((flags & mask) != 0);
	}
	
	public boolean isLightSet(int mask) {
		return ((lights & mask) != 0);
	}
	
	public void setDate(Date dt) {
		if (dt != null)
			_dt = dt;
	}
	
	public void setAltitude(int alt) {
		if ((alt >= -1500) && (alt <= 100000))
			altitude = alt;
	}

	public void setGspeed(int i) {
		if ((i >= -30) && (i <= 3000))
			gspeed = i;
	}
	
	public void setFlaps(int fl) {
		if ((fl >= 0) || (fl < 16))
			flaps = fl;
	}
	
	public void setHeading(int i) {
		if ((i >= 0) && (i <= 360))
			heading = i;
	}

	public void setPitch(double p) {
		if ((p >= -89) && (p <= 89))
			pitch = p;
	}
	
	public void setBank(double b) {
		if ((b >= -99) && (b <= 99))
			bank = b;
	}
	
	public void setLatitude(double l) {
		if ((l >= -90) && (l <= 90))
			latitude = l;
	}

	public void setLongitude(double l) {
		if ((l >= -180) && (l <= 180))
			longitude = l;
	}
	
	public void setFlag(int mask, boolean isSet) {
		flags = (isSet) ? (flags | mask) : (flags & (~mask));
	}

	public void setFlags(int flg) {
		flags = flg;
	}
	
	public void setLight(int mask, boolean isSet) {
		lights = (isSet) ? (lights | mask) : (lights & (~mask));
	}

	public void setLights(int l) {
		lights = l;
	}
	
	public void setVspeed(int i) {
		if ((i >= -7000) && (i <= 7000))
			vspeed = i;
	}
	
	/**
	 * Updates the fractional altitude.
	 * @param frac the fractional altitude in thousandths of feet MSL
	 */
	public void setFractionalAltitude(int frac) {
		_fractionalAlt = Math.max(0, frac);
	}
	
	@Override
	public int compareTo(LocationMessage msg2) {
		return _dt.compareTo(msg2._dt);
	}
}