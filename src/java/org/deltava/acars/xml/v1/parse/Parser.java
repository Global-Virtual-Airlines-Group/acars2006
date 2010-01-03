// Copyright 2004, 2005, 2006, 2007, 2008, 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.parse;

import org.deltava.acars.message.*;
import org.deltava.acars.xml.*;

/**
 * A parser for ACARS Protocol v1 messages.
 * @author Luke
 * @version 2.8
 * @since 1.0
 */

public class Parser extends XMLMessageParser {

	/**
	 * Initializes the Parser.
	 */
	public Parser() {
		super(1);
	}
	
	/**
	 * Initializes the Parser. This is used by Parsers for other protocol versions that subclass this clas.
	 * @param version the protocol version number
	 */
	protected Parser(int version) {
		super(version);
	}
	
	/**
	 * Initializes the parser map.
	 */
	protected void init() {
		_eParsers.put(Integer.valueOf(Message.MSG_ACK), new AckParser());
		_eParsers.put(Integer.valueOf(Message.MSG_AUTH), new AuthParser());
		_eParsers.put(Integer.valueOf(Message.MSG_DATAREQ), new DataRequestParser());
		_eParsers.put(Integer.valueOf(Message.MSG_DIAG), new DiagnosticParser());
		_eParsers.put(Integer.valueOf(Message.MSG_INFO), new FlightInfoParser());
		_eParsers.put(Integer.valueOf(Message.MSG_PIREP), new FlightReportParser());
		_eParsers.put(Integer.valueOf(Message.MSG_POSITION), new PositionParser());
		_eParsers.put(Integer.valueOf(Message.MSG_TEXT), new TextMessageParser());
		_dspParsers.put(Integer.valueOf(DispatchMessage.DSP_SVCREQ), new DispatchRequestParser());
		_dspParsers.put(Integer.valueOf(DispatchMessage.DSP_CANCEL), new DispatchCancelParser());
		_dspParsers.put(Integer.valueOf(DispatchMessage.DSP_ACCEPT), new DispatchAcceptParser());
		_dspParsers.put(Integer.valueOf(DispatchMessage.DSP_INFO), new DispatchInfoParser());
		_dspParsers.put(Integer.valueOf(DispatchMessage.DSP_ROUTEREQ), new DispatchRouteParser());
		_dspParsers.put(Integer.valueOf(DispatchMessage.DSP_COMPLETE), new DispatchCompletionParser());
		_dspParsers.put(Integer.valueOf(DispatchMessage.DSP_PROGRESS), new ProgressParser());
		_dspParsers.put(Integer.valueOf(DispatchMessage.DSP_RANGE), new DispatchRangeParser());
	}
}