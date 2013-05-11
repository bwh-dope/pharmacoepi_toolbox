/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.match;

import org.drugepi.PharmacoepiTool;
import org.drugepi.match.controllers.*;
import org.drugepi.util.*;

/**
 * Abstract class that encapsulates several algorithms to match on propensity scores.  
 * <p>
 * Patient information is specified through the {@link org.drugepi.PharmacoepiTool} addPatient method.
 * Patient data must be specified in the following order:
 * <ul>
 * 	<li>patient ID
 * 	<li>exposure group
 * 	<li>propensity score 1*
 * 	<li>propensity score 2
 * 	<li>...
 * 	<li>propensity score n.
 * </ul>
 * * Algorithms with more than two exposure groups require multiple propensity scores.
 * 
 * @author Jeremy A. Rassen
 * @version 1.0.0
 *
 */
public class Match extends PharmacoepiTool {
	public final String description = "Match Controller";
	 
	/**
	 * Path to store the match output file in.  Must be writable.  Any existing file will be replaced.
	 */
	public String outfilePath;
	
	protected int numGroups;
	
	public enum MatchType {
		NEAREST_NEIGHBOR, NN, BALANCED_NEAREST_NEIGHBOR, BALANCED_NN, 
		GREEDY_DIGIT, GREEDY_CALIPER, GREEDY, COMPLETE ;
		
		public static MatchType toMatchType(String s) {
			if (s == null) 
				return null;
			
			try {
				return(valueOf(s.toUpperCase()));
			} catch (Exception e) {
				return null;
			}
		}
	}

	public static final double INVALID_CALIPER = -1d;

	protected MatchType matchType;
	
	private MatchController matchController;
	
	/**
	 * Specifies the match ratio (n:1).
	 */
	public int matchRatio = 1;

	/**
	 * Specifies whether to allow variable ratio matching (0) or to enforce
	 * fixed ratio matching (1).
	 */
	public int fixedRatio = 0;
	
	/**
	 * For nearest neighbor matching, the maximum match distance.
	 */
	public double caliper = INVALID_CALIPER;

	/**
	 * For 2-way n:1 matching, whether to add secondary (teriary, etc.) matches
	 * in sequential passes or in a single parallel pass.  Default is sequential.
	 */
	public int parallelMatchingMode = 0;

	/**
	 * For greedy matching, digit of the propensity score the match should begin at.  Default = 5. 
	 */
	public int startDigit = 5;

	/**
	 * For greedy matching, digit of the propensity score the match should end at.  Default = 1. 
	 */
	public int endDigit = 1;

	/**
	 * Stores output data of the match, if no file is specified.
	 */
	private String matchOutputData;
	
	/**
	 * Generic constructor for matching algorithms.
	 */
	public Match()
	{
		super();
	}

	/**
	 * Initialize two-group matching algorithms.  Must be called before running the match.  
	 * 	
	 * @param matchType		Type of match: GREEDY or NN [nearest neighbor].
	 */
	public void initMatch(String matchType)
	throws Exception
	{
		this.initMatch(matchType, 2);
	}
	
	
	/**
	 * Initialize matching algorithms.  Must be called before running the match.  Provided
	 * as a convenience for calling from SAS macros.
	 * 	
	 * @param matchType		Type of match: GREEDY or NN [nearest neighbor].
	 * @param numGroups		Number of exposure groups.  Default is 2 (exposure and referent).
	 */
	public void initMatch(String matchType, String numGroups)
	throws Exception
	{
		this.initMatch(matchType, new Integer(numGroups).intValue());
	}
	
	public void initMatch(String matchType, int numGroups)
	throws Exception
	{
		this.initMatch(MatchType.toMatchType(matchType), numGroups);
	}
	
	/**
	 * Initialize matching algorithms.  Must be called before running the match.
	 * 	
	 * @param matchType		Type of match: GREEDY or NN [nearest neighbor].
	 * @param numGroups		Number of exposure groups.  Default is 2 (exposure and referent).
	 */
	public void initMatch(MatchType matchType, int numGroups)
	throws Exception
	{
		this.numGroups = numGroups;
		this.matchType = matchType;

		switch (matchType) {
			case NEAREST_NEIGHBOR:
			case NN:
				if (this.numGroups == 2)
					this.matchController = new TwoWayNearestNeighborMatch(this);
				else if (this.numGroups == 3)
					this.matchController = new ThreeWayNearestNeighborMatch(this);
				else if (this.numGroups >= 3)
					this.matchController = new NWayNearestNeighborMatch(this, this.numGroups);
				break;
				
			case BALANCED_NEAREST_NEIGHBOR:
			case BALANCED_NN:
				this.matchController = new TwoWayBalancedNearestNeighborMatch(this);
				break;
			
			case GREEDY_DIGIT:
			case GREEDY:
				this.matchController = new TwoWayDigitGreedyMatch(this);
				break;
				
			case GREEDY_CALIPER:
				this.matchController = new TwoWayCaliperGreedyMatch(this);
				break;

			case COMPLETE:
				this.matchController = new TwoWayCompleteMatching(this);
				break;

			default:
				throw new Exception("Unrecognized match type specified");
		}
		
		this.matchOutputData = null;
	}

	
	/**
	 * Add a match group to the match.
	 * 
	 * @param indicator		Value of the exposure group that indicates this match group.  Example: 
	 * 						0 for referent or 1 for exposed.
	 */
	public void addMatchGroup(String indicator)
	{
		this.matchController.addMatchGroup(indicator);
	}
	
    public void addPatients(RowReader reader)
    throws Exception
    {
    	this.matchController.addPatients(reader);
    }	        
    
	/**
	 * Begin execution of the match algorithm.
	 * 
	 * @throws Exception
	 */   
    public void run() 
	throws Exception
	{
    	this.startTool();
    	this.matchController.begin();
		this.matchController.match();
		
		RowWriter writer;
		if (this.outfilePath != null)
			writer = new TabDelimitedFileWriter(this.outfilePath);
		else
			writer = new StringBufferRowWriter();
		
		this.matchController.writePatients(writer);
		
		if (this.outfilePath == null)
			this.matchOutputData = writer.toString();
		
    	this.endTool();
	}
    
	/**
	 * @see  #outfilePath
	 */
	public String getOutfilePath() {
		return outfilePath;
	}

	/**
	 * @see  #outfilePath
	 */
	public void setOutfilePath(String outfilePath) {
		this.outfilePath = outfilePath;
	}
	
	/**
	 * @return the matchType
	 */
	public MatchType getMatchType() {
		return matchType;
	}
	
	/**
	 * @return the match output data
	 */
	public String getMatchOutputData() {
		return matchOutputData;
	}
}