// Copyright 2019 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

import org.deltava.beans.*;

/**
 * A utility class to score ACARS content warnings.
 * @author Luke
 * @version 8.7
 * @since 8.7
 */

public class WarningScorer {
	
	private static final int DEFAULT_SCORE = 2;

	// static class
	private WarningScorer() {
		super();
	}
	
	/**
	 * Scores a content warning.
	 * @param author the Pilot issuing the warning
	 * @return a warning score
	 */
	public static int score(Pilot author) {
		int score = DEFAULT_SCORE;
		if (author.isInRole("HR") || author.isInRole("Operations"))
			score += 7;
		else if ((author.getRank() == Rank.ACP) || (author.getRank() == Rank.CP))
			score += 5;
		else if ((author.getRank() == Rank.SC) || author.isInRole("Dispatch"))
			score += 2;
		
		return score;
	}
}