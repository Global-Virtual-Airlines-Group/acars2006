package org.deltava.acars.message;

import org.deltava.beans.Pilot;

/**
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class TextMessage extends AbstractMessage {

	private String _text;
	private String _recipient;

	public final static int MAX_MSG_SIZE = 1024;

	public TextMessage(Pilot msgFrom, String msgText) {
		super(Message.MSG_TEXT, msgFrom);
		_text = (msgText.length() > MAX_MSG_SIZE) ? msgText.substring(0, MAX_MSG_SIZE) : msgText;
	}
	
	// 
	public String getRecipient() {
		return _recipient;
	}
	
	public String getText() {
		return _text; 
	}
	
	public final boolean isPublic() {
		return (_recipient == null);
	}
	
	public void setRecipient(String msgTo) {
		_recipient = msgTo;
	}
	
	public void setText(String txt) {
		_text = txt;
	}
}