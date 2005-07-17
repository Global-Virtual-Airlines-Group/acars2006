package org.deltava.acars.message;

import org.deltava.beans.Pilot;
import org.deltava.beans.GeoLocation;
import org.deltava.beans.acars.ACARSFlags;

import org.deltava.util.StringUtils;

/**
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class PositionMessage extends AbstractMessage implements GeoLocation, ACARSFlags {
	
	// Flight phase constants
	public static final int PHASE_UNKNOWN = 0;
	public static final int PHASE_PREFLIGHT = 1;
	public static final int PHASE_TAXI = 2;
	public static final int PHASE_AIRBORNE = 3;
	public static final int PHASE_LANDED = 4;
	public static final int PHASE_SHUTDOWN = 5;
	public static final String[] FLIGHT_PHASES = {"???", "Preflight", "Taxi Out", "Airborne", "Landed", "Shutdown"};

	// Basic position/flight information fields
	private double latitude;
	private double longitude;
	private int altitude;
	private int r_altitude;
	private int heading;
	private int aspeed;
	private int gspeed;
	private int vspeed;
	private double mach;
	private int fuelRemaining;
	private double n1;
	private double n2;
	
	private int flaps;
	private int flags;
	private int simRate = 1;

	// Flight phase
	private int phase;
	
	/**
	 * Creates a new Position Message.
	 * @param msgFrom the originating Pilot
	 */
	public PositionMessage(Pilot msgFrom) {
		super(Message.MSG_POSITION, msgFrom);
	}
	
	public int getAltitude() {
		return this.altitude;
	}
	
	public int getAspeed() {
		return aspeed;
	}
	
	public int getFuelRemaining() {
		return this.fuelRemaining;
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

	public int getVspeed() {
		return vspeed;
	}
	
	public int getFlags() {
	   return flags;
	}
	
	public boolean isFlagSet(int mask) {
	   return ((flags & mask) != 0);
	}
	
	public void setAltitude(int alt) {
		if ((alt >= -1500) && (alt <= 100000))
			this.altitude = alt;
	}
	
	public void setAspeed(int i) {
		if ((i >= 0) && (i <= 700))
			this.aspeed = i;
	}
	
	public void setFuelRemaining(int fr) {
		if (fr >= 0)
			this.fuelRemaining = fr;
	}
	
	public void setFlaps(int fl) {
	   if ((fl >= 0) || (fl < 16))
	      flaps = fl;
	}

	public void setGspeed(int i) {
		if ((i >= -30) && (i <= 3000))
			gspeed = i;
	}

	public void setHeading(int i) {
		if ((i >= 0) && (i <= 360))
			heading = i;
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
		if ((nn1 >= 0) && (nn1 <= 150))
			this.n1 = nn1;
	}
	
	public void setN2(double nn2) {
		if ((nn2 >= 0) && (nn2 <= 150))
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
		if ((alt >= -50) && (alt <= 120000))
			this.r_altitude = alt;
	}
	
	public void setSimRate(int newRate) {
		this.simRate = newRate;
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
}