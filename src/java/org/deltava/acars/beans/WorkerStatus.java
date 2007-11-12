// Copyright 2004, 2005, 2006, 2007 Global Virtual Airline Group. All Rights Reserved.
package org.deltava.acars.beans;

import org.deltava.beans.ViewEntry;

/**
 * A bean to return ACARS worker thread information.
 * @author Luke
 * @version 2.0
 * @since 1.0
 */

public class WorkerStatus implements Comparable<WorkerStatus>, ViewEntry {
	
	public static final int STATUS_UNKNOWN = 0;
	public static final int STATUS_SHUTDOWN = 1;
	public static final int STATUS_RECYCLE = 2;
	public static final int STATUS_ERROR = 3;
	public static final int STATUS_START = 4;
	public static final int STATUS_INIT = 5;
	
	public static final String[] STATUS_NAME = {"Unknown", "Shutdown", "Recycle Request", "Error", "Started", "Initializing" };
	
	private long _execStartTime;
	private long _execStopTime;
	
	private String _name;
	private String _msg;
	private int _status;
	private long _execCount;
	private boolean _isRunning;
	
	public WorkerStatus(String name) {
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
	
	public synchronized void setMessage(String msg) {
		_msg = msg;
	}
	
	public void setAlive(boolean isAlive) {
		_isRunning = isAlive;
	}
	
	public synchronized void setStatus(int newStatus) {
		if ((newStatus >= 0) && (newStatus < STATUS_NAME.length)) {
			_status = newStatus;
		} else {
			_status = STATUS_UNKNOWN;
		}
	}
	
	public synchronized void execute() {
		_execStartTime = System.currentTimeMillis();
		_execStopTime = 0;
	}
	
	public synchronized void complete() {
		_execStopTime = System.currentTimeMillis();
		_execCount++;
	}
	
	public synchronized long getExecutionTime() {
		if (_execStartTime == 0)
			return 0;
		
		return ((_execStopTime == 0) ? System.currentTimeMillis() : _execStopTime) - _execStartTime;
	}
	
	/**
	 * Compares two workers by comparing their names.
	 */
	public int compareTo(WorkerStatus ws2) {
		return _name.compareTo(ws2._name);
	}
	
	/**
	 * Returns the worker name.
	 */
	public String toString() {
		return _name;
	}
	
	public String getRowClassName() {
		return _isRunning ? null : "warn";
	}
}