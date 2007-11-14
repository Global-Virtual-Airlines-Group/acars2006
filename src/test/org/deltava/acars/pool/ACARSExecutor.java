package org.deltava.acars.pool;

public class ACARSExecutor extends QueueingThreadPool {

	public ACARSExecutor(int coreSize, int maxSize, long keepAliveTime) {
		super(coreSize, maxSize, keepAliveTime, ACARSExecutor.class);
	}

	protected void beforeExecute(Thread t, Runnable r) {
		super.beforeExecute(t, r);
		System.out.println(t.getName() + " starting " + r.toString());
	}
	
	protected void afterExecute(Runnable r, Throwable t) {
		super.afterExecute(r, t);
		System.out.println(r.toString() + " finished");
	}
}