// Copyright 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.pool;

public class TestFactory extends PoolWorkerFactory {

	public TestFactory() {
		super("Test");
	}
	
	private class PoolDebugThread extends PoolWorkerFactory.PoolThread {
		
		PoolDebugThread(int id, Runnable r, String name) {
			super(id, r, name);
		}
		
		public void run() {
			super.run();
			System.out.println(getName() + " Shut Down");
		}
	}
	
	public Thread newThread(Runnable r) {
		int id = getNextID();
		return new PoolDebugThread(id, r, _name + "-" + String.valueOf(id));
	}
}