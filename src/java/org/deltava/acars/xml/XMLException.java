// Copyright 2004, 2006, 2009, 2016 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml;

/**
 * An XML parsing exception.
 * @author Luke
 * @version 7.0
 * @since 1.0
 */

public class XMLException extends Exception {

	private String _xml;
	
	/**
	 * Creates a new XML parsing exception.
	 * @param msg the exception message
	 */
	public XMLException(String msg) {
		super(msg);
	}
	
	/**
	 * Creates a new XML parsing exception.
	 * @param msg the exception message
	 * @param cause the root exception
	 */
	public XMLException(String msg, Throwable cause) {
		super(msg, cause);
	}
	
	/**
	 * Creates a new XML parsing exception.
	 * @param msg the exception message
	 * @param cause the root exception
	 * @param xml the offending XML
	 */
	public XMLException(String msg, Throwable cause, String xml) {
		super(msg, cause);
		_xml = xml;
	}
	
	/**
	 * Returns the offending XML snippet.
	 * @return the XML snippet
	 */
	public String getXML() {
		return _xml;
	}
	
	/**
	 * Displays the exception message and XML snippet (if any).
	 */
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder(getMessage());
		if (_xml != null) {
			buf.append("\n");
			buf.append(_xml);
		}
		
		return buf.toString();
	}
}