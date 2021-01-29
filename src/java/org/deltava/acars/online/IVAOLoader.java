// Copyright 2020, 2021 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.online;

import java.io.*;
import java.util.Properties;

import org.deltava.beans.OnlineNetwork;
import org.deltava.beans.servinfo.NetworkInfo;

import org.deltava.dao.DAOException;
import org.deltava.dao.file.GetServInfo;
import org.deltava.dao.http.GetURL;

import org.deltava.util.system.SystemData;

/**
 * A network information loader for IVAO.
 * @author Luke
 * @version 9.1
 * @since 9.0
 */

public class IVAOLoader extends Loader {
	
	/**
	 * Creates the Loader.
	 */
	public IVAOLoader() {
		super(OnlineNetwork.IVAO, 60);
	}

	@Override
	public void execute() throws IOException, DAOException {

		GetURL urldao = new GetURL(SystemData.get("online.ivao.status_url"), SystemData.get("online.ivao.local.status"));
		urldao.setConnectTimeout(3500);
		urldao.setReadTimeout(4500);
		Properties p = new Properties();
		try (InputStream is = new FileInputStream(urldao.download())) {
			p.load(is);
		}

		urldao = new GetURL(p.getProperty("url0"), SystemData.get("online.ivao.local.info"));
		urldao.setConnectTimeout(4500);
		urldao.setReadTimeout(25000);
		File f = urldao.download();
		try (InputStream is = new BufferedInputStream(new FileInputStream(f), 65536)) {
			GetServInfo sidao = new GetServInfo(is, _network);
			NetworkInfo in = sidao.getInfo();
			if ((in != null) && in.getValidDate().isAfter(getLastUpdate())) {
				update(in);
				f.setLastModified(in.getValidDate().toEpochMilli());
			}
		}
	}
}