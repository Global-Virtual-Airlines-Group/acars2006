// Copyright 2020 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.online;

import java.io.*;

/**
 * A network information loader for PilotEdge.
 * @author Luke
 * @version 9.1
 * @since 9.0
 */

import org.apache.log4j.Logger;

import org.deltava.beans.OnlineNetwork;
import org.deltava.beans.servinfo.NetworkInfo;

import org.deltava.dao.file.GetServInfo;
import org.deltava.dao.http.GetURL;

import org.deltava.util.system.SystemData;

public class PilotEdgeLoader extends Loader {
	
	private static final Logger log = Logger.getLogger(PilotEdgeLoader.class);
	
	public PilotEdgeLoader() {
		super(OnlineNetwork.PILOTEDGE, 30);
	}

	@Override
	public void run() {
		super.run();
		try {
			GetURL urldao = new GetURL(SystemData.get("online.pilotedge.status_url"), SystemData.get("online.pilotedge.local.info"));
			urldao.setConnectTimeout(5000);
			urldao.setReadTimeout(15000);
			File f = urldao.download();
			try (InputStream is = new BufferedInputStream(new FileInputStream(f), 32768)) {
				GetServInfo sidao = new GetServInfo(is, _network);
				NetworkInfo in = sidao.getInfo();
				if ((in != null) && in.getValidDate().isAfter(getLastUpdate())) {
					update(in);
					f.setLastModified(in.getValidDate().toEpochMilli());
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}
}