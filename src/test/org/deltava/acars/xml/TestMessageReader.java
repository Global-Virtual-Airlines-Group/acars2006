package org.deltava.acars.xml;

import java.io.*;
import java.util.*;

import junit.framework.TestCase;

import org.deltava.acars.beans.*;

public class TestMessageReader extends TestCase {
	
	private MessageReader _reader;

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		_reader = null;
		super.tearDown();
	}
	
	private String loadMsg(String fName) {
		File f = new File("data", fName);
		assertTrue(f.exists());
		
		StringBuilder buf = new StringBuilder();
		try {
			BufferedReader br = new BufferedReader(new FileReader(f));
			while (br.ready()) {
				buf.append(br.readLine());
				buf.append(System.getProperty("line.separator"));
			}
			
			br.close();
		} catch (IOException ie) {
			System.err.println(ie.getMessage());
		}
		
		return buf.toString();
	}

	public void testMessage() throws XMLException {
		Envelope env = new Envelope(null, loadMsg("positionMsg.xml"), 1);
		assertNotNull(env);
		
		// Load the reader
		_reader = new MessageReader(env);
		assertNotNull(_reader);
		Collection msgs = _reader.parse();
		assertNotNull(msgs);
		assertEquals(1, msgs.size());
	}
	
	public void testMutipleRequestMessage() throws XMLException {
		Envelope env = new Envelope(null, loadMsg("multiMsg.xml"), 1);
		assertNotNull(env);
		
		// Load the reader
		_reader = new MessageReader(env);
		assertNotNull(_reader);
		Collection msgs = _reader.parse();
		assertNotNull(msgs);
		assertEquals(1, msgs.size());
	}
}