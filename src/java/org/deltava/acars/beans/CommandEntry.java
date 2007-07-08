// Copyright 2007 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

import java.util.Date;

/**
 * A bean to log ACARS command invocations.
 * @author Luke
 * @version 1.0
 * @since 1.1
 */

public class CommandEntry implements Comparable<CommandEntry> {
	
	private String _name;
	private final Date _execDate = new Date();
	private long _execTime;

	/**
	 * Initializes the bean.
	 * @param cmd the Command class
	 * @param execTime the execution time in milliseconds
	 */
	public CommandEntry(Class cmd, long execTime) {
		super();
		_name = cmd.getSimpleName();
		_execTime = execTime;
	}

	/**
	 * Returns the Command name.
	 * @return the name
	 */
	public String getName() {
		return _name;
	}
	
	/**
	 * Returns the execution Date.
	 * @return the execution date/time
	 */
	public Date getDate() {
		return _execDate;
	}
	
	/**
	 * Returns the execution time.
	 * @return the execution time in milliseconds
	 */
	public long getExecTime() {
		return _execTime;
	}
	
	/**
	 * Compares two entries by comparing their dates and names.
	 */
	public int compareTo(CommandEntry e2) {
		int tmpResult = _execDate.compareTo(e2._execDate);
		return (tmpResult == 0) ? _name.compareTo(e2._name) : tmpResult;
	}
}