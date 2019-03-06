// Copyright 2018, 2019 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

/**
 * An enumeration to store ACARS data request types.
 * @author Luke
 * @version 8.6
 * @since 8.4
 */

public enum DataRequest {
	UNKNOWN("?"), USERLIST("pilots"), ONLINE("isonline"), VALIDATE("validate"), ADDUSER("addpilots"), REMOVEUSER("delpilots"), TRINFO("sidstar"),
	NAVAIDINFO("navaid"), PVTVOX("pvtvox"), EQLIST("eqList"), APLIST("apList"), ALLIST("aList"), CHARTS("charts"), ATCINFO("atc"), BUSY("busy"),
	DRAFTPIREP("draftpirep"), TS2SERVERS("ts2servers"), SCHED("sched"), NATS("nat"), HIDE("hide"), LIVERIES("liveries"), WX("wx"), APINFO("airportinfo"),
	APPINFO("appInfo"), CHLIST("vchannels"), LOAD("load"), LASTAP("lastairport"), ALT("alternate"), IATA("iataCodes"), FLIGHTNUM("flightnum"), 
	FIR("fir"), RUNWAYS("runways"), GATES("gates"), RWYINFO("runwayinfo");
	
	private final String _type;
	
	/**
	 * Creates a data request.
	 * @param type the request type
	 */
	DataRequest(String type) {
		_type = type;
	}
	
	/**
	 * Returns the data request type.
	 * @return the type
	 */
	public String getType() {
		return _type;
	}
	
	/**
	 * Exception-safe enumeration parser.
	 * @param type a request type
	 * @return a DataRequest, or UNKNOWN
	 */
	public static DataRequest fromType(String type) {
		for (DataRequest req : values()) {
			if (req.getType().equalsIgnoreCase(type))
				return req;
		}
		
		return UNKNOWN;
	}
}