// Copyright (c) 2005 Luke J. Kolin. All Rights Reserved.
package org.deltava.acars.command;

import org.deltava.acars.beans.Envelope;

/**
 * An ACARS server command object.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public abstract class ACARSCommand  {
	
	protected static final int MAX_EXEC_TIME = 1500; 

	/**
	 * Returns the maximum execution time of this command before a warning should be issued.
	 * @return the maximum execution time in milliseconds
	 */
	public int getMaxExecTime() {
		return MAX_EXEC_TIME;
	}
	
	/**
	 * Executes the ACARS server command.
	 * @param ctx the command context
	 * @param env the Envelope to process
	 */
	public abstract void execute(CommandContext ctx, Envelope env);
}