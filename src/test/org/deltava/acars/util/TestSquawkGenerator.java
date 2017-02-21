package org.deltava.acars.util;

import org.apache.log4j.*;

import java.util.*;

import junit.framework.TestCase;

import org.deltava.acars.beans.TXCode;

public class TestSquawkGenerator extends TestCase {
	
	private Logger log;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		// Init Log4j
		PropertyConfigurator.configure("etc/log4j.test.properties");
		log = Logger.getLogger(TestSquawkGenerator.class);
	}

	@Override
	protected void tearDown() throws Exception {
		LogManager.shutdown();
		super.tearDown();
	}

	public void testGenerate() {
		
		Collection<Integer> codes = new LinkedHashSet<Integer>();
		for (int x = 0; x < 250; x++) {
			TXCode tx = SquawkGenerator.generate(null);
			assertNotNull(tx);
			for (int y = 0; y < 4; y++) {
				if (codes.contains(Integer.valueOf(tx.getCode())))
					tx = SquawkGenerator.generate(null);
			}
			
			boolean isUnique = codes.add(Integer.valueOf(tx.getCode()));
			assertTrue(isUnique);
			log.info("Registered code " + tx);
		}
	}
}