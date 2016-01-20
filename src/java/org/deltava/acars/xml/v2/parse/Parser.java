// Copyright 2009, 2010, 2011, 2015, 2016 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.parse;

import org.deltava.acars.message.*;

/**
 * A parser for ACARS Protocol v2 messages.
 * @author Luke
 * @version 6.4
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
	@Override
	protected void init() {
		super.init();
		
		// Core parsers
		_eParsers.put(Integer.valueOf(Message.MSG_TOTD), new TakeoffParser());
		_eParsers.put(Integer.valueOf(Message.MSG_MPUPDATE), new MPLocationParser());
		_eParsers.put(Integer.valueOf(Message.MSG_MPINIT), new MPInitParser());
		_eParsers.put(Integer.valueOf(Message.MSG_VOICETOGGLE), new VoiceToggleParser());
		_eParsers.put(Integer.valueOf(Message.MSG_MUTE), new MuteParser());
		_eParsers.put(Integer.valueOf(Message.MSG_SWCHAN), new SwitchChannelParser());
		_eParsers.put(Integer.valueOf(Message.MSG_WARN), new WarnParser());
		_eParsers.put(Integer.valueOf(Message.MSG_WARNRESET), new WarnResetParser());
		_eParsers.put(Integer.valueOf(Message.MSG_POSITION), new PositionParser());
		_eParsers.put(Integer.valueOf(Message.MSG_COMPRESS), new CompressionParser());
		_eParsers.put(Integer.valueOf(Message.MSG_SYSINFO), new SysInfoParser());
		
		// Dispatch parsers
		_dspParsers.put(Integer.valueOf(DispatchMessage.DSP_SCOPEINFO), new ScopeInfoParser());
		_dspParsers.put(Integer.valueOf(DispatchMessage.DSP_ROUTEPLOT), new RoutePlotParser());
	}
}