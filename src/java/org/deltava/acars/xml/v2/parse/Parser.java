// Copyright 2009, 2010, 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.parse;

import org.deltava.acars.message.DispatchMessage;
import org.deltava.acars.message.Message;
import org.deltava.acars.message.ViewerMessage;

/**
 * A parser for ACARS Protocol v2 messages.
 * @author Luke
 * @version 4.0
 * @since 2.7
 */

public class Parser extends org.deltava.acars.xml.v1.parse.Parser {

	/**
	 * Initializes the Parser.
	 */
	public Parser() {
		super(2);
	}

	/**
	 * Initializes the parser map.
	 */
	protected void init() {
		super.init();
		
		// Core parsers
		_eParsers.put(Integer.valueOf(Message.MSG_TOTD), new TakeoffParser());
		_eParsers.put(Integer.valueOf(Message.MSG_MPUPDATE), new MPLocationParser());
		_eParsers.put(Integer.valueOf(Message.MSG_MPINIT), new MPInitParser());
		_eParsers.put(Integer.valueOf(Message.MSG_VOICETOGGLE), new VoiceToggleParser());
		_eParsers.put(Integer.valueOf(Message.MSG_MUTE), new MuteParser());
		_eParsers.put(Integer.valueOf(Message.MSG_SWCHAN), new SwitchChannelParser());
		
		// Dispatch parsers
		_dspParsers.put(Integer.valueOf(DispatchMessage.DSP_SCOPEINFO), new ScopeInfoParser());
		_dspParsers.put(Integer.valueOf(DispatchMessage.DSP_ROUTEPLOT), new RoutePlotParser());
		
		// Flight viewer parsers
		_viewParsers.put(Integer.valueOf(ViewerMessage.VIEW_REQ), new ViewerRequestParser());
		_viewParsers.put(Integer.valueOf(ViewerMessage.VIEW_ACCEPT), new ViewerAcceptParser());
		_viewParsers.put(Integer.valueOf(ViewerMessage.VIEW_CANCEL), new ViewerCancelParser());
	}
}