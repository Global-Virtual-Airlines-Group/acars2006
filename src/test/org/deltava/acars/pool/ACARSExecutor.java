package org.deltava.acars.pool;

import java.util.concurrent.*;

public class ACARSExecutor extends ThreadPoolExecutor {

	public ACARSExecutor(int coreSize, int maxSize, long keepAliveTime, BlockingQueue<Runnable> workQueue) {
		super(coreSize, maxSize, keepAliveTime, TimeUnit.MILLISECONDS, workQueue);
	}

	public ACARSExecutor(int coreSize, int maxSize, long keepAliveTime, BlockingQueue<Runnable> workQueue, 
			ThreadFactory threadFactory) {
		super(coreSize, maxSize, keepAliveTime, TimeUnit.SECONDS, workQueue, threadFactory);
	}

	protected void beforeExecute(Thread t, Runnable r) {
		System.out.println(t.getName() + " starting " + r.toString());
	}
	
	protected void afterExecute(Runnable r, Throwable t) {
		System.out.println(r.toString() + " finished");
	}
}