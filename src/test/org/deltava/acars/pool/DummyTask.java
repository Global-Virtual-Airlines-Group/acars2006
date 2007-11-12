package org.deltava.acars.pool;

import org.deltava.util.ThreadUtils;

public class DummyTask implements Runnable {
	
	private String _name;

	DummyTask(String name) {
		super();
		_name = name;
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