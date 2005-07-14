package org.deltava.acars.workers;

/**
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class WorkerStatus implements java.io.Serializable {
	
	public static final int STATUS_UNKNOWN = 0;
	public static final int STATUS_SHUTDOWN = 1;
	public static final int STATUS_RECYCLE = 2;
	public static final int STATUS_ERROR = 3;
	public static final int STATUS_START = 4;
	public static final int STATUS_INIT = 5;
	
	private String workerMessage;
	private int workerStatus;

	public synchronized String getMessage() {
		return this.workerMessage;
	}
	
	public synchronized int getStatus() {
		return this.workerStatus;
	}
	
	synchronized void setMessage(String msg) {
		this.workerMessage = msg;
	}
	
	synchronized void setStatus(int newStatus) {
		if ((newStatus >= 0) && (newStatus <= 4))
			this.workerStatus = newStatus;
	}
}