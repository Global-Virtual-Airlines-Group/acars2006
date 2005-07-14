/*
 * Created on Feb 6, 2004
 */
package org.deltava.acars.xml;

import org.deltava.beans.Pilot;
import org.deltava.acars.message.Message;

import org.jdom.Element;

/**
 * @author Luke J. Kolin
 */
interface MessageParser {

	public abstract int getProtocolVersion();
	public abstract Message parse(int msgType) throws XMLException;
	public abstract void setElement(Element e);
	public abstract void setUser(Pilot userInfo);
	public abstract void setTime(long ts);
}