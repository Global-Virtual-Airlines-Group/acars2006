// Copyright (c) 2005, 2006 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import java.util.*;

/**
 * A class to store ACARS server messages.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class SystemTextMessage extends AbstractMessage {
	
	private Collection<String> _msgs;

	public SystemTextMessage() {
		super(Message.MSG_SYSTEM, null);
		_msgs = new ArrayList<String>();
	}

	public void addMessage(String msg) {
		_msgs.add(msg);
	}
	
	public void addMessages(Collection<? extends String> msgs) {
		_msgs.addAll(msgs);
	}
	
	public Collection<String> getMsgs() {
		return _msgs;
	}
}