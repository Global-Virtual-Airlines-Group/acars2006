// Copyright 2004, 2005, 2006 Global Virtual Airline Group. All Rights Reserved.
package org.deltava.acars.beans;

import java.util.*;

/**
 * An ACARS server message stack
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class MessageStack implements Iterator {

   public static final MessageStack RAW_INPUT = new MessageStack();
   public static final MessageStack MSG_INPUT = new MessageStack();
   public static final MessageStack MSG_OUTPUT = new MessageStack();
   public static final MessageStack RAW_OUTPUT = new MessageStack();
   
	private final List<Envelope> _data = new ArrayList<Envelope>();

	private MessageStack() {
		super();
	}

	public synchronized void push(Envelope env) {
		_data.add(env);
	}
	
	public synchronized void push(Collection<Envelope> envs) {
		_data.addAll(envs);
	}
	
	public synchronized Envelope pop() {
		
		// If there is nothing in the stack, get nothing
		if (_data.size() == 0)
			return null;
		
		// Get the first element and nuke it
		Envelope env = _data.get(0);
		_data.remove(0);
		return env;
	}

	public synchronized int size() {
		return _data.size();
	}

	// Iterator implementation methods
	public synchronized boolean hasNext() {
		return (_data.size() > 0);
	}
	
	public synchronized Object next() {
		
		// Throw an exception if nothing there
		if (_data.size() == 0)
			throw new NoSuchElementException();

		// Return the next element
		return pop();
	}
	
	public void remove() {
		// Not implemented since next automatically does a remove 
	}
	
	public synchronized void wakeup() {
		notifyAll();
	}
	
	public synchronized void waitForActivity() throws InterruptedException {
	   wait();
	}
}