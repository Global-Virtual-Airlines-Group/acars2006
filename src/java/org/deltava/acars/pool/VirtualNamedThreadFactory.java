package org.deltava.acars.pool;

import java.util.concurrent.ThreadFactory;

public class VirtualNamedThreadFactory implements ThreadFactory {
	@Override
	public Thread newThread(Runnable r) {
		return Thread.ofVirtual().name(r.toString()).unstarted(r);
	}
}