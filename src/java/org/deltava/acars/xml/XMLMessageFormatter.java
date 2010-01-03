// Copyright 2004, 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml;

import java.util.*;

import org.deltava.acars.message.Message;

/**
 * An ACARS message formatter that generates XML messages. 
 * @author Luke
 * @version 2.8
 * @since 2.8
 */

public abstract class XMLMessageFormatter extends MessageFormatter {
	
	protected final Map<Class<? extends Message>, XMLElementFormatter> _eFormatters = 
		new HashMap<Class<? extends Message>, XMLElementFormatter>();

	/**
	 * Initializes the formatter.
	 * @param version the protocol version
	 */
	public XMLMessageFormatter(int version) {
		super(version);
	}
}