// Copyright (c) 2005 Luke J. Kolin. All Rights Reserved.
package org.deltava.acars.command;

import org.deltava.acars.beans.Envelope;

/**
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public interface ACARSCommand  {

	public void execute(CommandContext ctx, Envelope env);
}