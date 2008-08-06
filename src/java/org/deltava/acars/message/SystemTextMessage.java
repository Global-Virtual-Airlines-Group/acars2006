// Copyright 2005, 2006, 2008 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import java.util.*;

/**
 * A class to store ACARS server messages.
 * @author Luke
 * @version 2.2
 * @since 1.0
 */

public class SystemTextMessage extends AbstractMessage {
	
	private final Collection<String> _msgs = new ArrayList<String>();

	public SystemTextMessage() {
		super(Message.MSG_SYSTEM, null);
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