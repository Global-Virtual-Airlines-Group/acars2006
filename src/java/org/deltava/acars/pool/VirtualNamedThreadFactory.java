// Copyright 2026 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.pool;

import java.util.concurrent.ThreadFactory;

/**
 * A ThreadFactory that creates virtual Threads named after the Runnable they execute.
 * @author Luke
 * @version 13.0
 * @since 13.0
 */
public class VirtualNamedThreadFactory implements ThreadFactory {
	@Override
	public Thread newThread(Runnable r) {
		return Thread.ofVirtual().name(r.toString()).unstarted(r);
	}
}