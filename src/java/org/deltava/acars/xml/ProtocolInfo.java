/*
 * Created on Feb 15, 2004
 *
 * XML Message constants
 */
package org.deltava.acars.xml;

/**
 * @author Luke J. Kolin
 */
public interface ProtocolInfo {
	
	// Standard XML header string
	public final static String XML_HEADER = "<?xml version=\"1.0\" ?>";
	
	// Request and command element names
	public final static String REQ_ELEMENT_NAME = "ACARSRequest";
	public final static String RSP_ELEMENT_NAME = "ACARSResponse";
	public final static String CMD_ELEMENT_NAME = "CMD";
	
	// Request element open/close tags for use in reading from the socket
	public final static String REQ_ELEMENT_OPEN = "<" + REQ_ELEMENT_NAME;
	public final static String REQ_ELEMENT_CLOSE = "</" + REQ_ELEMENT_NAME + ">";
}
