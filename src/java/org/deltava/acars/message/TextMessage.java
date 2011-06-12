// Copyright 2004, 2009, 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.Pilot;

/** 
 * An ACARS message for text messaging.
 * @author Luke
 * @version 4.0
 * @since 1.0
 */

public class TextMessage extends RecipientMessage {

	private String _text;

	public final static int MAX_MSG_SIZE = 1024;

	public TextMessage(Pilot msgFrom, String msgText) {
		super(Message.MSG_TEXT, msgFrom);
		_text = (msgText.length() > MAX_MSG_SIZE) ? msgText.substring(0, MAX_MSG_SIZE) : msgText;
	}
	
	public String getText() {
		return _text; 
	}
	
	public final boolean isPublic() {
		return (getRecipient() == null);
	}
	
	public void setText(String txt) {
		_text = txt;
	}
}