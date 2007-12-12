// Copyright 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import org.apache.log4j.Logger;

/**
 * An ACARS server command to process Dispatch Messages.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public abstract class DispatchCommand extends ACARSCommand {
	
	protected Logger log;

	/**
	 * Initializes the Command.
	 * @param logClass the Log4j logging class
	 */
	protected DispatchCommand(Class logClass) {
		super();
		log = Logger.getLogger(logClass);
	}
}