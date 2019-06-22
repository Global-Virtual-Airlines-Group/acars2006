// Copyright 2018, 2019 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

/**
 * An enumeration of ACARS message types.
 * @author Luke
 * @version 8.6
 * @since 8.4
 */

public enum MessageType {
	POSITION("position", "Position Report"), TEXT("text", "Text Message"), DATAREQ("datareq", "Data Request"), INFO("flight_info", "Flight Information"),
	ACK("ack", "Acknowledgement"), DIAG("diag", "Diagnostic"), AUTH("auth", "Authentication"), VIEWER("view", "Flight Viewer"), 
	DATARSP("datarsp", "Data Response"), PING("ping", "Ping"), QUIT("quit", "Disconnect"), ENDFLIGHT("end_flight", "End Flight"),
	SYSTEM("smsg", "System Message"), PIREP("pirep", "Flight Report"), ERROR("error", "Error"), DISPATCH("dispatch", "Dispatch Data"),
	TOTD("totd", "Takeoff Touchdown"), MPUPDATE("mp", "MP Update"), MPINIT("mpinit", "MP Init"), MPREMOVE("mpquit", "MP Remove"),
	SWCHAN("voxswchan", "Switch Channel"), MUTE("mute", "Mute"), VOICETOGGLE("voxtoggle", "Voice Toggle"), VOICE("vox", "Voice"),
	WARN("warn", "Warning"), WARNRESET("warnreset", "Warning Reset"), POSUPDINT("updint", "Update Interval"),
	VOICEPINGINT("voxping", "Voice Ping Interval"), COMPRESS("compress", "Data Compression"), SYSINFO("sysinfo", "System Information"),
	PERFORMANCE("performance", "Client Performance");
	
	private final String _code;
	private final String _type;
	
	MessageType(String code, String type) {
		_code = code;
		_type = type;
	}

	/**
	 * Returns the message type code.
	 * @return the code
	 */
	public String getCode() {
		return _code;
	}
	
	/**
	 * Returns the message type name.
	 * @return the name
	 */
	public String getType() {
		return _type;
	}
	
	/**
	 * Case-sensitive enum parser.
	 * @param type the type name
	 * @return a MessageType, or null if unknown
	 */
	public static MessageType fromType(String type) {
		for (MessageType mt : values()) {
			if (mt.getCode().equalsIgnoreCase(type))
				return mt;
		}
		
		return null;
	}
}