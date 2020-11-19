// Copyright 2004, 2005, 2006, 2007, 2008, 2009, 2016, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.xml.v1.parse;

import org.deltava.acars.message.*;
import org.deltava.acars.xml.*;

/**
 * A parser for ACARS Protocol v1 messages.
 * @author Luke
 * @version 8.4
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
	
	@Override
	protected void init() {
		_eParsers.put(MessageType.ACK, new AckParser());
		_eParsers.put(MessageType.AUTH, new AuthParser());
		_eParsers.put(MessageType.DATAREQ, new DataRequestParser());
		_eParsers.put(MessageType.DIAG, new DiagnosticParser());
		_eParsers.put(MessageType.INFO, new FlightInfoParser());
		_eParsers.put(MessageType.PIREP, new FlightReportParser());
		_eParsers.put(MessageType.POSITION, new PositionParser());
		_eParsers.put(MessageType.TEXT, new TextMessageParser());
		_eParsers.put(MessageType.PING, new PingParser());
		_dspParsers.put(DispatchRequest.SVCREQ, new DispatchRequestParser());
		_dspParsers.put(DispatchRequest.CANCEL, new DispatchCancelParser());
		_dspParsers.put(DispatchRequest.ACCEPT, new DispatchAcceptParser());
		_dspParsers.put(DispatchRequest.INFO, new DispatchInfoParser());
		_dspParsers.put(DispatchRequest.ROUTEREQ, new DispatchRouteParser());
		_dspParsers.put(DispatchRequest.COMPLETE, new DispatchCompletionParser());
		_dspParsers.put(DispatchRequest.PROGRESS, new ProgressParser());
		_dspParsers.put(DispatchRequest.RANGE, new DispatchRangeParser());
	}
}