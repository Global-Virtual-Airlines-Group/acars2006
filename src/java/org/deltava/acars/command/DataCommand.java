// Copyright 2004, 2005, 2006, 2009, 2020, 2023 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import org.apache.logging.log4j.*;

/**
 * An ACARS server command to handle data requests.
 * @author Luke
 * @version 11.0
 * @since 1.0
 */

public abstract class DataCommand extends ACARSCommand {

	protected final Logger log;

	/**
	 * Initializes the Command.
	 */
	protected DataCommand() {
		super();
		log = LogManager.getLogger(getClass());
	}
}