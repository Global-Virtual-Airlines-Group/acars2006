package org.deltava.acars.message;

import java.util.*;

import org.deltava.beans.Pilot;

/**
 * @author Luke J. Kolin
 * @version 1.0
 * @since 1.0
 */

public class DataRequestMessage extends DataMessage {
	
	private Set _flags = new HashSet();
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
	
	public void addFlag(String newFlag) {
		_flags.add(newFlag.toUpperCase());
	}

	public String getFilter() {
		return _reqFilter;
	}

	public Collection getFlags() {
		return _flags;
	}
	
	public String getAllFlags() {
		
		StringBuffer buf = new StringBuffer();
		for (Iterator i = _flags.iterator(); i.hasNext(); ) {
			buf.append((String) i.next());
			if (i.hasNext())
				buf.append(FILTER_SPACER);
		}
		
		// Return the buffer
		return buf.toString();
	}
	
	public boolean hasFlag(String flagName) {
		return _flags.contains(flagName.toUpperCase());
	}
	
	public void setFilter(String newFilter) {
		if (newFilter != null)
			_reqFilter = newFilter;
	}
	
	public void setFlags(String newFlags) {
		
		// Split using tokenizer
		StringTokenizer fTokens = new StringTokenizer(newFlags, FILTER_SPACER);
		while (fTokens.hasMoreTokens())
			addFlag(fTokens.nextToken());
	}
}