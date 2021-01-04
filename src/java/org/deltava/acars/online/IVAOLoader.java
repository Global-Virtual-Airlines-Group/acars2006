// Copyright 2020 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.online;

import java.io.*;
import java.util.Properties;

import org.apache.log4j.Logger;

import org.deltava.beans.OnlineNetwork;
import org.deltava.beans.servinfo.NetworkInfo;

import org.deltava.dao.file.GetServInfo;
import org.deltava.dao.http.GetURL;

import org.deltava.util.system.SystemData;

/**
 * A network information loader for IVAO.
 * @author Luke
 * @version 9.2
 * @since 9.0
 */

public class IVAOLoader extends Loader {
	
	private static final Logger log = Logger.getLogger(IVAOLoader.class);

	public IVAOLoader() {
		super(OnlineNetwork.IVAO, 60);
	}

	@Override
	public void run() {
		super.run();
		String url = null;
		try {
			GetURL urldao = new GetURL(SystemData.get("online.ivao.status_url"), SystemData.get("online.ivao.local.status"));
			urldao.setConnectTimeout(3500);
			urldao.setReadTimeout(4500);
			Properties p = new Properties();
			File f = urldao.download();
			try (InputStream is = new FileInputStream(f)) {
				p.load(is);
			}

			urldao = new GetURL(p.getProperty("url0"), SystemData.get("online.ivao.local.info"));
			urldao.setConnectTimeout(3500);
			urldao.setReadTimeout(25000);
			f = urldao.download();
			try (InputStream is = new BufferedInputStream(new FileInputStream(f), 65536)) {
				GetServInfo sidao = new GetServInfo(is, _network);
				NetworkInfo in = sidao.getInfo();
				if ((in != null) && in.getValidDate().isAfter(getLastUpdate())) {
					update(in);
					f.setLastModified(in.getValidDate().toEpochMilli());
				}
			}
		} catch (Exception e) {
			boolean isTimeout = e.getCause() instanceof java.net.SocketTimeoutException;
			if (isTimeout)
				log.warn("Timeout loading " + url);
			else
				log.error(e.getMessage(), e);
		}
	}
}