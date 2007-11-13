package org.deltava.acars.pool;

import java.util.concurrent.*;

import junit.framework.TestCase;

public class TestThreadPool extends TestCase {
	
	private QueueingThreadPool _pool;

	protected void setUp() throws Exception {
		super.setUp();
		_pool = new ACARSExecutor(1, 4, 500);
		assertNotNull(_pool);
	}

	protected void tearDown() throws Exception {
		_pool.shutdown();
		_pool.awaitTermination(2000, TimeUnit.MILLISECONDS);
		_pool = null;
		super.tearDown();
	}

	/* public void testCoreThreads() throws Exception {
		for (int x = 0; x < 5; x++)
			_pool.execute(new DummyTask(String.valueOf(x + 1)));
			
		for (int x = 0; x < 5; x++)
			_pool.execute(new DummyTask(String.valueOf(x + 1)));
		
		Thread.sleep(1000);
	}*/
	
	public void testThreadShutdown() throws Exception {
		_pool.setThreadFactory(new TestFactory());
		for (int x = 0; x < 2; x++)
			_pool.execute(new DummyTask(String.valueOf(x + 1)));
		
		Thread.sleep(1000);
	}
}