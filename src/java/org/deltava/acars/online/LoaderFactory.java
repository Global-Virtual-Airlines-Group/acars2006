// Copyright 2020 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.online;

import java.util.concurrent.*;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A Thread Factory for online status loader threads.
 * @author Luke
 * @version 9.0
 * @since 9.0
 */

public class LoaderFactory implements ForkJoinWorkerThreadFactory {
	
	private final AtomicInteger _lastID = new AtomicInteger();

	@Override
	public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
		
		String name = "OnlineWorker-" + String.valueOf(_lastID.incrementAndGet());
		final ForkJoinWorkerThread worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
		worker.setName(name);
		worker.setDaemon(true);
		return worker;
	}
}