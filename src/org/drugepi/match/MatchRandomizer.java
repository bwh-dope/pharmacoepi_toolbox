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
