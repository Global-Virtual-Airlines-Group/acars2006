// Copyright 2019, 2021, 2022 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.parse;

import org.jdom2.Element;

import org.deltava.acars.message.PerformanceMessage;

import org.deltava.beans.Pilot;
import org.deltava.beans.acars.*;

import org.deltava.util.StringUtils;
import org.deltava.acars.xml.*;

/**
 * A parser for ACARS client performance counter messages.
 * @author Luke
 * @version 10.2
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
		
		Element pe = e.getChild("perfctrs");
		if (pe != null) {
			for (Element ce : pe.getChildren()) {
				int value = StringUtils.parse(ce.getTextTrim(), 0);
				if (value != 0)
					msg.addCounter(ce.getName(), value);
			}
		}
		
		Element fre = e.getChild("framerates");
		if (fre != null) {
			FrameRates fr = new FrameRates();
			fr.setSize(StringUtils.parse(getChildText(fre, "size", "0"), 0));
			fr.setMin(StringUtils.parse(getChildText(fre, "min", "0"), 0));
			fr.setMax(StringUtils.parse(getChildText(fre, "max", "0"), 0));
			fr.setAverage(StringUtils.parse(getChildText(fre, "avg", "0.0"), 0d));
			fr.setPercentile(1, StringUtils.parse(getChildText(fre, "p1", "0"), 0));
			fr.setPercentile(5, StringUtils.parse(getChildText(fre, "p5", "0"), 0));
			fr.setPercentile(50, StringUtils.parse(getChildText(fre, "p50", "0"), 0));
			fr.setPercentile(95, StringUtils.parse(getChildText(fre, "p95", "0"), 0));
			fr.setPercentile(99, StringUtils.parse(getChildText(fre, "p99", "0"), 0));
			msg.setFrames(fr);
		}
		
		return msg;
	}
}