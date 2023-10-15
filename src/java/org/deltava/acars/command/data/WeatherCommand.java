// Copyright 2009, 2012, 2017, 2019, 2020, 2023 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.sql.Connection;

import org.deltava.beans.wx.*;
import org.deltava.beans.UserData;
import org.deltava.beans.navdata.AirportLocation;
import org.deltava.beans.system.*;

import org.deltava.acars.beans.MessageEnvelope;

import org.deltava.acars.message.*;
import org.deltava.acars.message.data.WXMessage;

import org.deltava.acars.command.*;

import org.deltava.dao.*;
import org.deltava.dao.http.GetFAWeather;

import org.deltava.util.system.SystemData;

/**
 * An ACARS data command to return available weather data.
 * @author Luke
 * @version 11.1
 * @since 2.3
 */

public class WeatherCommand extends DataCommand {
	
	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		String code = msg.getFlag("code").toUpperCase();
		WeatherDataBean.Type wt = WeatherDataBean.Type.METAR;
		try {
			String type = msg.getFlag("type").toUpperCase();
			wt = WeatherDataBean.Type.valueOf(type);
		} catch (IllegalArgumentException iae) {
			log.error("Unknown weather data type - {}", msg.getFlag("type"));
			return;
		}
		
		// Create the response
		WXMessage wxMsg = new WXMessage(env.getOwner(), msg.getID());
		wxMsg.setAirport(SystemData.getAirport(code));
		
		// Get the weather source
		boolean isFA = "FA".equals(msg.getFilter()) && SystemData.getBoolean("schedule.flightaware.enabled");
		try {
			Connection c = ctx.getConnection();
			
			// Load the airport location
			GetNavData navdao = new GetNavData(c);
			AirportLocation ap = navdao.getAirport(code);
			
			if (isFA) {
				UserData usrLoc = ctx.getACARSConnection().getUserData();
				GetFAWeather dao = new GetFAWeather();
				dao.setUser(SystemData.get("schedule.flightaware.flightXML.user"));
				dao.setPassword(SystemData.get("schedule.flightaware.flightXML.v3"));
				dao.setReadTimeout(5000);
				APILogger.add(new APIRequest(API.FlightAware.createName("WEATHER"), usrLoc.getDB(), (ctx.getUser() == null), false));
				WeatherDataBean wx = dao.get(wt, ap);
				wxMsg.add(wx);
			} else {
				// Download the file we want
				GetWeather dao = new GetWeather(c);
				WeatherDataBean wx = dao.get(wt, code);
				if (wx == null) {
					wx = WeatherDataBean.create(wt);
					wx.setData("Weather data not available");
				}
					
				wxMsg.add(wx);
			}
			
			ctx.push(wxMsg);
		} catch (DAOException de) {
			log.atError().withThrowable(de).log("Error loading weather data - {}", de.getMessage());
			ctx.push(new AcknowledgeMessage(env.getOwner(), msg.getID(), "Cannot load " + wt + " data for " + code + " - " + de.getMessage()));
		} finally {
			ctx.release();
		}
	}
}