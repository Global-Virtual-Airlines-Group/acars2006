// Copyright 2004, 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

/**
 * An ACARS message with an optional recipeint.
 * @author Luke
 * @version 2.8
 * @since 2.8
 */

public interface RecipientMessage extends Message {

	/**
	 * Returns the recipient pilot code.
	 * @return the pilot code
	 */
	public String getRecipient();
	
	/**
	 * Updates the recipient of this message.
	 * @param msgTo the recipient pilot code, or null if none;
	 */
	public void setRecipient(String msgTo);
}