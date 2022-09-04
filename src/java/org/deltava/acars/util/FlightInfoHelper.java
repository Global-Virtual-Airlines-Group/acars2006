// Copyright 2021, 2022 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.util;

import org.deltava.acars.message.InfoMessage;
import org.deltava.beans.acars.FlightInfo;

/**
 * A utility class to convert FlightInfo messages to Flight Information beans.
 * @author Luke
 * @version 10.3
 * @since 10.0
 */

public class FlightInfoHelper {

	// static class
	private FlightInfoHelper() {
		super();
	}
	
	/**
	 * Converts an InfoMessage to a FlightInfo bean.
	 * @param msg an InfoMessage
	 * @return a FlightInfo bean
	 */
	public static FlightInfo convert(InfoMessage msg) {

		FlightInfo inf = new FlightInfo(msg.getFlightID());
		inf.setAirportD(msg.getAirportD());
		inf.setAirportA(msg.getAirportA());
		inf.setAirportL(msg.getAirportL());
		inf.setFlightCode(msg.getFlightCode());
		inf.setScheduleValidated(msg.isScheduleValidated());
		inf.setAltitude(msg.getAltitude());
		inf.setEquipmentType(msg.getEquipmentType());
		inf.setStartTime(msg.getStartTime());
		inf.setEndTime(msg.getEndTime());
		inf.setLoadType(msg.getLoadType());
		inf.setLoadFactor(msg.getLoadFactor());
		inf.setAutopilotType(msg.getAutopilotType());
		inf.setDispatcherID(msg.getDispatcherID());
		inf.setDispatchLogID(msg.getDispatchLogID());
		inf.setDispatcher(msg.getDispatcher());
		inf.setIsACARS64Bit(msg.getIsACARS64Bit());
		inf.setIsSim64Bit(msg.getIsSim64Bit());
		inf.setPassengers(msg.getPassengers());
		inf.setSeats(msg.getSeats());
		inf.setSimulator(msg.getSimulator());
		return inf;
	}
}