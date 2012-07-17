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

	public static void main(String[] args)
	  {
		String runType;
		
		runType = "nn";
//		runType = "balanced_nn";
//		runType = "greedy";
//		runType = "brute";
//		runType = "greedy_caliper";
//		runType = "complete";
		
		int numGroups;
		numGroups = 2;

		int matchRatio;
//		matchRatio = 1;
//		matchRatio = 2;
//		matchRatio = 3;
//		matchRatio = 4;
		matchRatio = 8;
		
//		String inDirectory = "/Users/jeremy/Documents/Windows Shared/Data/Projects/matching/";
//		String outDirectory = "/Users/jeremy/Documents/Windows Shared/Data/Projects/matching/";
		
//		String inDirectory = "/Volumes/Secure Disk Image/analgesics/";
//		String outDirectory = "/Users/jeremy/Desktop/";

		String inDirectory = "/Users/jeremy/Dropbox/JAR73/Projects/matching/anon_data/";
//		String inDirectory = "/Users/jeremy/Desktop/";
		String outDirectory = "/Users/jeremy/Desktop/";

//		String buf = "9970\t1\t-0.531523492653773\n9971\t1\t1.05447722664272\n9972\t0\t1.40543668623124\n9973\t0\t-0.473436795258838\n9974\t0\t-0.0306564772928972\n9975\t1\t-2.9333125149801\n9976\t1\t-0.164182076314819\n9977\t1\t-0.793981157122129\n9978\t0\t-0.816783800542681\n9979\t0\t-0.413613800097581\n9980\t0\t-1.56459705894743\n9981\t1\t0.253244050981263\n9982\t0\t-0.0624570505073035\n9983\t0\t-1.63817565162842\n9984\t1\t-0.663972358791491\n9985\t0\t0.214249878980373\n9986\t0\t0.50542202787103\n9987\t0\t1.04866970203843\n9988\t0\t-1.11893905423621\n9989\t0\t-0.727746435884549\n9990\t0\t-0.752610682567327\n9991\t0\t0.928527044057384\n9992\t1\t-0.581712475117916\n9993\t1\t-1.00265456444296\n9994\t0\t3.90864864674076\n9995\t0\t-0.264332992889959\n9996\t0\t0.551131117711795\n9997\t0\t-0.00367360626855811\n9998\t0\t1.18558557369563\n9999\t1\t-0.68097251037442\n10000\t1\t0.0646692819248309";
		
	  	try {
	  		for (int i = 0; i < 100; i++) {
	  			Match g = new Match();
	  			g.initMatch(runType, numGroups);
	  			g.outfilePath = outDirectory +
	  				String.format("match_%s_%d_output.txt", runType, numGroups); 
	
		  		// TWO-WAY
	  			if (numGroups == 2) {
	//		  		String infileName = inDirectory + "match_nsaid_opi.txt";
	//		  		String infileName = inDirectory + "match_nsaid_coxib.txt";
			  		String infileName = inDirectory + "statin_vytorin.txt";
		  			g.outfilePath = outDirectory + "match_" + runType + "_output.txt";
			  		g.addMatchGroup("1");
			  		g.addMatchGroup("0");
			  		g.addPatients(infileName);
	//		  		g.addPatientsFromBuffer(buf);
			  		g.parallelMatchingMode = 1;
		  			g.matchRatio = matchRatio;
		  			
	//	  			g.caliper = 0.001;
		  			g.fixedRatio = 0;
		  			
			  		g.run();
			  		
			  		String s = g.getMatchOutputData();
			  		System.out.println(s);
		  		}
		  		
		  		// THREE-WAY
	//  			if (numGroups == 3) {
	//		  		String infileName = inDirectory + "match_multi.txt";
	//		  		g.addMatchGroup("N");
	//		  		g.addMatchGroup("C");
	//		  		g.addMatchGroup("O");
	//		  		g.addPatients(infileName);
	//		  		g.run();
	//	  		}
	
				if (numGroups == 3) {
	//				String infileName = inDirectory + "opioids_match_3cat_anon.txt";
	//		  		g.addMatchGroup("C");
	//		  		g.addMatchGroup("D");
	//		  		g.addMatchGroup("O");
	
					String infileName = inDirectory + "match_nsaid_coxib_opi.txt";
			  		g.addMatchGroup("N");
			  		g.addMatchGroup("C");
			  		g.addMatchGroup("O");
	
	//		  		String infileName = "/Users/jeremy/Desktop/matching_input_data_3way.txt";
	//		  		g.addMatchGroup("1");
	//		  		g.addMatchGroup("2");
	//		  		g.addMatchGroup("3");
			  		g.caliper = 0.05;
			  		g.addPatients(infileName);
			  		g.run();
				}
	
	  			
	  			if (numGroups == 4) {
			  		String infileName = inDirectory + "opioids_match_4cat_anon.txt";
			  		g.addMatchGroup("C");
			  		g.addMatchGroup("D");
			  		g.addMatchGroup("O");
			  		g.addMatchGroup("U");
			  		g.addPatients(infileName);
			  		g.caliper = 0.1;
			  		g.run();
		  		}
	
	  			if (numGroups == 5) {
			  		String infileName = inDirectory + "opioids_match_5cat_anon.txt";
			  		g.addMatchGroup("C");
			  		g.addMatchGroup("D");
			  		g.addMatchGroup("O");
			  		g.addMatchGroup("U");
			  		g.addMatchGroup("V");
			  		g.addPatients(infileName);
			  		g.caliper = 0.05;
			  		g.run();
		  		}
	  		}
	   	} catch (Exception e) {
	   		e.printStackTrace();
		}
	 }
}
