/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.match;

import java.util.Random;

public class MatchRandomizer {
	final static private int seed = 1234567;
	
	private Random random;

	public MatchRandomizer() {
		random = new Random(seed);
	}
	
	public double nextDouble() {
		return(random.nextDouble());
	}
	
	public boolean nextBoolean() {
		return(random.nextBoolean());
	}

}
