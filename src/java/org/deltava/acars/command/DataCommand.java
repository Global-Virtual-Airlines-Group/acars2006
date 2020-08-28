// Copyright 2004, 2005, 2006, 2009, 2020 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import org.apache.log4j.Logger;

/**
 * An ACARS server command to handle data requests.
 * @author Luke
 * @version 9.1
 * @since 1.0
 */

public abstract class DataCommand extends ACARSCommand {

	protected final Logger log;

	/**
	 * Initializes the Command.
	 */
	protected DataCommand() {
		super();
		log = Logger.getLogger(getClass());
	}
}