// Copyright (c) 2005 Luke J. Kolin. All Rights Reserved.
package org.deltava.acars.message;

import java.util.*;

/**
 * A class to store ACARS server messages.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class SystemTextMessage extends AbstractMessage {
	
	private List _msgs;

	public SystemTextMessage() {
		super(Message.MSG_SYSTEM, null);
		_msgs = new ArrayList();
	}

	public void addMessage(String msg) {
		_msgs.add(msg);
	}
	
	public void addMessages(Collection msgs) {
		_msgs.addAll(msgs);
	}
	
	public Collection getMsgs() {
		return _msgs;
	}
}