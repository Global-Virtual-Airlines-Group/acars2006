// Copyright 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.*;
import org.deltava.beans.servinfo.Controller;

import org.deltava.util.StringUtils;

/**
 * An ACARS position report message.
 * @author Luke
 * @version 4.1
 * @since 1.0
 */

public class PositionMessage extends LocationMessage {

	// Flight phase constants
    public static final String[] FLIGHT_PHASES = {"Unknown", "Pre-Flight", "Pushback", "Taxi Out", "Takeoff", "Airborne",
    	"Landed", "Taxi In", "At Gate", "Shutdown", "Complete", "Aborted", "Error", "PIREP File"};

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
	private Controller _atc;

	// Weather information
	private int _windHeading;
	private int _windSpeed;
	private double _viz;
	private int _ceiling;
	private int _temperature;

	private int simRate = 1;

	// Flight phase
	private int phase;
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
	
	public int getAspeed() {
		return aspeed;
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
	
	public String getPhaseName() {
		return FLIGHT_PHASES[phase];
	}

	public int getRadarAltitude() {
		return this.r_altitude;
	}

	public int getSimRate() {
		return this.simRate;
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
	
	public Controller getController() { 
		return _atc;
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
		if (m <= 4.2)
			this.mach = Math.max(0, m);
	}

	public void setN1(double nn1) {
		if (Double.isNaN(nn1))
			this.n1 = 0;
		else
			this.n1 = Math.max(0, nn1);
	}

	public void setN2(double nn2) {
		if (Double.isNaN(nn2))
			this.n2 = 0;
		else
			this.n2 = Math.max(0, nn2);
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
	
	public void setCOM1(String com1) {
		_com1 = com1;
	}
	
	public void setController(Controller atc) {
		_atc = atc; 
	}
}