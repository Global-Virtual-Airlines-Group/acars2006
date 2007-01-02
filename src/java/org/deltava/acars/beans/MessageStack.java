// Copyright 2004, 2005, 2006 Global Virtual Airline Group. All Rights Reserved.
package org.deltava.acars.beans;

import java.util.*;

/**
 * An ACARS server message stack.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class MessageStack<T extends Envelope> implements Iterator<T> {

	public static final MessageStack<TextEnvelope> RAW_INPUT = new MessageStack<TextEnvelope>();
	public static final MessageStack<MessageEnvelope> MSG_INPUT = new MessageStack<MessageEnvelope>();
	public static final MessageStack<MessageEnvelope> MSG_OUTPUT = new MessageStack<MessageEnvelope>();
	public static final MessageStack<TextEnvelope> RAW_OUTPUT = new MessageStack<TextEnvelope>();

	private final List<T> _data = new ArrayList<T>();

	private MessageStack() {
		super();
	}

	/**
	 * Pushes an Envelope onto the Stack.
	 * @param env the Envelope
	 */
	public synchronized void push(T env) {
		_data.add(env);
	}

	/**
	 * Pushes a number of Envelopes onto the Stack.
	 * @param envs a Collection Envelopes
	 */
	public synchronized void push(Collection<T> envs) {
		_data.addAll(envs);
	}

	public synchronized T pop() {

		// If there is nothing in the stack, get nothing
		if (_data.size() == 0)
			return null;

		// Get the first element and nuke it
		T env = _data.get(0);
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

	public synchronized T next() {

		// Throw an exception if nothing there
		if (_data.size() == 0)
			throw new NoSuchElementException();

		// Return the next element
		return pop();
	}

	public void remove() {
		// Not implemented since next automatically does a remove
	}

	/**
	 * Notifies threads waiting on this queue.
	 * @param doAll TRUE if all threads should be notified, otherwise FALSE
	 */
	public synchronized void wakeup(boolean doAll) {
		if (doAll)
			notifyAll();
		else
			notify();
	}

	/**
	 * Waits for the queue to be notified by another thread.
	 */
	public synchronized void waitForActivity() {
		try {
			wait();
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
	}
}