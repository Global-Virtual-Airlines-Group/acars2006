package org.deltava.acars.beans;

/**
 * A class to store ACARS Server statistics.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public final class ServerStats implements java.io.Serializable {

	public static final int START_TIME = 0;
	public static final int CONNECT_COUNT = 1;
	public static final int AUTH_COUNT = 2;
	public static final int MSGS_IN = 3;
	public static final int MSGS_OUT = 4;
	public static final int CURRENT_CONNECT = 5;
	public static final int MAX_CONNECT = 6;
	public static final int RAW_MSGS_IN = 7;
	public static final int RAW_MSGS_OUT = 8;

	private static long[] stats = {0, 0, 0, 0, 0, 0, 0, 0, 0};

	// Singleton
	private ServerStats() {
	}
	
	public static synchronized void add(int statType) {
		if ((statType >= 0) && (statType < stats.length))
			stats[statType]++;
	}
	
	public static synchronized void dec(int statType) {
		if ((statType >= 0) && (statType < stats.length) && (stats[statType] > 0))
			stats[statType]--;
	}
	
	public static synchronized void set(int statType, long statValue) {
		if ((statType >= 0) && (statType < stats.length) && (statValue >= 0))
			stats[statType] = statValue; 
	}
	
	public static synchronized long get(int statType) {
		return ((statType >= 0) && (statType < stats.length)) ? stats[statType] : 0;
	}
}