package org.deltava.acars.pool;

import org.deltava.acars.beans.LatencyWorkerStatus;

import org.deltava.util.ThreadUtils;

public class DummyTask implements PoolWorker {
	
	private String _name;
	private LatencyWorkerStatus _status;

	DummyTask(String name) {
		super();
		_name = name;
	}
	
	public LatencyWorkerStatus getStatus() {
		return _status;
	}
	
	public void setStatus(LatencyWorkerStatus ws) {
		_status = ws;
	}
	
	public String getName() {
		return _name;
	}
	
	public void run() {
		ThreadUtils.sleep(1000);
	}
	
	public int hashCode() {
		return _name.hashCode();
	}

	public String toString() {
		return _name;
	}
}