/*
 * Created on Feb 9, 2004
 *
 * FIFO Bean message stack
 */
package org.deltava.acars.beans;

import java.util.*;

/**
 * @author Luke J. Kolin
 */
public class MessageStack implements Iterator {

	private List _data;

	public MessageStack() {
		super();
		_data = new ArrayList();
	}

	public synchronized void push(Envelope env) {
		_data.add(env);
	}
	
	public synchronized Envelope pop() {
		
		// If there is nothing in the stack, get nothing
		if (_data.size() == 0)
			return null;
		
		// Get the first element and nuke it
		Envelope env = (Envelope) _data.get(0);
		_data.remove(0);
		return env;
	}

	public synchronized int size() {
		return _data.size();
	}

	// Iterator implementation methods
	public boolean hasNext() {
		return (_data.size() > 0);
	}
	
	public Object next() {
		
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
}