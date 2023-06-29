// Copyright 2005, 2006, 2008, 2016, 2021, 2023 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

import org.deltava.beans.navdata.*;

/**
 * A bean to wrap a navaid with a radio name and heading.
 * @author Luke
 * @version 11.0
 * @since 1.0
 */

public class NavigationRadioBean extends NavigationFrequencyBean {

	private final String _radio;
	private final String _hdg;
	
	/**
	 * Initializes the bean.
	 * @param radioName the name of the aircraft radio to set
	 * @param navaid the navigation data bean
	 * @param hdg the heading to set in degrees
	 */
	public NavigationRadioBean(String radioName, NavigationDataBean navaid, String hdg) {
		super(navaid.getType(), navaid.getLatitude(), navaid.getLongitude());
		setName(navaid.getName());
		setCode(navaid.getCode());
		_hdg = hdg;
		_radio = (radioName != null) ? radioName.toLowerCase() : null;
		if (navaid instanceof NavigationFrequencyBean nfb)
			setFrequency(nfb.getFrequency());
	}

	/**
	 * Returns the radio name.
	 * @return the radio name
	 */
	public String getRadio() {
		return _radio;
	}
	
	/**
	 * Returns the heading.
	 * @return the heading in degrees
	 */
	public String getHeading() {
		return _hdg;
	}
	
	/**
	 * Returns the Goole Maps icon color. <i>NOT IMPLEMENTED</i>
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public String getIconColor() {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Returns the Goole Maps infobox text. <i>NOT IMPLEMENTED</i>
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public String getInfoBox() {
		throw new UnsupportedOperationException();
	}
	/**
	 * Returns the Google Earth palette code.
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public int getPaletteCode() {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Returns the Google Earth icon code.
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public int getIconCode() {
		throw new UnsupportedOperationException();
	}
}