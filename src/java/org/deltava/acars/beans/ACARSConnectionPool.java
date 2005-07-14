package org.deltava.acars.beans;

import java.net.*;
import java.util.*;
import java.nio.channels.*;

import org.apache.log4j.Logger;

import org.deltava.acars.ACARSException;
import org.deltava.beans.Pilot;

/**
 * A TCP/IP Connection Pool.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class ACARSConnectionPool {
	
	private static final Logger log = Logger.getLogger(ACARSConnectionPool.class);
	
	// Hard-coded anonymous inactivity timeout (in ms)
	private static final long ANONYMOUS_INACTIVITY_TIMEOUT = 15000;

	// List of connections, disconnected connections and connection pool info
	private int maxSize;
	private List cons;
	private List disCon;
	
	// Inactivity timeout
	private long inactivityTimeout = -1;
	
	// The selector to use for non-blocking I/O reads
	private Selector cSelector;
	
	// The input/output stacks in raw text
	private MessageStack inputStack;
	private MessageStack outputStack;
	
	public ACARSConnectionPool(int mxSize) {
		super();
		
		// Init the maximum size and connection lists
		this.maxSize = (mxSize > 0) ? mxSize : -1;
		this.cons = new ArrayList();
		this.disCon = new ArrayList();
	}
	
	public void add(ACARSConnection c) throws ACARSException {
		
		// Check if we're already there
		if (cons.contains(c))
			throw new ACARSException("Connection already in pool");		
		
		if (cons.size() >= maxSize)
			throw new ACARSException("Connection Pool full");
			
		// Register the SocketChannel with the selector
		try {
			c.getChannel().register(cSelector, SelectionKey.OP_READ);
			cons.add(c);
		} catch (ClosedChannelException cce) {
			throw new ACARSException(cce.getMessage());
		}
	}
	
	public Collection checkConnections() {
		
		// Start with the list of dropped connections
		ArrayList disCons = new ArrayList(this.disCon);
		this.disCon.clear();
		
		// Build list of dropped connections; return it with just the dropped connections if we have no timeout
		if (this.inactivityTimeout == -1)
			return disCons;
		
		// Get current time to save repeated system calls
		long now = System.currentTimeMillis();
		
		// Loop through the channels
		for (Iterator i = cons.iterator(); i.hasNext(); ) {
			ACARSConnection con = (ACARSConnection) i.next();
			
			// Calculate the inactivity timeout
			long timeout = con.isAuthenticated() ? this.inactivityTimeout : ANONYMOUS_INACTIVITY_TIMEOUT;
			
			// Have we exceeded the timeout interval
			if ((now - con.getLastActivity()) > timeout) {
				con.close();
				i.remove();
				disCons.add(con);
			}
		}
		
		// Return the list of dropped connections
		return disCons;
	}
	
	public boolean hasConnection(String remoteAddr) {
		
		// Loop through the connections
		Iterator i = this.cons.iterator();
		while (i.hasNext()) {
			ACARSConnection c = (ACARSConnection) i.next();
			if (c.getRemoteAddr().equals(remoteAddr))
				return true;
		}
		
		// No connection from that address found, return false
		return false;
	}
	
	public Collection getAll() {
		return cons;
	}
	
	public ACARSConnection get(long cid) {
		for (Iterator i = cons.iterator(); i.hasNext(); ) {
			ACARSConnection c = (ACARSConnection) i.next();
			if (c.equals(cid)) return c;
		}
		
		// Return nothing if not found
		return null;
	}
	
	public Collection get(String pid) {
		
		// Wildcard matches everyone
		if (("*".equals(pid)) || (pid == null))
			return cons;

		// Build results
		Set results = new HashSet();
		for (Iterator i = cons.iterator(); i.hasNext(); ) {
			ACARSConnection c = (ACARSConnection) i.next();
			if (c.getUserID().equals(pid))
				results.add(c);
		}
		
		return results;
	}
	
	public Iterator get(Pilot userInfo) {
		
		// Build results list
		ArrayList results = new ArrayList();
		
		// Loop through the connections
		for (Iterator i = cons.iterator(); i.hasNext(); ) {
			ACARSConnection c = (ACARSConnection) i.next();
			if ((c.isAuthenticated()) && (c.getUser().equals(userInfo)))
				results.add(c);
		}
		
		// Return the iterator
		return results.iterator();
	}
	
	public ACARSConnection get(SocketChannel ch) {
		
		// Loop through the connections
		Iterator i = cons.iterator();
		while (i.hasNext()) {
			ACARSConnection c = (ACARSConnection) i.next();
			if (c.equals(ch)) return c;
		}
		
		// Return nothing if not found
		return null;
	}
	
	public void read() {
		
		// Get the list of channels waiting for input
		for (Iterator i = cSelector.selectedKeys().iterator(); i.hasNext(); ) {
			SelectionKey sKey = (SelectionKey) i.next();
			
			// If the selection key is ready for reading, get the Connection and read
			if (sKey.isReadable()) {
				ACARSConnection con = get((SocketChannel) sKey.channel());
				
				try {
					String msg = con.read();
					if (msg != null) {
						Envelope env = new Envelope(con.getUser(), msg, con.getID());
						inputStack.push(env);
					}
				} catch (SocketException se) {
					log.error(se.getMessage(), se);
					con.close();
					cons.remove(con);
				} catch (ProtocolException pe) {
					con.close();
					cons.remove(con);
					disCon.add(con);
				}
			}
			
			// Remove from the selected keys list
			i.remove();
		}
	}
	
	public void remove(String id) {
		
		for (Iterator i = cons.iterator(); i.hasNext(); ) {
			ACARSConnection c = (ACARSConnection) i.next();
			
			// Close/Remove if found
			if (c.equals(id)) {
				if (c.isConnected()) c.close();
				i.remove();
				break;
			}
		}
	}
	
	public void remove(ACARSConnection c) {
		
		// Find the connection
		int pos = cons.indexOf(c);
		if (pos != -1) {
			if (c.isConnected()) c.close();
			cons.remove(pos);
		}
	}
	
	public void setSelector(Selector cs) {
		cSelector = cs;
	}
	
	public void setStacks(MessageStack is, MessageStack os) throws ACARSException {
		
		// Check that the stacks are not null
		if ((is == null) || (os == null))
			throw new ACARSException("Input/Output stacks are NULL");
		
		// Check that we're not resetting anything
		if ((inputStack == null) && (outputStack == null)) {
			inputStack = is;
			outputStack = os;
		}
	}
	
	public void setTimeout(int toSeconds) {
		inactivityTimeout = (toSeconds < 60) ? -1 : (toSeconds * 1000); 
	}
	
	public void write() {
		// Loop through the raw output stack
		while (outputStack.hasNext()) {
			Envelope env = outputStack.pop();
			
			// Get the connection and write the message
			ACARSConnection c = get(env.getConnectionID());
			c.write((String) env.getMessage());
		}
	}
}