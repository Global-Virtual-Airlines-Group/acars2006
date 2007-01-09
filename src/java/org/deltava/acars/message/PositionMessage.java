// Copyright 2004, 2005, 2006, 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import java.util.Date;

import org.deltava.beans.Pilot;
import org.deltava.beans.GeoLocation;
import org.deltava.beans.acars.ACARSFlags;

import org.deltava.util.StringUtils;

/**
 * An ACARS position report message.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class PositionMessage extends AbstractMessage implements GeoLocation, ACARSFlags, Comparable {

	// Flight phase constants
	public static final int PHASE_UNKNOWN = 0;
	public static final int PHASE_PREFLIGHT = 1;
	public static final int PHASE_TAXI = 2;
	public static final int PHASE_AIRBORNE = 3;
	public static final int PHASE_LANDED = 4;
	public static final int PHASE_SHUTDOWN = 5;
	public static final String[] FLIGHT_PHASES = { "???", "Preflight", "Taxi Out", "Airborne", "Landed", "Shutdown" };

	// Basic position/flight information fields
	private double latitude;
	private double longitude;
	private int altitude;
	private int r_altitude;
	private int heading;
	private double pitch;
	private double bank;
	private int aspeed;
	private int gspeed;
	private int vspeed;
	private double mach;
	private int fuelRemaining;
	private double n1;
	private double n2;
	private int _fuelFlow;
	private double _gForce;
	private double _angleOfAttack;
	private int flaps;
	private int _frameRate;

	// Wind information
	private int _windHeading;
	private int _windSpeed;

	private int flags;
	private int simRate = 1;

	// Flight phase
	private int phase;
	private boolean _noFlood;
	private boolean _isLogged;

	// Date
	private Date _dt;

	/**
	 * Creates a new Position Message.
	 * @param msgFrom the originating Pilot
	 */
	public PositionMessage(Pilot msgFrom) {
		super(Message.MSG_POSITION, msgFrom);
		_dt = new Date();
	}

	public Date getDate() {
		return _dt;
	}

	public int getAltitude() {
		return altitude;
	}

	public int getAspeed() {
		return aspeed;
	}

	public double getAngleOfAttack() {
		return _angleOfAttack;
	}

	public int getFuelRemaining() {
		return this.fuelRemaining;
	}

	public int getFrameRate() {
		return _frameRate;
	}

	public int getFlaps() {
		return flaps;
	}

	public int getGspeed() {
		return gspeed;
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

	public int getFuelFlow() {
		return _fuelFlow;
	}

	public int getWindSpeed() {
		return _windSpeed;
	}

	public int getWindHeading() {
		return _windHeading;
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public double getMach() {
		return mach;
	}

	public double getN1() {
		return this.n1;
	}

	public double getN2() {
		return this.n2;
	}

	public int getPhase() {
		return this.phase;
	}

	public int getRadarAltitude() {
		return this.r_altitude;
	}

	public int getSimRate() {
		return this.simRate;
	}

	public boolean getNoFlood() {
		return _noFlood;
	}

	public boolean isLogged() {
		return _isLogged;
	}

	public int getVspeed() {
		return vspeed;
	}

	public int getFlags() {
		return flags;
	}

	public double getG() {
		return _gForce;
	}

	public boolean isFlagSet(int mask) {
		return ((flags & mask) != 0);
	}

	public void setNoFlood(boolean noFlood) {
		_noFlood = noFlood;
	}

	public void setLogged(boolean isLogged) {
		_isLogged = isLogged;
	}

	public void setDate(Date dt) {
		if (dt != null)
			_dt = dt;
	}

	public void setAltitude(int alt) {
		if ((alt >= -1500) && (alt <= 100000))
			altitude = alt;
	}

	public void setAspeed(int i) {
		if ((i >= 0) && (i <= 700))
			aspeed = i;
	}

	public void setAngleOfAttack(double aoa) {
		if (aoa > 99.99)
			aoa = 99.99;
		else if (aoa < -99.99)
			aoa = -99.99;
			
		_angleOfAttack = aoa;
	}

	public void setFuelRemaining(int fr) {
		if (fr >= 0)
			fuelRemaining = fr;
	}

	public void setFrameRate(int rate) {
		if (rate > 0)
			_frameRate = rate;
	}

	public void setFlaps(int fl) {
		if ((fl >= 0) || (fl < 16))
			flaps = fl;
	}

	public void setGspeed(int i) {
		if ((i >= -30) && (i <= 3000))
			gspeed = i;
	}

	public void setG(double gForce) {
		_gForce = gForce;
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

	public void setFuelFlow(int flow) {
		if ((flow >= 0) && (flow < 120000))
			_fuelFlow = flow;
	}

	public void setLatitude(double l) {
		if ((l >= -90) && (l <= 90))
			latitude = l;
	}

	public void setLongitude(double l) {
		if ((l >= -180) && (l <= 180))
			longitude = l;
	}

	public void setMach(double m) {
		if ((m >= 0) && (m <= 4.2))
			this.mach = m;
	}

	public void setN1(double nn1) {
		if ((nn1 >= 0) && (nn1 <= 145))
			this.n1 = nn1;
	}

	public void setN2(double nn2) {
		if ((nn2 >= 0) && (nn2 <= 145))
			this.n2 = nn2;
	}

	public void setPhase(int newPhase) {
		if ((newPhase >= 0) && (newPhase < FLIGHT_PHASES.length))
			this.phase = newPhase;
	}

	public void setPhase(String newPhase) {
		setPhase(StringUtils.arrayIndexOf(FLIGHT_PHASES, newPhase));
	}

	public void setRadarAltitude(int alt) {
		if ((alt > 0) && (alt <= 120000))
			this.r_altitude = alt;
	}

	public void setSimRate(int newRate) {
		this.simRate = newRate;
	}

	public void setWindHeading(int i) {
		if ((i >= 0) && (i <= 360))
			_windHeading = i;
	}

	public void setWindSpeed(int spd) {
		if (spd >= 0)
			_windSpeed = spd;
	}

	public void setVspeed(int i) {
		if ((i >= -7000) && (i <= 7000))
			vspeed = i;
	}

	public void setFlag(int mask, boolean isSet) {
		flags = (isSet) ? (flags | mask) : (flags & (~mask));
	}

	public void setFlags(int flg) {
		flags = flg;
	}

	public int compareTo(Object o2) {
		PositionMessage msg2 = (PositionMessage) o2;
		return _dt.compareTo(msg2.getDate());
	}
}