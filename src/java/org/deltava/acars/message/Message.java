// Copyright 2004, 2005, 2006, 2007, 2008 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;

/**
 * An interface to store common ACARS message data.
 * @author Luke
 * @version 2.2
 * @since 1.0
 */

public interface Message extends java.io.Serializable {

	// Maximum protocol version
	public static final int PROTOCOL_VERSION = 1;

	// System name and welcome message constants
	public static final String SYSTEM_NAME = "SYSTEM";

	// Message type constants
	public static final int MSG_POSITION = 0;
	public static final int MSG_TEXT = 1;
	public static final int MSG_DATAREQ = 2;
	public static final int MSG_INFO = 3;
	public static final int MSG_ACK = 4;
	public static final int MSG_DIAG = 5;
	public static final int MSG_AUTH = 6;
	//public static final int MSG_RAW = 7;
	public static final int MSG_DATARSP = 8;
	public static final int MSG_PING = 9;
	public static final int MSG_QUIT = 10;
	public static final int MSG_ENDFLIGHT = 11;
	public static final int MSG_SYSTEM = 12;
	public static final int MSG_PIREP = 13;
	public static final int MSG_ERROR = 14;
	public static final int MSG_DISPATCH = 15;
	public static final int MSG_MPUPDATE = 16;
	public static final int MSG_MPINIT = 17;

	/**
	 * XML message codes.
	 */
	public static final String[] MSG_CODES = {"position", "text", "datareq", "flight_info", "ack", "diag", "auth", "raw", "datarsp",
	      "ping", "quit", "end_flight", "smsg", "pirep", "error", "dispatch", "mp", "mpinit"};
	
	/**
	 * XML message type descriptions.
	 */
	public static final String[] MSG_TYPES = {"Position Report", "Text Message", "Data Request", "Flight Information",
						"Acknowledgement", "Diagnostic", "Authentication", "Raw Text", "Data Response", "Ping", "Disconnect",
						"End Flight", "System Message", "Flight Report", "Error", "Dispatch Data", "MP Update", "MP Init"};

	/**
	 * Returns the message type.
	 * @return the type code
	 */
	public int getType();
	
	public boolean isPublic();
	public boolean isAnonymous();
	
	public void setTime(long ts);
	public long getTime();
	
	void setProtocolVersion(int pVersion);
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
	
	public void setSender(Pilot msgFrom);
	public Pilot getSender();
	public String getSenderID();
}