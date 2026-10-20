// Copyright 2026 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.workers;

import java.sql.Connection;
import java.util.concurrent.*;

import org.deltava.acars.beans.MessageEnvelope;
import org.deltava.acars.message.SummaryMessage;

import org.deltava.acars.pool.VirtualNamedThreadFactory;

import org.deltava.beans.flight.*;
import org.deltava.beans.system.Prompt;

import org.deltava.dao.*;
import org.deltava.dao.acars.SetSummaryQueue;
import org.deltava.dao.http.GetAIAnalysis;

import org.deltava.util.system.SystemData;

import org.gvagroup.ipc.WorkerState;
import org.gvagroup.pool.ConnectionPool;

/**
 * An ACARS Server worker to generate AI flight summaries.
 * @author Luke
 * @version 13.0
 * @since 12.5
 */

public class FlightSummarizer extends Worker {
	
	private ConnectionPool<Connection> _jdbcPool;

	private class SummaryWorker implements Runnable {
		private final ScorePackage _pkg;
		private final Prompt _p;
		private final int _id;
		
		SummaryWorker(ScorePackage pkg, Prompt p) {
			super();
			_p = p;
			_pkg = pkg;
			_id = _pkg.getFlightReport().getDatabaseID(DatabaseID.ACARS);
		}
		
		@Override
		public String toString() {
			return String.format("Summarizer-%d", Integer.valueOf(_id));
		}
		
		@Override
		public void run() {
			String k = SystemData.get(String.format("security.key.claude.%s", _pkg.getPilot().getAirlineCode().toLowerCase()));
			try {
				GetAIAnalysis sdao = new GetAIAnalysis(_pkg);
				sdao.setConnectTimeout(3500);
				sdao.setReadTimeout(45000);
				sdao.setKey(k);
				sdao.setPrompt(_p);
				FlightAnalysis fa = sdao.execute();
				_pkg.getFlightReport().addStatusUpdate(0, HistoryType.SYSTEM, "Generated AI Flight Summary");
				log.info("Summary for {} Flight Report {} generated in {}ms", _pkg.getPilot().getAirlineCode(), Integer.valueOf(_pkg.getID()), Long.valueOf(fa.getGenerationTime()));
				
				// Write to database
				try (Connection c = _jdbcPool.getConnection()) {
					c.setAutoCommit(false);
					SetFlightReport frwdao = new SetFlightReport(c);
					SetSummaryQueue qwdao = new SetSummaryQueue(c);
					frwdao.writeAnalysis(fa, _pkg.getPilot().getAirlineCode());
					frwdao.writeHistory(_pkg.getFlightReport().getStatusUpdates(), _pkg.getPilot().getAirlineCode());
					qwdao.clear(_id);
					c.commit();
				}
			} catch (Exception e) {
				log.atError().withThrowable(e).log("Error summarizing {} - {}", Integer.valueOf(_pkg.getID()), e.getMessage());
			} finally {
				log.info("{} Complete", toString());
			}
		}
	}
	
	/**
	 * Creates the Worker.
	 */
	public FlightSummarizer() {
		super("Flight Summarizer", 85, FlightSummarizer.class);
	}
	
	@Override
	public void open() {
		super.open();
		_jdbcPool = SystemData.getJDBCPool();
	}

	@Override
	public void run() {
		log.info("Started");
		_status.setStatus(WorkerState.RUNNING);
		try (ExecutorService pool = Executors.newThreadPerTaskExecutor(new VirtualNamedThreadFactory())) {
			while (!Thread.currentThread().isInterrupted()) {
				_status.setMessage("Idle");
				try {
					MessageEnvelope env = MSG_OUTPUT.poll(30, TimeUnit.SECONDS);
					if (env != null) {
						SummaryMessage msg = (SummaryMessage) env.getMessage();
						
						// Get the prompt
						try (Connection c = _jdbcPool.getConnection()) {
							GetSystemData pdao = new GetSystemData(c);
							Prompt p = pdao.getPrompt("FLIGHTSUMMARY", env.getOwner().getAirlineCode());

							// Submit the job
							SummaryWorker w = new SummaryWorker(msg.getPackage(), p);
							pool.submit(w);
						}
					}
				
					_status.complete();
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
				} catch (Exception e) {
					log.atError().withThrowable(e).log(e.getMessage());
				}
			}

			try {
				pool.shutdown();
				pool.awaitTermination(25, TimeUnit.SECONDS);
			} catch (InterruptedException ie) {
				int tasks = pool.shutdownNow().size();
				log.warn("Shutdown timeout - {} tasks running", Integer.valueOf(tasks));
			}
		}
	}
}