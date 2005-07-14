/*
 * Created on Feb 9, 2004
 */
package org.deltava.acars.xml;

import org.jdom.Element;

import org.deltava.acars.message.Message;

/**
 * @author Luke J. Kolin
 */
interface MessageFormatter {

	public abstract int getProtocolVersion();
	public abstract Element format(Message msgBean) throws XMLException;
}
