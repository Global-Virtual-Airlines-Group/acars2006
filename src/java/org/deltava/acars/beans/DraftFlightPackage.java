// Copyright 2022 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

import org.deltava.beans.flight.DraftFlightReport;
import org.deltava.beans.simbrief.BriefingPackage;

/**
 * A bean to combine Draft Flight Reports with SimBrief briefing packages.
 * @author Luke
 * @version 10.3
 * @since 10.3
 */

public class DraftFlightPackage {

	private final DraftFlightReport _dfr;
	private final BriefingPackage _sbPkg;
	
	/**
	 * Creates the bean.
	 * @param dfr the DraftFlightReport
	 * @param pkg the BriefingPackage
	 */
	public DraftFlightPackage(DraftFlightReport dfr, BriefingPackage pkg) {
		super();
		_dfr = dfr;
		_sbPkg = pkg;
	}
	
	/**
	 * Returns the draft Flight Report.
	 * @return a DraftFlightReport
	 */
	public DraftFlightReport getFlightReport() {
		return _dfr;
	}
	
	/**
	 * Returns the SimBrief briefing package.
	 * @return the BriefingPackage
	 */
	public BriefingPackage getPackage() {
		return _sbPkg;
	}
}