// Copyright 2020, 2021 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.online;

import java.io.*;

import org.deltava.beans.OnlineNetwork;
import org.deltava.beans.servinfo.NetworkInfo;

import org.deltava.dao.DAOException;
import org.deltava.dao.file.GetServInfo;
import org.deltava.dao.http.DAO.Compression;
import org.deltava.dao.http.GetURL;

import org.deltava.util.system.SystemData;

/**
 * A network information loader for PilotEdge.
 * @author Luke
 * @version 10.0
 * @since 9.0
 */

public class PilotEdgeLoader extends Loader {
	
	/**
	 * Creates the Loader.
	 */
	public PilotEdgeLoader() {
		super(OnlineNetwork.PILOTEDGE, 30);
	}

	@Override
	public void execute() throws IOException, DAOException {

		GetURL urldao = new GetURL(SystemData.get("online.pilotedge.status_url"), SystemData.get("online.pilotedge.local.info"));
		urldao.setConnectTimeout(9500);
		urldao.setReadTimeout(17500);
		urldao.setCompression(Compression.GZIP);
		File f = urldao.download();
		try (InputStream is = new BufferedInputStream(new FileInputStream(f), 32768)) {
			GetServInfo sidao = new GetServInfo(is, _network);
			NetworkInfo in = sidao.getInfo();
			if ((in != null) && in.getValidDate().isAfter(getLastUpdate())) {
				update(in);
				f.setLastModified(in.getValidDate().toEpochMilli());
			}
		}
	}
}