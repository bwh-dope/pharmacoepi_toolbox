/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

package org.drugepi.emr.transformers;

import java.util.HashMap;
import org.drugepi.emr.EmrToken;
import org.tartarus.snowball.SnowballStemmer;

/**
 * Class to transform EMR tokens by applying Porter2 stemming.  Uses the Snowball library 
 * with English language training.
 * 
 * @author Elaine Angelino
 * @author Jeremy A. Rassen
 */
public class PorterStemmerTransformer extends EmrTokenTransformer {
	private static HashMap<Integer, SnowballStemmer> stemmers;
	
	static 
	{
		stemmers = new HashMap<Integer, SnowballStemmer>();
	}
	
	/**
	 * Static method to create a stemmer for the current thread.  Each operating thread must have 
	 * its own stemmer.  
	 * 
	 * @param hashCode Hash code of current thread.
	 * @return
	 */
	private static synchronized SnowballStemmer createStemmer(int hashCode) {
		try {
			Class<?> stemClass = Class.forName("org.tartarus.snowball.ext.englishStemmer");
			SnowballStemmer stemmer = (SnowballStemmer) stemClass.newInstance();
			stemmers.put(hashCode, stemmer);
			return stemmer;
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * Retrieve the stemmer for the current thread, or create one if one doesn't already exist.
	 * 
	 * @return
	 */
	private static SnowballStemmer getStemmer() {
		int hashCode = Thread.currentThread().hashCode();
		SnowballStemmer stemmer = stemmers.get(hashCode);
		if (stemmer == null) {
			stemmer = createStemmer(hashCode);
		}
		return stemmer;
	}

	public EmrToken transform(EmrToken t) {
		SnowballStemmer stemmer = getStemmer();
		stemmer.setCurrent(t.text);
		stemmer.stem();
		String stem = stemmer.getCurrent();
		
		if (! stem.equals(t.text)) {
			EmrToken newToken = new EmrToken(stem, t.type);
			return newToken;
		}
		return t;
	}
}
