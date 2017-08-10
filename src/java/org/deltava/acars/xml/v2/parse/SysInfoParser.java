// Copyright 2016 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.parse;

import org.jdom2.Element;

import org.deltava.beans.Pilot;
import org.deltava.beans.Simulator;
import org.deltava.util.StringUtils;

import org.deltava.acars.message.SystemInfoMessage;

import org.deltava.acars.xml.*;

/**
 * A parser for ACARS system information messages.
 * @author Luke
 * @version 6.4
 * @since 6.4
 */

public class SysInfoParser extends XMLElementParser<SystemInfoMessage> {
	
	/**
	 * Convert an XML ack element into an AcknowledgeMessage.
	 * @param e the XML element
	 * @return an AcknowledgeMessage
	 * @throws XMLException if a parse error occurs 
	 */
	@Override
	public SystemInfoMessage parse(Element e, Pilot user) throws XMLException {

		SystemInfoMessage msg = new SystemInfoMessage(user);
		msg.setOSVersion(getChildText(e, "os", "?"));
		msg.setCLRVersion(getChildText(e, "clr", "?"));
		msg.setDotNETVersion(getChildText(e, "dotNET", "?"));
		msg.setIs64Bit(Boolean.valueOf(getChildText(e, "is64", "false")).booleanValue());
		msg.setIsSLI(Boolean.valueOf(getChildText(e, "isSLI", "false")).booleanValue());
		msg.setMemorySize(StringUtils.parse(getChildText(e, "memory", "0"), 0));
		msg.setSockets(StringUtils.parse(getChildText(e, "sockets", "1"), 1));
		msg.setCores(StringUtils.parse(getChildText(e, "cores", "1"), msg.getSockets()));
		msg.setThreads(StringUtils.parse(getChildText(e, "procs", "1"), msg.getCores()));
		msg.setLocale(getChildText(e, "locale", "en-us"));
		msg.setTimeZone(getChildText(e, "tz", "?"));
		msg.setSimulator(Simulator.fromName(getChildText(e, "simulator", ""), Simulator.UNKNOWN));
		msg.setBridgeInfo(getChildText(e, "bridge", null));

		Element ce = e.getChild("cpu");
		msg.setCPU(getChildText(e, "cpu", "?"));
		msg.setCPUSpeed(StringUtils.parse(ce.getAttributeValue("speed", "0"), 0));
		
		Element ge = e.getChild("gpu");
		msg.setGPU(getChildText(e, "gpu", "?"));
		msg.setVideoMemorySize(StringUtils.parse(ge.getAttributeValue("vram"), 16));
		msg.setGPUDriverVersion(ge.getAttributeValue("drv", "?"));
		msg.setColorDepth(StringUtils.parse(ge.getAttributeValue("bpp"), 32));
		msg.setScreenSize(StringUtils.parse(ge.getAttributeValue("x"), 1024), StringUtils.parse(ge.getAttributeValue("y"), 768));
		return msg;
	}
}