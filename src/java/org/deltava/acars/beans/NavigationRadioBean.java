// Copyright (c) 2005 Luke J. Kolin. All Rights Reserved.
package org.deltava.acars.beans;

import org.deltava.beans.navdata.NavigationDataBean;

/**
 * A bean to wrap a navaid with a radio name and heading.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class NavigationRadioBean {

	private NavigationDataBean _navaid;
	private String _radio;
	private String _hdg;
	
	public NavigationRadioBean(String radioName, NavigationDataBean navaid, String hdg) {
		super();
		_radio = radioName.toLowerCase();
		_navaid = navaid;
		_hdg = hdg;
	}
	
	public String getRadio() {
		return _radio;
	}
	
	public String getHeading() {
		return _hdg;
	}
	
	public NavigationDataBean getNavaid() {
		return _navaid;
	}
}