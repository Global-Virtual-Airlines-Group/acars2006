package org.deltava.acars.pool;

import junit.framework.TestCase;

public class TestConnectionPool extends TestCase {
	
	private QueueingThreadPool _pool;

	protected void setUp() throws Exception {
		super.setUp();
		_pool = new ACARSExecutor(1, 4, 50);
		assertNotNull(_pool);
	}

	protected void tearDown() throws Exception {
		_pool.shutdown();
		while (!_pool.isTerminated())
			Thread.sleep(150);
		
		super.tearDown();
	}

	public void testCoreThreads() throws Exception {
		_pool.prestartCoreThread();
		for (int x = 0; x < 5; x++)
			_pool.execute(new DummyTask(String.valueOf(x + 1)));
			
		for (int x = 0; x < 5; x++)
			_pool.execute(new DummyTask(String.valueOf(x + 1)));
		
		Thread.sleep(1000);
	}
}