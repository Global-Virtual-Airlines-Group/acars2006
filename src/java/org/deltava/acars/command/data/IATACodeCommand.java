// Copyright 2013, 2014, 2019, 2020, 2021 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.command.data;

import java.util.*;
import java.sql.Connection;
import java.util.stream.Collectors;

import org.deltava.dao.*;
import org.deltava.dao.acars.GetACARSIATACodes;

import org.deltava.beans.UserData;
import org.deltava.beans.acars.IATACodes;
import org.deltava.beans.schedule.Aircraft;
import org.deltava.acars.beans.MessageEnvelope;

import org.deltava.acars.command.*;

import org.deltava.acars.message.DataRequestMessage;
import org.deltava.acars.message.data.IATACodeMessage;

/**
 * An ACARS Command to return a list of IATA aircraft codes.
 * @author Luke
 * @version 9.1
 * @since 5.1
 */

public class IATACodeCommand extends DataCommand {

	/**
	 * Executes the command.
	 * @param ctx the Command context
	 * @param env the message Envelope
	 */
	@Override
	public void execute(CommandContext ctx, MessageEnvelope env) {
		
		// Get the message
		DataRequestMessage msg = (DataRequestMessage) env.getMessage();
		IATACodeMessage rspMsg = new IATACodeMessage(env.getOwner(), msg.getID());
		UserData ud = ctx.getACARSConnection().getUserData();

		try {
			Connection con = ctx.getConnection();
			GetACARSIATACodes dao = new GetACARSIATACodes(con);
			Map<String, IATACodes> codes = dao.getAll(ud.getDB());
			
			// Add existing codes
			GetAircraft acdao = new GetAircraft(con);
			Collection<Aircraft> allAC = acdao.getAll().stream().filter(a -> a.isUsed(ud.getAirlineCode())).collect(Collectors.toList());
			for (Aircraft ac : allAC) {
				IATACodes c = codes.get(ac.getName());
				if (c == null) {
					c = new IATACodes(ac.getName());
					codes.put(ac.getName(), c);
				}
				
				final IATACodes cc = c;
				ac.getIATA().forEach(iata -> cc.putIfAbsent(iata, Integer.valueOf(1)));
			}
			
			rspMsg.setMaxAge(5500);
			rspMsg.addAll(codes.values());
			ctx.push(rspMsg);
		} catch (DAOException de) {
			log.error("Error loading FDE codes - " + de.getMessage(), de);
		} finally {
			ctx.release();
		}
	}
}