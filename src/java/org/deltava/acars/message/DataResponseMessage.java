// Copyright (c) 2005 Luke J. Kolin. All Rights Reserved.
package org.deltava.acars.message;

import java.util.*;

import org.deltava.acars.beans.ACARSConnection;
import org.deltava.beans.Pilot;
import org.deltava.beans.schedule.Airport;

/**
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class DataResponseMessage extends DataMessage {
	
	// Response data
	private List _rspData = new ArrayList();


	/**
	 * @param type
	 * @param msgFrom
	 */
	public DataResponseMessage(Pilot msgFrom, int rType) {
		super(Message.MSG_DATARSP, msgFrom);
		setRequestType(rType);
	}
	
	public DataResponseMessage(Pilot msgFrom, String rType) {
		super(Message.MSG_DATARSP, msgFrom);
		setRequestType(rType);
	}

	private void $addResponse(Object obj) {
		
		// Check if we're not already in the response
		if ((obj == null) || _rspData.contains(obj))
			return;
		
		_rspData.add(obj);
	}
	
	public void addResponse(Airport apBean) {
		$addResponse(apBean);
	}
	
	public void addResponse(Message rspBean) {
		$addResponse(rspBean);
	}
	
	public void addResponse(ACARSConnection con) {
		$addResponse(con);
	}

	public Collection getResponse() {
		return _rspData;
	}
}