// Copyright 2005, 2006, 2008, 2011, 2012 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import org.deltava.acars.beans.MessageEnvelope;

/**
 * An ACARS server command object.
 * @author Luke
 * @version 4.2
 * @since 1.0
 */

public abstract class ACARSCommand  {
	
	/**
	 * Returns the maximum execution time of this command before a warning should be issued.
	 * @return the maximum execution time in milliseconds
	 */
	@SuppressWarnings("all")
	public int getMaxExecTime() {
		return 1500;
	}
	
	/**
	 * Executes the ACARS server command.
	 * @param ctx the command context
	 * @param env the Envelope to process
	 */
	public abstract void execute(CommandContext ctx, MessageEnvelope env);
}