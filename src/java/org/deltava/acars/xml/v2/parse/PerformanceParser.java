// Copyright 2019, 2021 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.parse;

import org.jdom2.Element;

import org.deltava.acars.message.PerformanceMessage;

import org.deltava.beans.Pilot;
import org.deltava.beans.acars.TaskTimerData;

import org.deltava.util.StringUtils;

import org.deltava.acars.xml.*;

/**
 * A parser for ACARS client performance counter messages.
 * @author Luke
 * @version 10.0
 * @since 8.6
 */

public class PerformanceParser extends XMLElementParser<PerformanceMessage> {
	
	/**
	 * Convert an XML performance element into a PerformanceMessage.
	 * @param e the XML element
	 * @return a PerformanceMessage
	 * @throws XMLException if a parse error occurs 
	 */
	@Override
	public PerformanceMessage parse(Element e, Pilot p) throws XMLException {
	
		PerformanceMessage msg = new PerformanceMessage(p);
		msg.setFlightID(StringUtils.parse(getChildText(e, "id", "0"), 0));
		int tickSize = StringUtils.parse(getChildText(e, "tickSize", "10000"), 10000);
		for (Element te : e.getChildren("timer")) {
			TaskTimerData ttd = new TaskTimerData(te.getChildTextTrim("name"), tickSize);
			ttd.setMax(StringUtils.parse(getChildText(te, "max", "0"), 0));
			ttd.setMin(StringUtils.parse(getChildText(te, "min", "0"), 0));
			ttd.setCount(StringUtils.parse(getChildText(te, "count", "0"), 0, false));
			ttd.setTotal(StringUtils.parse(getChildText(te, "total", "0"), 0, false));
			ttd.setStdDev(StringUtils.parse(getChildText(te, "stdDev", "0"), 0.0));
			msg.addTimerData(ttd);
		}
		
		return msg;
	}
}