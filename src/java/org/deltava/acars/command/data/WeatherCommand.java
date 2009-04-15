// Copyright 2009 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.io.*;
import java.net.*;

import org.deltava.beans.wx.*;
import org.deltava.beans.navdata.AirportLocation;

import org.deltava.acars.beans.MessageEnvelope;

import org.deltava.acars.message.*;
import org.deltava.acars.message.data.WXMessage;

import org.deltava.acars.command.*;

import org.deltava.dao.*;
import org.deltava.dao.file.GetNOAAWeather;
import org.deltava.dao.wsdl.GetFAWeather;

import org.deltava.util.cache.*;
import org.deltava.util.ftp.*;
import org.deltava.util.system.SystemData;

/**
 * An ACARS data command to return available weather data.
 * @author Luke
 * @version 2.5
 * @since 2.3
 */

public class WeatherCommand extends DataCommand {
	
	private static final Cache<WeatherDataBean> _cache = new ExpiringCache<WeatherDataBean>(256, 1800);

	/**
	 * Initializes the Command.
	 */
	public WeatherCommand() {
		super(WeatherCommand.class);
	}

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		String code = msg.getFlag("code").toUpperCase();
		String type = msg.getFlag("type").toUpperCase();
		if (type == null)
			type = "metar";
		
		// Create the response
		WXMessage wxMsg = new WXMessage(env.getOwner(), msg.getID());
		wxMsg.setAirport(SystemData.getAirport(code));
		
		// Check the cache
		WeatherDataBean info = _cache.get(type + "$" + code);
		if (info != null) {
			wxMsg.add(info);
			ctx.push(wxMsg, env.getConnectionID());
			return;
		}
		
		// Get the weather source
		boolean isFA = "FA".equals(msg.getFilter()) && SystemData.getBoolean("schedule.flightaware.enabled");
		try {
			// Load the airport location
			GetNavData navdao = new GetNavData(ctx.getConnection());
			AirportLocation ap = navdao.getAirport(code);
			
			if (isFA) {
				GetFAWeather dao = new GetFAWeather();
				dao.setUser(SystemData.get("schedule.flightaware.download.user"));
				dao.setPassword(SystemData.get("schedule.flightaware.download.pwd"));
				WeatherDataBean wx = dao.get(type, code);
				wx.setAirport(ap);
				wxMsg.add(wx);
			} else {
				try {
					URL url = new URL(SystemData.get("weather.url." + type.toLowerCase()));
					if (!"ftp".equalsIgnoreCase(url.getProtocol()))
						throw new DAOException("FTP expected - " + url.toExternalForm());

					// Connect to the FTP site and change directories
					FTPConnection ftpc = new FTPConnection(url.getHost());
					ftpc.connect("anonymous", "golgotha@" + InetAddress.getLocalHost().getHostName());
					ftpc.getClient().chdir(url.getPath());
					
					// Download the file we want
					InputStream is = ftpc.get(code + ".TXT", false);
					GetNOAAWeather dao = new GetNOAAWeather(is);
					WeatherDataBean wx = dao.get(type);
					ftpc.close();
					wx.setAirport(ap);
					wxMsg.add(wx);
				} catch (FTPClientException fe) {
					WeatherDataBean wx = WeatherDataBean.create(type);
					wx.setAirport(ap);
					wx.setData("Weather data not available");
					wxMsg.add(wx);
				} catch (Exception e) {
					throw new DAOException(e);
				}
			}
			
			// Send the respose
			ctx.push(wxMsg, env.getConnectionID());
		} catch (DAOException de) {
			log.error("Error loading weather data - " + de.getMessage(), de);
			AcknowledgeMessage errorMsg = new AcknowledgeMessage(env.getOwner(), msg.getID());
			errorMsg.setEntry("error", "Cannot load " + type + " data for " + code);
			ctx.push(errorMsg, ctx.getACARSConnection().getID());
		} finally {
			ctx.release();
		}
	}
}