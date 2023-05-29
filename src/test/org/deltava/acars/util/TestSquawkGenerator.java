package org.deltava.acars.util;

import org.apache.logging.log4j.*;

import java.util.*;

import junit.framework.TestCase;

import org.deltava.acars.beans.TXCode;

public class TestSquawkGenerator extends TestCase {
	
	private Logger log;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		// Init Log4j
		log = LogManager.getLogger(TestSquawkGenerator.class);
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