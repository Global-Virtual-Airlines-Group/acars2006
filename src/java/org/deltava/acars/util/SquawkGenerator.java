// Copyright 2016, 2017 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.util;

import java.util.*;
import java.time.Instant;

import org.deltava.beans.*;
import org.deltava.acars.beans.TXCode;

/**
 * A class to generate random Transponder codes.
 * @author Luke
 * @version 7.2
 * @since 7.2
 */

public class SquawkGenerator {
	
	@SuppressWarnings("boxing")
	private static final Collection<Integer> DIGITS = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7);
	private static final Random _RND = new Random();
	
	// singleton
	private SquawkGenerator() {
		super();
	}
	
	private static int generateDigit(List<Integer> choices, int position) {
		int pos = _RND.nextInt(choices.size());
		int digit = (int) (choices.get(pos).intValue() * Math.pow(10, (position - 4)));
		choices.remove(pos);
		return digit;
	}
	
	/**
	 * Generates a random squawk code.
	 * @param loc the aircraft's current location.
	 * @return a squawk code
	 */
	public static synchronized TXCode generate(GeoLocation loc) {
		
		// Calculate first digit
		List<Integer> digits = new ArrayList<Integer>(DIGITS);
		int firstDigit = 3;
		if (loc != null) {
			if ((loc.getLatitude() < 30) && (loc.getLongitude() < -20))
				firstDigit = 5;
			else if ((loc.getLongitude() >= -20) && (loc.getLongitude() < 15) && (loc.getLatitude() < 36))
				firstDigit = 6;
			else if ((loc.getLongitude() >= -20) && (loc.getLongitude() < 40))
				firstDigit = 4;
			else {
				int pos = _RND.nextInt(4) + 2;
				firstDigit = digits.get(pos).intValue();
			}
		}
		
		// Generate the code
		digits.remove(Integer.valueOf(firstDigit));
		int txCode = (firstDigit * 1000) + (generateDigit(digits, 2) + generateDigit(digits, 3) + generateDigit(digits, 4));
		TXCode tx = new TXCode(txCode);
		tx.setAssignedOn(Instant.now());
		return tx;
	}
}