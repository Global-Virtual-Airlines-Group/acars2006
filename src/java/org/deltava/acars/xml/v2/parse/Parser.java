// Copyright 2009, 2010, 2011, 2015, 2016, 2018, 2019, 2020, 2023 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v2.parse;

import org.deltava.acars.message.*;

/**
 * A parser for ACARS Protocol v2 messages.
 * @author Luke
 * @version 11.1
 * @since 2.7
 */

public class Parser extends org.deltava.acars.xml.v1.parse.Parser {

	/**
	 * Initializes the Parser.
	 */
	public Parser() {
		super(2);
	}

	@Override
	protected void init() {
		super.init();
		
		// Core parsers
		_eParsers.put(MessageType.PIREP, new FlightReportParser());
		_eParsers.put(MessageType.TOTD, new TakeoffParser());
		_eParsers.put(MessageType.VOICETOGGLE, new VoiceToggleParser());
		_eParsers.put(MessageType.MUTE, new MuteParser());
		_eParsers.put(MessageType.SWCHAN, new SwitchChannelParser());
		_eParsers.put(MessageType.WARN, new WarnParser());
		_eParsers.put(MessageType.WARNRESET, new WarnResetParser());
		_eParsers.put(MessageType.POSITION, new PositionParser());
		_eParsers.put(MessageType.COMPRESS, new CompressionParser());
		_eParsers.put(MessageType.SYSINFO, new SysInfoParser());
		_eParsers.put(MessageType.PERFORMANCE, new PerformanceParser());
		_eParsers.put(MessageType.DISCONNECT, new KickParser());
		_eParsers.put(MessageType.PING, new PingParser());
		
		// Dispatch parsers
		_dspParsers.put(DispatchRequest.SCOPEINFO, new ScopeInfoParser());
		_dspParsers.put(DispatchRequest.ROUTEPLOT, new RoutePlotParser());
	}
}