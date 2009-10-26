// Copyright 2004, 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars;

/**
 * An ACARS exception.
 * @author Luke
 * @version 2.6
 * @since 1.0
 */

public class ACARSException extends Exception {
	
	public ACARSException(String msg) {
		super(msg);
	}
	
	public ACARSException(Throwable t) {
		super(t);
	}
}