package org.deltava.acars.pool;

import org.deltava.util.ThreadUtils;

public class DummyTask extends PoolWorker {
	
	private String _name;

	DummyTask(String name) {
		super();
		_name = name;
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