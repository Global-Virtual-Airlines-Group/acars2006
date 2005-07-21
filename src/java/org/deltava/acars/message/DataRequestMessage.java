package org.deltava.acars.message;

import java.util.*;

import org.deltava.beans.Pilot;

/**
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class DataRequestMessage extends DataMessage {
	
	private Map _flags = new HashMap();
	private String _reqFilter = FILTER_WILDCARD;
	
	// Constants to use for wildcards and flag parsing
	private static final String FILTER_WILDCARD = "*";
	private static final String FILTER_SPACER = ","; 

	public DataRequestMessage(Pilot msgFrom, int rType) {
		super(Message.MSG_DATAREQ, msgFrom);
		setRequestType(rType);
	}
	
	public DataRequestMessage(Pilot msgFrom, String rType) {
		super(Message.MSG_DATAREQ, msgFrom);
		setRequestType(rType);
	}
	
	public void addFlag(String name, String value) {
		_flags.put(name.toUpperCase(), value);
	}

	public String getFilter() {
		return _reqFilter;
	}
	
	public String getFlag(String name) {
		return (String) _flags.get(name.toUpperCase());
	}

	public boolean hasFlag(String flagName) {
		return _flags.containsKey(flagName.toUpperCase());
	}
	
	public void setFilter(String newFilter) {
		if (newFilter != null)
			_reqFilter = newFilter;
	}
}