// Copyright 2018, 2019, 2020, 2022 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

/**
 * An enumeration to store ACARS data request types.
 * @author Luke
 * @version 10.3
 * @since 8.4
 */

public enum DataRequest implements SubRequest {
	UNKNOWN("?"), USERLIST("pilots"), ONLINE("isonline"), VALIDATE("validate"), ADDUSER("addpilots"), REMOVEUSER("delpilots"), TRINFO("sidstar"),
	NAVAIDINFO("navaid"), PVTVOX("pvtvox"), EQLIST("eqList"), APLIST("apList"), ALLIST("aList"), CHARTS("charts"), ATCINFO("atc"), BUSY("busy"),
	DRAFTPIREP("draftpirep"), TS2SERVERS("ts2servers"), SCHED("sched"), NATS("nat"), HIDE("hide"), LIVERIES("liveries"), WX("wx"), APINFO("airportinfo"),
	APPINFO("appInfo"), CHLIST("vchannels"), LOAD("load"), LASTAP("lastairport"), ALT("alternate"), IATA("iataCodes"), FLIGHTNUM("flightnum"), 
	FIR("fir"), RUNWAYS("runways"), GATES("gates"), RWYINFO("runwayinfo"), TAXITIME("taxitime"), SBDATA("simBrief"), ATIS("atis");
	
	private final String _code;
	
	/**
	 * Creates a data request.
	 * @param code the request code
	 */
	DataRequest(String code) {
		_code = code;
	}
	
	@Override
	public String getCode() {
		return _code;
	}
	
	@Override
	public final SubRequestType getType() {
		return SubRequestType.DATA;
	}
	
	/**
	 * Exception-safe enumeration parser.
	 * @param type a request type
	 * @return a DataRequest, or UNKNOWN
	 */
	public static DataRequest fromType(String type) {
		for (DataRequest req : values()) {
			if (req._code.equalsIgnoreCase(type))
				return req;
		}
		
		return UNKNOWN;
	}
}