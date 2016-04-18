package org.deltava.acars.pool;

import org.deltava.util.ThreadUtils;

public class DummyTask extends PoolWorker {
	
	private String _name;

	DummyTask(String name) {
		super();
		_name = name;
	}
	
	@Override
	public String getName() {
		return _name;
	}
	
	@Override
	public void run() {
		ThreadUtils.sleep(1000);
	}
	
	@Override
	public int hashCode() {
		return _name.hashCode();
	}

	@Override
	public String toString() {
		return _name;
	}
}