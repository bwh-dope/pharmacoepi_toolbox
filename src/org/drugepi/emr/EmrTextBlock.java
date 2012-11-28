/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

package org.drugepi.emr;

import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.drugepi.emr.filters.*;
import org.drugepi.emr.transformers.EmrTokenTransformer;

/**
 * Class to store and process a text block. A text block contains a title and
 * one or more text items.
 * 
 * @author Elaine Angelino
 * @author Jeremy A. Rassen
 */
public class EmrTextBlock {
	public String title;
	public List<EmrTextItem> textItems;

	public EmrTextBlock() {
		this.textItems = new ArrayList<EmrTextItem>();
	}

	public void addTextItem(String s) {
		EmrTextItem ti = new EmrTextItem(s);
		ti.tokenize();
		this.textItems.add(ti);
	}

	/**
	 * Turn this text block into a list of ngrams at the specified set of
	 * levels. This is more efficient than running at individual levels, since
	 * the filtered and transformed list of tokens can be re-used.
	 * 
	 * @param minLevel
	 *            Minimum level of n.
	 * @param maxLevel
	 *            Maximum level of n.
	 * @param filters
	 *            A list of token filters to apply. Filters must be subclasses
	 *            of EmrTokenFilter.
	 * @param transforms
	 *            A list of token transformers to apply. Transformers must be
	 *            subclasses of EmrTokenTransformer.
	 * @return A hashtable indexed by integer levels with value
	 *         [minLevel..maxLevel]. Each entry contains a list of Strings,
	 *         which are the ngrams.
	 */
	public Hashtable<Integer, List<String>> toNgrams(int minLevel,
			int maxLevel, List<EmrTokenFilter> filters,
			List<EmrTokenTransformer> transforms) {

		Hashtable<Integer, List<String>> ngrams = new Hashtable<Integer, List<String>>();

		if ((minLevel < 1) || (maxLevel < minLevel))
			return null;

		// Apply filters and transforms
		List<String> activeTokens = new ArrayList<String>();

		for (EmrTextItem ti : this.textItems) {
			activeTokens.addAll(ti.applyFiltersAndTransforms(filters,
					transforms));
		}

		for (int level = minLevel; level <= maxLevel; level++) {
			List<String> ngramsAtLevel = new LinkedList<String>();
			ngrams.put(level, ngramsAtLevel);
			// moving window of n-grams
			for (int i = 0; i < activeTokens.size() - level; i++) {
				ngramsAtLevel.add(StringUtils.join(
						activeTokens.subList(i, i + level), " "));
			}
		}

		return ngrams;
	}

	/**
	 * Turn this text block into a list of ngrams at a single level. Convenience
	 * function that wraps the multi-level implementation of toNgrams.
	 * 
	 * @param level
	 *            Number of words per ngram (ie, n).
	 * @param filters
	 *            A list of token filters to apply. Filters must be subclasses
	 *            of EmrTokenFilter.
	 * @param transforms
	 *            A list of token transformers to apply. Transformers must be
	 *            subclasses of EmrTokenTransformer.
	 * @return An List of strings, each containing ngrams at the specified
	 *         level.
	 */
	public List<String> toNgrams(int level, List<EmrTokenFilter> filters,
			List<EmrTokenTransformer> transforms) {

		Hashtable<Integer, List<String>> ngrams = this.toNgrams(level, level,
				filters, transforms);
		if (ngrams != null)
			// return the first value
			// there's probably a better way to do this.
			return ngrams.values().iterator().next();

		return null;
	}
}
