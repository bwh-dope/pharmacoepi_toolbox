/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

package org.drugepi.emr.filters;

import java.util.HashSet;

import org.drugepi.emr.EmrToken;

/**
 * Class to filter designated stop words.  A default stop word list is supplied.  A custom
 * list can be supplied to the alternative constuctor.
 * 
 * @author Elaine Angelino
 * @author Jeremy A. Rassen
 */
public class EmrStopWordFilter extends EmrTokenFilter {
	public static String DEFAULT_STOP_LIST = "the,of,and,to,in,that,was,his,he,it,with," +
											 "is,for,as,had,you,be,her,on,at,by,she";
	private HashSet<String> stopWordSet;
	
	public EmrStopWordFilter(String stopWordList) {
		String[] stopWords = stopWordList.split(",");
		stopWordSet = new HashSet<String>();
		
		for (int i = 0; i < stopWords.length; i++) {
			stopWordSet.add(stopWords[i].toLowerCase());
		}
	}
	
	public EmrStopWordFilter() {
		this(DEFAULT_STOP_LIST);
	}
	
	public EmrToken filter(EmrToken t) {
		if (stopWordSet.contains(t.text.toLowerCase()))
			return null;
		
		return t;
	}
}
