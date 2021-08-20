// Copyright 2021 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.online;

import java.io.*;

import org.deltava.beans.OnlineNetwork;
import org.deltava.beans.servinfo.NetworkInfo;

import org.deltava.dao.DAOException;
import org.deltava.dao.file.GetPOSCONInfo;
import org.deltava.dao.http.GetURL;
import org.deltava.dao.http.DAO.Compression;

import org.deltava.util.system.SystemData;

/**
 * A network information loader for POSCON.
 * @author Luke
 * @version 10.1
 * @since 10.1
 */

public class POSCONLoader extends Loader {

	/**
	 * Creates the Loader.
	 */
	public POSCONLoader() {
		super(OnlineNetwork.POSCON, 120);
	}

	@Override
	protected void execute() throws IOException, DAOException {
		
		GetURL urldao = new GetURL(SystemData.get("online.poscon.status_url"), SystemData.get("online.poscon.local.info"));
		urldao.setConnectTimeout(9500);
		urldao.setReadTimeout(17500);
		urldao.setCompression(Compression.GZIP);
		File f = urldao.download();
		try (InputStream is = new BufferedInputStream(new FileInputStream(f), 32768)) {
			GetPOSCONInfo sidao = new GetPOSCONInfo(is);
			NetworkInfo in = sidao.getInfo();
			if ((in != null) && in.getValidDate().isAfter(getLastUpdate())) {
				update(in);
				f.setLastModified(in.getValidDate().toEpochMilli());
			}
		}
	}
}