// Copyright 2006, 2009, 2020 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import org.apache.log4j.Logger;

/**
 * An ACARS server command to process Dispatch Messages.
 * @author Luke
 * @version 9.1
 * @since 1.0
 */

public abstract class DispatchCommand extends ACARSCommand {
	
	protected final Logger log;

	/**
	 * Initializes the Command.
	 */
	protected DispatchCommand() {
		super();
		log = Logger.getLogger(getClass().getPackageName() + ".Dispatch" + getClass().getName());
	}
}