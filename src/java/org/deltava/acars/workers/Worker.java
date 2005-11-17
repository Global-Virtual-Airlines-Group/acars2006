package org.deltava.acars.workers;

import org.apache.log4j.Logger;

/**
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public abstract class Worker implements Runnable {
	
	protected Logger log;
	private String _name;
	
	protected WorkerStatus _status;
	
	protected abstract void $run0() throws Exception;

	protected Worker(String name, Class loggerClass) {
		_name = name.trim();
		_status = new WorkerStatus(name);
		log = Logger.getLogger(loggerClass);
	}

	public final WorkerStatus getStatus() {
		return _status;
	}
	
	public final String getName() {
		return _name;
	}

	// Default placeholder for the open() method; just sets status
	public void open() {
		_status.setStatus(WorkerStatus.STATUS_INIT);
		_status.setMessage("Initializing");
	}

	// Default placeholder for the close() method; just sets status
	public void close() {
		_status.setStatus(WorkerStatus.STATUS_UNKNOWN);
		_status.setMessage("Shut Down");
	}

	// public thread interface which catches exceptions
	public final void run() {
		try {
			_status.setStatus(WorkerStatus.STATUS_START);
			$run0();
			_status.setStatus(WorkerStatus.STATUS_SHUTDOWN);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			_status.setStatus(WorkerStatus.STATUS_ERROR);
			_status.setMessage(e.getMessage());
		}
	}
}