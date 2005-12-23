// Copyright (c) 2005 Delta Virtual Airlines. All Rights Reserved.
package org.deltava.acars.beans;

import java.io.*;
import java.net.*;

import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * An ACARS Connection that dumps messages to a text file.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */
public class ACARSDebugConnection extends ACARSConnection {

	private PrintWriter _debugWriter;

	public ACARSDebugConnection(long cid, SocketChannel sc) {
		super(cid, sc);

		// Create the debug file
		File debugFile = new File(System.getProperty("java.io.tmpdir"), Long.toHexString(cid) + ".txt");
		try {
			_debugWriter = new PrintWriter(new FileWriter(debugFile), false);
		} catch (IOException ie) {
			log.warn("Error creating debug file " + debugFile.getAbsolutePath());
		}
		
		// Log connection info
		InetAddress addr = sc.socket().getInetAddress();
		_debugWriter.println("Connection to " + addr.getHostAddress() + "(" + addr.getHostName() + ")");
		_debugWriter.println("Connected on " + new java.util.Date());
		_debugWriter.println("*****");
	}

	/**
	 * Closes the connection.
	 */
	public void close() {
		super.close();
		
		// Log connection close
		_debugWriter.println("*****");
		_debugWriter.println("Closed on " + new java.util.Date());
		_debugWriter.flush();
		_debugWriter.close();
	}

	/**
	 * Reads a message from the connection.
	 * @return the XML message
	 */
	String read() throws SocketException, ProtocolException {
		String msg = super.read();

		// Dump the message
		_debugWriter.println("-- in");
		_debugWriter.println(msg);
		_debugWriter.println();
		_debugWriter.flush();
		return msg;
	}

	/**
	 * Writes a message to the connection.
	 * @param msg the message text
	 */
	public final void flush() {
		for (Iterator<String> i = _msgOutBuffer.iterator(); i.hasNext(); ) {
			String msg = i.next();
			i.remove();
			_debugWriter.println("-- out");
			_debugWriter.println(msg);
			_debugWriter.println();
			_debugWriter.flush();
			write(msg);
		}
	}
}