// Copyright (c) 2005 Luke J. Kolin. All Rights Reserved.
package org.deltava.acars.message;

import java.util.*;

import org.deltava.acars.beans.ACARSConnection;

import org.deltava.beans.Pilot;
import org.deltava.beans.schedule.Airport;

import org.deltava.acars.beans.NavigationRadioBean;

/**
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class DataResponseMessage extends DataMessage {
   
   public class TextElement {
      
      private String _name;
      private String _value;
      
      public TextElement(String name, String value) {
         super();
         _name = name;
         _value = value;
      }
      
      public String getName() {
         return _name;
      }
      
      public String getValue() {
         return _value;
      }
   }
	
	// Response data
	private Set _rspData = new HashSet();

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
		if (obj == null)
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
	
	public void addResponse(NavigationRadioBean navaid) {
		$addResponse(navaid);
	}
	
	public void addResponse(String name, String value) {
	   $addResponse(new TextElement(name, value));
	}

	public Collection getResponse() {
		return _rspData;
	}
}