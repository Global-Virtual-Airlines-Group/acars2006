// Copyright 2005, 2006, 2007, 2011, 2016, 2023 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

import java.io.*;
import java.net.*;

import java.nio.channels.SocketChannel;

/**
 * An ACARS Connection that dumps messages to a text file.
 * @author Luke
 * @version 11.1
 * @since 1.0
 */

public class ACARSDebugConnection extends ACARSConnection {

	private transient PrintWriter _debugWriter;

	public ACARSDebugConnection(long cid, SocketChannel sc) {
		super(cid, sc);

		// Create the debug file
		File debugFile = new File(System.getProperty("java.io.tmpdir"), Long.toHexString(cid) + ".txt");
		try {
			_debugWriter = new PrintWriter(new FileWriter(debugFile), false);
		} catch (IOException ie) {
			log.warn("Error creating debug file {}", debugFile.getAbsolutePath());
		}

		// Log connection info
		InetAddress addr = sc.socket().getInetAddress();
		_debugWriter.println("Connection to " + addr.getHostAddress() + "(" + addr.getHostName() + ")");
		_debugWriter.println("Connected on " + java.time.Instant.now());
		_debugWriter.println("*****");
	}

	/**
	 * Closes the connection.
	 */
	@Override
	public void close() {
		super.close();

		// Log connection close
		_debugWriter.println("*****");
		_debugWriter.println("Closed on " + java.time.Instant.now());
		_debugWriter.flush();
		_debugWriter.close();
	}

	/**
	 * Reads a message from the connection.
	 * @return the XML message
	 */
	@Override
	String read() throws IOException {
		String msg = super.read();

		// Dump the message
		_debugWriter.println("-- in " + java.time.Instant.now());
		_debugWriter.println(msg);
		_debugWriter.println();
		_debugWriter.flush();
		return msg;
	}

	/**
	 * Writes a message to the connection.
	 * @param msg the message string
	 */
	@Override
	public final void write(String msg) {
		_debugWriter.println("-- out " + java.time.Instant.now());
		_debugWriter.println(msg);
		_debugWriter.println();
		super.write(msg);
		_debugWriter.flush();
	}
}