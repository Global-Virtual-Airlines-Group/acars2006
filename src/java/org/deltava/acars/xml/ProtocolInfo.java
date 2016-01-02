// Copyright 2004, 2016 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml;

/**
 * An interface to store ACARS protocol constants.
 * @author luke
 * @version 6.4
 * @since 1.0
 */

public interface ProtocolInfo {
	
	/**
	 * Standard XML header string.
	 */
	public final static String XML_HEADER = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";
	
	/**
	 * Request element name.
	 */
	public final static String REQ_ELEMENT_NAME = "ACARSRequest";
	
	/**
	 * Response element name.
	 */
	public final static String RSP_ELEMENT_NAME = "ACARSResponse";
	
	/**
	 * Command element name.
	 */
	public final static String CMD_ELEMENT_NAME = "CMD";
	
	/**
	 * Request element XML open substring.
	 */
	public final static String REQ_ELEMENT_OPEN = "<" + REQ_ELEMENT_NAME;
	
	/**
	 * Request element XML close substring.
	 */
	public final static String REQ_ELEMENT_CLOSE = "</" + REQ_ELEMENT_NAME + ">";
}
