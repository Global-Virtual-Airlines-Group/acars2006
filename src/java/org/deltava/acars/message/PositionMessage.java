// Copyright 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2014, 2016 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import java.time.Instant;

import org.deltava.beans.*;
import org.deltava.beans.servinfo.Controller;

import org.deltava.util.StringUtils;

/**
 * An ACARS position report message.
 * @author Luke
 * @version 7.0
 * @since 1.0
 */

public class PositionMessage extends LocationMessage {

	// Flight phase constants
    public static final String[] FLIGHT_PHASES = {"Unknown", "Pre-Flight", "Pushback", "Taxi Out", "Takeoff", "Airborne", "Landed", "Taxi In", "At Gate", "Shutdown", "Complete", "Aborted", "Error", "PIREP File"};

	private int r_altitude;
	private int aspeed;
	private double mach;
	private int fuelRemaining;
	private double n1;
	private double n2;
	private int _fuelFlow;
	private double _gForce;
	private double _angleOfAttack;
	private int _frameRate;
	
	private int _txCode;
	private boolean _txActive;
	
	private String _com1;
	private String _com2;
	private String _nav1;
	private String _nav2;
	
	private Controller _atc1;
	private Controller _atc2;

	// Weather information
	private int _windHeading;
	private int _windSpeed;
	private double _viz;
	private int _ceiling;
	private int _temperature;
	private int _pressure;

	private int _simRate = 1;
	private Instant _simTime = getDate();

	// Flight phase
	private int _phase;
	private boolean _isReplay;
	private boolean _isLogged;

	/**
	 * Creates a new Position Message.
	 * @param msgFrom the originating Pilot
	 */
	public PositionMessage(Pilot msgFrom) {
		super(Message.MSG_POSITION, msgFrom);
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

	public int getFuelFlow() {
		return _fuelFlow;
	}

	public int getWindSpeed() {
		return _windSpeed;
	}

	public int getWindHeading() {
		return _windHeading;
	}
	
	public double getVisibility() {
		return _viz;
	}
	
	public int getCeiling() {
		return _ceiling;
	}
	
	public int getTemperature() {
		return _temperature;
	}
	
	public int getPressure() {
		return _pressure;
	}
	
	public int getAspeed() {
		return aspeed;
	}

	public double getMach() {
		return mach;
	}

	public double getN1() {
		return n1;
	}

	public double getN2() {
		return n2;
	}

	public int getPhase() {
		return _phase;
	}
	
	public String getPhaseName() {
		return FLIGHT_PHASES[_phase];
	}

	public int getRadarAltitude() {
		return this.r_altitude;
	}

	public int getSimRate() {
		return _simRate;
	}
	
	public Instant getSimTime() {
		return _simTime;
	}

	public boolean isReplay() {
		return _isReplay;
	}

	public boolean isLogged() {
		return _isLogged;
	}

	public double getG() {
		return _gForce;
	}
	
	public boolean getTXActive() {
		return _txActive;
	}
	
	public int getTXCode() {
		return _txCode;
	}
	
	public String getCOM1() {
		return _com1;
	}
	
	public String getCOM2() {
		return _com2;
	}
	
	public String getNAV1() {
		return _nav1;
	}
	
	public String getNAV2() {
		return _nav2;
	}
	
	public Controller getATC1() { 
		return _atc1;
	}
	
	public Controller getATC2() { 
		return _atc2;
	}
	
	public boolean hasATC() {
		return (_atc1 != null) || (_atc2 != null);
	}

	public void setReplay(boolean isReplay) {
		_isReplay = isReplay;
	}

	public void setLogged(boolean isLogged) {
		_isLogged = isLogged;
	}

	public void setAngleOfAttack(double aoa) {
		if (!Double.isNaN(aoa))
			_angleOfAttack = Math.max(-90, Math.min(90, aoa));
	}

	public void setFuelRemaining(int fr) {
		fuelRemaining = Math.max(0, fr);
	}

	public void setFrameRate(int rate) {
		_frameRate = Math.max(0, rate);
	}

	public void setG(double gForce) {
		if (!Double.isNaN(gForce))
			_gForce = gForce;
	}

	public void setFuelFlow(int flow) {
		_fuelFlow = Math.max(0, flow);
	}
	
	public void setAspeed(int i) {
		if ((i >= 0) && (i <= 700))
			aspeed = i;
	}

	public void setMach(double m) {
		if (!Double.isNaN(m))
			mach = Math.min(6.5, Math.max(0, m));
	}

	public void setN1(double nn1) {
		if (Double.isNaN(nn1))
			this.n1 = 0;
		else
			this.n1 = Math.min(9999, Math.max(0, nn1));
	}

	public void setN2(double nn2) {
		if (Double.isNaN(nn2))
			this.n2 = 0;
		else
			this.n2 = Math.min(9999, Math.max(0, nn2));
	}

	public void setPhase(String newPhase) {
		_phase = StringUtils.arrayIndexOf(FLIGHT_PHASES, newPhase, 0);
	}

	public void setRadarAltitude(int alt) {
		if ((alt > 0) && (alt <= 125000))
			this.r_altitude = alt;
	}

	public void setSimRate(int newRate) {
		_simRate = Math.max(1, newRate);
	}
	
	public void setSimTime(Instant i) {
		_simTime = i;
	}

	public void setWindHeading(int i) {
		if ((i >= 0) && (i <= 360))
			_windHeading = i;
	}
	
	public void setTXActive(boolean isActive) {
		_txActive = isActive;
	}
	
	public void setTXCode(int txCode) {
		_txCode = Math.max(0, Math.min(9900, txCode));
	}

	public void setWindSpeed(int spd) {
		_windSpeed = Math.max(0, spd);
	}
	
	public void setVisibility(double viz) {
		_viz = Math.max(0, Math.min(9999, viz));
	}
	
	public void setCeiling(int ft) {
		_ceiling = Math.min(35000, Math.max(-300, ft));
	}
	
	public void setTemperature(int t) {
		_temperature = Math.max(-100, Math.min(99, t));
	}
	
	public void setPressure(int p) {
		_pressure = Math.max(0, p);
	}
	
	public void setCOM1(String freq) {
		_com1 = freq;
	}
	
	public void setCOM2(String freq) {
		_com2 = freq;
	}
	
	public void setNAV1(String freq) {
		_nav1 = freq;
	}
	
	public void setNAV2(String freq) {
		_nav2 = freq;
	}
	
	public void setATC1(Controller atc) {
		_atc1 = atc; 
	}
	
	public void setATC2(Controller atc) {
		_atc2 = atc; 
	}
}