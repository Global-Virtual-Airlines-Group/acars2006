// Copyright 2020, 2021 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.online;

import java.io.*;

import org.deltava.beans.OnlineNetwork;
import org.deltava.beans.servinfo.NetworkInfo;

import org.deltava.dao.DAOException;
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
	
	/**
	 * Creates the Loader.
	 */
	public VATSIMLoader() {
		super(OnlineNetwork.VATSIM, 30);
	}

	@Override
	public void execute() throws IOException, DAOException {

		GetURL urldao = new GetURL(SystemData.get("online.vatsim.status_url"), SystemData.get("online.vatsim.local.info"));
		urldao.setConnectTimeout(5000);
		urldao.setReadTimeout(15000);
		File f = urldao.download();
		try (InputStream is = new BufferedInputStream(new FileInputStream(f), 131072)) {
			GetVATSIMInfo sidao = new GetVATSIMInfo(is);
			sidao.setVersion(2);
			NetworkInfo in = sidao.getInfo();
			if ((in != null) && in.getValidDate().isAfter(getLastUpdate())) {
				update(in);
				f.setLastModified(in.getValidDate().toEpochMilli());
			}
		}
	}
}