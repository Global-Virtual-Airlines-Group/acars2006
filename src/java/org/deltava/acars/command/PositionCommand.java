// Copyright 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2014, 2016, 2017, 2018, 2019, 2020, 2021, 2023, 2024 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command;

import java.util.concurrent.*;

import org.apache.logging.log4j.*;

import static org.deltava.acars.workers.Worker.*;

import org.deltava.beans.OnlineNetwork;
import org.deltava.beans.acars.*;
import org.deltava.beans.navdata.Airspace;
import org.deltava.beans.servinfo.*;

import org.deltava.acars.beans.*;
import org.deltava.acars.message.*;

import org.deltava.dao.jedis.SetTrack;

import org.deltava.util.*;
import org.deltava.util.system.SystemData;

/**
 * An ACARS server command to process position updates.
 * @author Luke
 * @version 11.3
 * @since 1.0
 */

public class PositionCommand extends PositionCacheCommand {

	private static final Logger log = LogManager.getLogger(PositionCommand.class);
	
	private final int MIN_INTERVAL = SystemData.getInt("acars.position.min", 2000);
	private final int ATC_INTERVAL = SystemData.getInt("acars.position.atc", 2000);
	private final int NOATC_INTERVAL = SystemData.getInt("acars.position.std", 2000);
	
	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {

		// Get the Message and the ACARS Connection
		PositionMessage msg = (PositionMessage) env.getMessage();
		ACARSConnection ac = ctx.getACARSConnection();
		if (ac == null) {
			log.warn("Missing Connection for {}", env.getOwnerID());
			return;
		} else if (ac.getClientType() != ClientType.PILOT) {
			log.warn("ATC/Dispatch Client sending Position Report!");
			return;
		}

		// Create the ack message and envelope
		AcknowledgeMessage ackMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());

		// Get the last position report and its age
		InfoMessage info = ac.getFlightInfo();
		PositionMessage oldPM = ac.getPosition();
		if (info == null) {
			log.warn("No Flight Information for {}", ac.getUserID());
			ackMsg.setEntry("sendInfo", "true");
			ctx.push(ackMsg);
			return;
		}
		
		// Set flight ID
		msg.setFlightID(info.getFlightID());
		msg.setEngineCount(info.getEngineCount());
		
		// Adjust the message date and calculate the age of the last message
		// Check if it's really a flood or whether the previous message was just stuck in transit
		msg.setDate(msg.getDate().plusMillis(ac.getTimeOffset()));
		long pmAge = (oldPM == null) ? Long.MAX_VALUE : TimeUnit.MILLISECONDS.convert(System.nanoTime() - oldPM.getTime(), TimeUnit.NANOSECONDS);
		if ((pmAge < MIN_INTERVAL) && (oldPM != null))
			pmAge = msg.getDate().toEpochMilli() - oldPM.getDate().toEpochMilli();

		// Check for frequency with missing controller
		OnlineNetwork network = info.getNetwork();
		boolean noATC1 = ((network != null) && (msg.getATC1() == null) && !StringUtils.isEmpty(msg.getCOM1()));
		boolean noATC2 = ((network != null) && (msg.getATC2() == null) && !StringUtils.isEmpty(msg.getCOM2()));
		
		// If we're online, have a frequency and no controller, find one
		if (msg.isLogged() && (noATC1 || noATC2)) {
			NetworkInfo netInfo = ServInfoHelper.getInfo(network);
			if (noATC1) {
				Controller ctr = netInfo.getControllerByFrequency(msg.getCOM1(), msg);
				if ((ctr != null) && (ctr.getFacility() != Facility.ATIS) && !ctr.isObserver() && ctr.hasFrequency()) {
					int distance = msg.distanceTo(ctr);
					if (distance < (ctr.getRange() * 2)) {
						msg.setATC1(ctr);
						log.warn("No ATC1 set from {}, found {}, distance = {}", ac.getUserID(), ctr.getCallsign(), Integer.valueOf(distance));
						ackMsg.setEntry("reqATC", "true");
					}
				}
			}
			
			if (noATC2) {
				Controller ctr = netInfo.getControllerByFrequency(msg.getCOM2(), msg);
				if ((ctr != null) && (ctr.getFacility() != Facility.ATIS) && !ctr.isObserver() && ctr.hasFrequency()) {
					int distance = msg.distanceTo(ctr);
					if (distance < (ctr.getRange() * 2)) {
						msg.setATC2(ctr);
						log.warn("No ATC2 set from {}, found {}, distance = {}", ac.getUserID(), ctr.getCallsign(), Integer.valueOf(distance));
						ackMsg.setEntry("reqATC", "true");
					}
				}
			}
		}
		
		// Check what country we are in
		lookup(msg);
		if (msg.getCountry() == null) {
			msg.setCountry((oldPM == null) ? info.getAirportD().getCountry() : oldPM.getCountry());
			GEO_INPUT.add(env);
		}
		
		// Clear temporary track if being saved
		if (msg.isLogged()) {
			SetTrack tkdao = new SetTrack();
			tkdao.clear(true, String.valueOf(info.getFlightID()));
		}

		// Queue it up
		if (msg.isReplay() && msg.isLogged())
			queue(msg);
		else if (!msg.isReplay() && !msg.isLogged() && (pmAge < MIN_INTERVAL)) {
			log.warn("Position flood from {} ({}), interval={}ms", ac.getUser().getName(), ac.getUserID(), Long.valueOf(pmAge));
			return;
		} else {
			boolean isPaused = msg.isFlagSet(ACARSFlags.PAUSED);
			ac.setPosition(msg);
			if (msg.isLogged() && !isPaused)
				queue(msg);
			else if (!isPaused)
				queue(new TrackUpdate(true, String.valueOf(info.getFlightID()), msg));
		}
		
		// Log message received
		ctx.push(ackMsg);
		log.debug("Received position from {}", ac.getUserID());
		
		// Send it to any dispatchers that are around
		if (!msg.isReplay()) {
			if ((ac.getUpdateInterval() < NOATC_INTERVAL) && (ac.getProtocolVersion() > 1)) {
				ac.setUpdateInterval(NOATC_INTERVAL);
				ctx.push(new UpdateIntervalMessage(ac.getUser(), NOATC_INTERVAL));
				log.info("Update interval for {} set to {}ms", ac.getUserID(), Integer.valueOf(ATC_INTERVAL));
			}
			
			// Check for prohibited airspace
			Airspace a = Airspace.isRestricted(msg);
			if (a != null) {
				log.info("{} in airspace {}", env.getOwnerID(), a.getID());
				msg.setAirspaceType(a.getType());
				if (!msg.isLogged())
					queue(msg);

				if ((oldPM == null) || !oldPM.getAirspaceType().isRestricted()) {
					msg.setAirspaceType(a.getType());
					log.warn("{} breached restricted airspace {}", env.getOwnerID(), a.getID());
					SystemTextMessage sysMsg = new SystemTextMessage();
					sysMsg.addMessage(String.format("Entry into restricted airspace %s", a.getID()));
					sysMsg.setWarning(true);
					ctx.push(sysMsg, env.getConnectionID(), true);
				}
			} else if ((oldPM != null) && (oldPM.getAirspaceType().isRestricted())) {
				SystemTextMessage sysMsg = new SystemTextMessage();
				sysMsg.addMessage("Exit from restricted airspace");
				ctx.push(sysMsg);
			}
		}

		// Check if the caches need to be flushed
		try {
			flush(false, ctx);
		} catch (Exception e) {
			log.atError().withThrowable(e).log("Error flushing positions - {}", e.getMessage());
		}
	}
}