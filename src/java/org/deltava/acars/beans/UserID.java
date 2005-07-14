// Copyright (c) 2005 Luke J. Kolin. All Rights Reserved.
package org.deltava.acars.beans;

import java.text.DecimalFormat;

/**
 * A bean to parse user IDs.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public class UserID {
	
	private static final DecimalFormat _df = new DecimalFormat("##000");
	
	private String _airline;
	private int _id;

	/**
	 * 
	 */
	public UserID(CharSequence code) {
		super();
		
        if (code == null)
            return;

        StringBuffer pBuf = new StringBuffer();
        StringBuffer cBuf = new StringBuffer();
        for (int x = 0; x < code.length(); x++) {
            char c = Character.toUpperCase(code.charAt(x));
            if ("0123456789".indexOf(c) != -1) {
                cBuf.append(c);
            } else if ("ABCDEFGHIJKLMNOPQRSTUVWXYZ".indexOf(c) != -1) {
                pBuf.append(c);
            }
        }

        // Save the prefix and the code
        _airline = pBuf.toString();
        try {
            _id = Integer.parseInt(cBuf.toString());
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("Invalid Pilot Code - " + code);
        }
	}

	public String getAirlineCode() {
		return _airline;
	}
	
	public int getUserID() {
		return _id;
	}
	
	public String toString() {
		return _airline + _df.format(_id);
	}
}