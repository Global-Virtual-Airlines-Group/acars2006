package org.deltava.acars.workers;

/**
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public abstract class WorkerThread extends Thread {

	protected WorkerStatus _status;
	
	protected abstract void $run0() throws Exception;

	protected WorkerThread(ThreadGroup tg, String wName) {
		super(tg, null, wName);
		_status = new WorkerStatus();
	}

	public final WorkerStatus getWorkerStatus() {
		return _status;
	}

	// Default placeholder for the open() method; just sets status
	public void open() {
		_status.setStatus(WorkerStatus.STATUS_INIT);
	}

	// Default placeholder for the close() method; just sets status
	public void close() {
		_status.setStatus(WorkerStatus.STATUS_UNKNOWN);
	}

	// public thread interface which catches exceptions
	public final void run() {
		try {
			_status.setStatus(WorkerStatus.STATUS_START);
			$run0();
			_status.setStatus(WorkerStatus.STATUS_SHUTDOWN);
		} catch (Exception e) {
			_status.setStatus(WorkerStatus.STATUS_ERROR);
			_status.setMessage(e.getMessage());
			e.printStackTrace(System.err);
		}
	}
}