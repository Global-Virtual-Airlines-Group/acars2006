/*
 * Created on Feb 11, 2004
 *
 * This allows us to dump raw text to the socket
 */
package org.deltava.acars.message;

/**
 * @author Luke J. Kolin
 *
 */
public class RawMessage extends AbstractMessage {

	private String msgText;
	
	public RawMessage(String msg) {
		super(Message.MSG_RAW, null);
		this.msgText = msg;
	}
	
	public String getText() {
		return this.msgText;
	}
	
	public boolean isAnonymous() {
		return true;
	}
}
