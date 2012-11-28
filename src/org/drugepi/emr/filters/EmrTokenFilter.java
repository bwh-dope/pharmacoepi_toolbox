/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

/**
 * Generic class to filter EMR tokens.  Filters can be subclassed from here.  
 * Filter does nothing; returns the token input.
 * 
 * @author Elaine Angelino
 * @author Jeremy A. Rassen
 */
package org.drugepi.emr.filters;

import org.drugepi.emr.EmrToken;

public class EmrTokenFilter {
	/**
	 * Filter a token
	 * 
	 * @param t Input token
	 * @return The token input, or null if the token is to be filtered out.
	 */
	public EmrToken filter(EmrToken t) {
		return t;
	}
}
