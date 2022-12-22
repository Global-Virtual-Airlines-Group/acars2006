// Copyright 2020, 2021, 2022 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.online;

import java.io.*;

import org.deltava.beans.OnlineNetwork;
import org.deltava.beans.servinfo.NetworkInfo;

import org.deltava.dao.DAOException;
import org.deltava.dao.file.GetIVAOInfo;
import org.deltava.dao.http.*;

import org.deltava.util.system.SystemData;

/**
 * A network information loader for IVAO.
 * @author Luke
 * @version 10.3
 * @since 9.0
 */

public class IVAOLoader extends Loader {
	
	/**
	 * Creates the Loader.
	 */
	public IVAOLoader() {
		super(OnlineNetwork.IVAO, 45);
	}

	@Override
	public void execute() throws IOException, DAOException {

		GetURL urldao = new GetURL(SystemData.get("online.ivao.status_url"), SystemData.get("online.ivao.local.info"));
		urldao.setConnectTimeout(4500);
		urldao.setReadTimeout(25000);
		urldao.setCompression(Compression.GZIP);
		File f = urldao.download();
		try (InputStream is = new BufferedInputStream(new FileInputStream(f), 131072)) {
			GetIVAOInfo sidao = new GetIVAOInfo(is);
			NetworkInfo in = sidao.getInfo();
			if ((in != null) && in.getValidDate().isAfter(getLastUpdate())) {
				update(in);
				f.setLastModified(in.getValidDate().toEpochMilli());
			}
		}
	}
}