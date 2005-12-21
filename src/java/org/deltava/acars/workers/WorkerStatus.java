package org.deltava.acars.workers;

import org.deltava.beans.ViewEntry;

/**
 * A bean to return ACARS worker thread information.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class WorkerStatus implements java.io.Serializable, Comparable, ViewEntry {
	
	public static final int STATUS_UNKNOWN = 0;
	public static final int STATUS_SHUTDOWN = 1;
	public static final int STATUS_RECYCLE = 2;
	public static final int STATUS_ERROR = 3;
	public static final int STATUS_START = 4;
	public static final int STATUS_INIT = 5;
	
	public static final String[] STATUS_NAME = {"Unknown", "Shutdown Request", "Recycle Request", "Error",
		"Starting", "Initializing" };
	
	private String _name;
	private String _msg;
	private int _status;
	private long _execCount;
	private boolean _isRunning;
	
	WorkerStatus(String name) {
		super();
		_name = name;
	}

	public String getMessage() {
		return _msg;
	}
	
	public synchronized int getStatus() {
		return _status;
	}
	
	public boolean getAlive() {
		return _isRunning;
	}
	
	public String getStatusName() {
		return STATUS_NAME[getStatus()];
	}
	
	public long getExecutionCount() {
		return _execCount;
	}
	
	public void setMessage(String msg) {
		_msg = msg;
	}
	
	public void setAlive(boolean isAlive) {
		_isRunning = isAlive;
	}
	
	synchronized void setStatus(int newStatus) {
		if ((newStatus >= 0) && (newStatus < STATUS_NAME.length)) {
			_status = newStatus;
		} else {
			_status = STATUS_UNKNOWN;
		}
	}
	
	synchronized void execute() {
		_execCount++;
	}
	
	public int compareTo(Object o2) {
		WorkerStatus ws2 = (WorkerStatus) o2;
		return _name.compareTo(ws2._name);
	}
	
	public String toString() {
		return _name;
	}
	
	public String getRowClassName() {
		return _isRunning ? null : "warn";
	}
}