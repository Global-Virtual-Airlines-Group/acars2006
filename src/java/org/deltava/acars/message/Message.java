// Copyright 2004, 2005, 2006, 2007, 2008, 2009, 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;

/**
 * An interface to store common ACARS message data.
 * @author Luke
 * @version 4.0
 * @since 1.0
 */

public interface Message extends java.io.Serializable {

	/**
	 * System user name.
	 */
	public static final String SYSTEM_NAME = "SYSTEM";

	// Message type constants
	public static final int MSG_POSITION = 0;
	public static final int MSG_TEXT = 1;
	public static final int MSG_DATAREQ = 2;
	public static final int MSG_INFO = 3;
	public static final int MSG_ACK = 4;
	public static final int MSG_DIAG = 5;
	public static final int MSG_AUTH = 6;
	public static final int MSG_VIEWER = 7;
	public static final int MSG_DATARSP = 8;
	public static final int MSG_PING = 9;
	public static final int MSG_QUIT = 10;
	public static final int MSG_ENDFLIGHT = 11;
	public static final int MSG_SYSTEM = 12;
	public static final int MSG_PIREP = 13;
	public static final int MSG_ERROR = 14;
	public static final int MSG_DISPATCH = 15;
	public static final int MSG_TOTD = 16;
	public static final int MSG_MPUPDATE = 17;
	public static final int MSG_MPINIT = 18;
	public static final int MSG_MPREMOVE = 19;
	public static final int MSG_SWCHAN = 20;
	public static final int MSG_MUTE = 21;
	public static final int MSG_VOICETOGGLE = 22;
	public static final int MSG_VOICE = 23;
	public static final int MSG_WARN = 24;
	public static final int MSG_WARNRESET = 25;

	/**
	 * XML message codes.
	 */
	public static final String[] MSG_CODES = {"position", "text", "datareq", "flight_info", "ack", "diag", "auth", "view", "datarsp",
	      "ping", "quit", "end_flight", "smsg", "pirep", "error", "dispatch", "totd", "mp", "mpinit", "mpquit", "voxswchan", "mute",
	      "voxtoggle", "vox", "warn", "warnreset"};
	
	/**
	 * XML message type descriptions.
	 */
	public static final String[] MSG_TYPES = {"Position Report", "Text Message", "Data Request", "Flight Information",
			"Acknowledgement", "Diagnostic", "Authentication", "Raw Text", "Data Response", "Ping", "Disconnect", "End Flight",
			"System Message", "Flight Report", "Error", "Dispatch Data", "Takeoff Touchdown", "MP Update", "MP Init", "MP Remove",
			"Switch Channel", "Mute", "Voice Toggle", "Voice", "Warning", "Warning Reset"};

	/**
	 * Returns the message type.
	 * @return the type code
	 */
	public int getType();
	
	/**
	 * Returns if the message can be sent by an unauthenticated user.
	 * @return TRUE if can be sent by an unauthenticated user, otherwise FALSE
	 */
	public boolean isAnonymous();
	
	public void setTime(long ts);
	public long getTime();
	
	/**
	 * Returns the message protocol version.
	 * @return the protocol version
	 */
	public int getProtocolVersion();

	/**
	 * Updates the message ID.
	 * @param id the ID
	 */
	public void setID(long id);
	
	/**
	 * Returns the message ID.
	 * @return the ID
	 */
	public long getID();
	
	/**
	 * Updates the message sender.
	 * @param msgFrom the sending Pilot
	 */
	public void setSender(Pilot msgFrom);
	
	/**
	 * Returns the message sender.
	 * @return the sending Pilot, or null if unauthenticated
	 */
	public Pilot getSender();
	
	/**
	 * Returns the message sender's Pilot ID.
	 * @return the Pilot ID, or null if unathenticated
	 */
	public String getSenderID();
}