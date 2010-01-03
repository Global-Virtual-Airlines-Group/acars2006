// Copyright 2006, 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import org.apache.log4j.Logger;

/**
 * An ACARS server command to process Dispatch Messages.
 * @author Luke
 * @version 2.8
 * @since 1.0
 */

public abstract class ViewerCommand extends ACARSCommand {
	
	protected Logger log;

	/**
	 * Initializes the Command.
	 * @param logClass the Log4j logging class
	 */
	protected ViewerCommand(Class<?> logClass) {
		super();
		Package p = logClass.getPackage();
		log = Logger.getLogger(p.getName() + ".View" + logClass.getName());
	}
}