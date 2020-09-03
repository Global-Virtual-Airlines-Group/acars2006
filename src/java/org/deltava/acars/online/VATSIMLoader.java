// Copyright 2020 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.online;

import java.io.*;

import org.apache.log4j.Logger;

import org.deltava.beans.OnlineNetwork;
import org.deltava.beans.servinfo.NetworkInfo;

import org.deltava.dao.file.GetVATSIMInfo;
import org.deltava.dao.http.GetURL;

import org.deltava.util.system.SystemData;

/**
 * A network information loader for VATSIM.
 * @author Luke
 * @version 9.1
 * @since 9.0
 */

public class VATSIMLoader extends Loader {
	
	private static final Logger log = Logger.getLogger(VATSIMLoader.class);

	public VATSIMLoader() {
		super(OnlineNetwork.VATSIM, 30);
	}

	@Override
	public void run() {
		super.run();
		try {
			GetURL urldao = new GetURL(SystemData.get("online.vatsim.status_url"), SystemData.get("online.vatsim.local.info"));
			urldao.setConnectTimeout(5000);
			urldao.setReadTimeout(15000);
			File f = urldao.download();
			try (InputStream is = new BufferedInputStream(new FileInputStream(f), 131072)) {
				GetVATSIMInfo sidao = new GetVATSIMInfo(is);
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