/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

package org.drugepi.emr;

import java.io.*;
import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.drugepi.emr.filters.*;
import org.drugepi.emr.transformers.EmrTokenTransformer;
import org.tartarus.snowball.SnowballStemmer;

import opennlp.tools.sentdetect.*;
import opennlp.tools.tokenize.*;

/**
 * Class to store a text item, usually a group of sentences within a text block.
 * 
 * @author Elaine Angelino
 * @author Jeremy A. Rassen
 */
public class EmrTextItem {
	public String text;

	private static SentenceModel sentModel;
	private SentenceDetectorME sentDetector;
	private static TokenizerModel tokenModel;
	private TokenizerME tokenizer;
	private List<EmrToken> tokens;

	private static HashMap<Integer, EmrTokenResources> resourceMap;

	/**
	 * Class to maintain resources for tokenizing. Each thread must have its own
	 * resources.
	 */
	public static class EmrTokenResources {
		public SentenceDetectorME sentDetector;
		public TokenizerME tokenizer;

		public EmrTokenResources() {
		}
	}

	/*
	 * Static elements for base sentence and tokenization models. Shared among
	 * all threads.
	 */
	static {
		try {
			InputStream modelIn = EmrTextItem.class
					.getResourceAsStream("en-sent.bin");
			sentModel = new SentenceModel(modelIn);

			modelIn = EmrTextItem.class.getResourceAsStream("en-token.bin");
			tokenModel = new TokenizerModel(modelIn);

			resourceMap = new HashMap<Integer, EmrTokenResources>();
		} catch (Exception e) {
			System.out.println(e.toString());
		}
	}

	/**
	 * Static method to create the sentence and token processing resources for
	 * for the current thread. Each operating thread must have its own
	 * resources.
	 * 
	 * @param hashCode
	 *            Hash code of current thread.
	 * @return
	 */
	private static synchronized EmrTokenResources createResources(int hashCode) {
		EmrTokenResources resources = new EmrTokenResources();
		resources.sentDetector = new SentenceDetectorME(sentModel);
		resources.tokenizer = new TokenizerME(tokenModel);
		resourceMap.put(hashCode, resources);
		return resources;
	}

	/**
	 * Retrieve the resources for the current thread, or create them if they
	 * don't already exist.
	 * 
	 * @return
	 */
	private static EmrTokenResources getResources() {
		int hashCode = Thread.currentThread().hashCode();
		EmrTokenResources resources = resourceMap.get(hashCode);
		if (resources == null) {
			resources = createResources(hashCode);
		}
		return resources;
	}

	public EmrTextItem(String s) {
		this.text = s;
		EmrTokenResources resources = getResources();
		this.sentDetector = resources.sentDetector;
		this.tokenizer = resources.tokenizer;
	}

	public EmrTextItem() {
		this.text = null;
	}

	public void clean() {
	}

	/**
	 * Take a string and create a series of tokens. Tokens are labeled according
	 * to type. Called by the tokenize() method, which is the public interface.
	 * 
	 * @param s
	 *            Input string
	 */
	private void tokenizeSentence(String s) {
		boolean sentenceHasTokens = false;
		String[] tokenStrings = null;
		try {
			tokenStrings = this.tokenizer.tokenize(s);
		} catch (Exception e) {
			System.out.printf("Error on string %s\n", s);
		}
		// remove whitespace and symbols
		tokenStrings = StringUtils.stripAll(tokenStrings,
				" !@#$%^&*()-=_+[]{};':,.<>/?\"");
		for (String t : tokenStrings) {
			// simplification of Elaine's code
			EmrToken token = null;

			// create token
			if ((t != null) && (t.length() > 0)) {
				if (t.equals("xxxx")) {
					// token = new EmrToken(t, "HIDDEN");
					token = null;
				} else if (StringUtils.isNumeric(t)) {
					token = new EmrToken(t, "NUMERIC");
				} else if (StringUtils.isAlpha(t)) {
					token = new EmrToken(t, "ALPHA");
				} else {
					token = new EmrToken(t, "MIXED");
				}
			}

			// keep non-null tokens
			if (token != null) {
				this.tokens.add(token);
				sentenceHasTokens = true;
			}
		}

		// Add a token for the end of the sentence.
		if (sentenceHasTokens) {
			EmrToken stopToken = new EmrToken("#", "STOP");
			this.tokens.add(stopToken);
		}
	}

	/**
	 * Tokenize the text stored in this object.
	 */
	public void tokenize() {
		this.tokens = new ArrayList<EmrToken>();

		String[] rawSentences = sentDetector.sentDetect(this.text);

		for (int i = 0; i < rawSentences.length; i++) {
			String s = rawSentences[i].toLowerCase();
			tokenizeSentence(s);
		}
	}

	/**
	 * After the text has been tokenized, apply a list of filters and/or
	 * transformers.
	 * 
	 * @param filters
	 *            Filters to apply. Filters take in a token and return either
	 *            the token or null.
	 * @param transformers
	 *            Transformers to apply. Transformers take in a token and return
	 *            the transformed version of the token (eg, stemming).
	 * @return List of strings that are the filtered and transformed tokens.
	 */
	public List<String> applyFiltersAndTransforms(List<EmrTokenFilter> filters,
			List<EmrTokenTransformer> transformers) {

		List<String> processedTokens = new ArrayList<String>(this.tokens.size());

		for (EmrToken token : this.tokens) {
			// apply filters
			// stop when all filters applied or a filter returns null
			if (filters != null)
				for (EmrTokenFilter filter : filters) {
					token = filter.filter(token);
					// stop filtering on null
					if (token == null)
						break;
				}

			// skip filtered tokens
			if (token == null)
				continue;

			// apply transformations
			if (transformers != null)
				for (EmrTokenTransformer transformer : transformers) {
					token = transformer.transform(token);
				}

			if (token != null)
				processedTokens.add(token.text);
		}

		return processedTokens;
	}
}
