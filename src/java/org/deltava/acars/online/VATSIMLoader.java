// Copyright 2020, 2021, 2022, 2023 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.online;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.deltava.beans.OnlineNetwork;
import org.deltava.beans.servinfo.*;

import org.deltava.dao.file.*;
import org.deltava.dao.http.*;
import org.deltava.dao.DAOException;

import org.deltava.util.system.SystemData;

/**
 * A network information loader for VATSIM.
 * @author Luke
 * @version 11.0
 * @since 9.0
 */

public class VATSIMLoader extends Loader {
	
	private final ExecutorService _tp = Executors.newFixedThreadPool(2);
	
	/**
	 * Creates the Loader.
	 */
	public VATSIMLoader() {
		super(OnlineNetwork.VATSIM, 30);
	}
	
	private class URLLoader implements Runnable {
		private final String _url;
		private final String _file;
		
		URLLoader(String url, String file) {
			super();
			_url = url;
			_file = file;
		}
		
		@Override
		public void run() {
			GetURL urldao = new GetURL(_url, _file);
			try {
				urldao.setConnectTimeout(2500);
				urldao.setReadTimeout(25000);
				urldao.setCompression(Compression.GZIP);
				urldao.download();
			} catch (Exception e) {
				log.error(String.format("Error loading %s using %s - %s", _url, urldao.getCompression(), e.getMessage()));
			}
		}
	}

	@Override
	public void execute() throws IOException, DAOException {
		try {
			CompletableFuture<Void> sif = CompletableFuture.runAsync(new URLLoader(SystemData.get("online.vatsim.status_url"), SystemData.get("online.vatsim.local.info")), _tp);
			CompletableFuture<Void> tcf = CompletableFuture.runAsync(new URLLoader(SystemData.get("online.vatsim.transceiver_url"), SystemData.get("online.vatsim.local.transceiver")), _tp);
			CompletableFuture<Void> ft = CompletableFuture.allOf(sif, tcf);
			ft.get(28000, TimeUnit.MILLISECONDS);
		} catch (ExecutionException ee) {
			log.error("Error downloading VATSIM data - " + ee.getMessage(), ee);
		} catch (TimeoutException | InterruptedException ie) {
			log.warn("Timed out waiting for download");
		}
		
		// Load transceivers
		Collection<RadioPosition> positions = new ArrayList<RadioPosition>();
		File tf = new File(SystemData.get("online.vatsim.local.transceiver")); 
		try (InputStream is = new BufferedInputStream(new FileInputStream(tf), 65536)) {
			GetVATSIMTransceivers tdao = new GetVATSIMTransceivers(is);
			positions.addAll(tdao.load());
		} catch (Exception e) {
			log.error("Error loading radio positions - " + e.getMessage(), e);
		}
		
		// Load servinfo
		File f = new File(SystemData.get("online.vatsim.local.info"));
		try (InputStream is = new BufferedInputStream(new FileInputStream(f), 131072)) {
			GetVATSIMInfo sidao = new GetVATSIMInfo(is);
			NetworkInfo in = sidao.getInfo();
			if ((in != null) && in.getValidDate().isAfter(getLastUpdate())) {
				in.merge(positions);
				update(in);
				f.setLastModified(in.getValidDate().toEpochMilli());
			}
		}
	}
}