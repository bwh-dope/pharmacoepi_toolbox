/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.match.controllers;

import java.util.*;

import org.drugepi.match.*;
import org.drugepi.match.storage.*;

/**
 * Optimized two-way nearest neighbor matching. EXPERIMENTAL ONLY.
 * <p>
 * This is experimental code that is not recommended for use.
 * 
 * @author Jeremy A. Rassen
 * @version 0.0.1
 * 
 */
public class TwoWayCompleteMatching extends TwoWayMatchController {
	public final String description = "n:1 Caliper-Based Greedy Matching";
	
	protected TreeMap<Double, TwoWayMatchPatient> txTree;
	protected TreeMap<Double, TwoWayMatchPatient> refTree;

	/**
	 * Maximum match distance.
	 */
	private static final double DEFAULT_CALIPER = 0.0001;

	/**
	 * Constructor for nearest neighbor matching.
	 */
	public TwoWayCompleteMatching(Match match) {
		super(match);
		txTree = new TreeMap<Double, TwoWayMatchPatient>();
		refTree = new TreeMap<Double, TwoWayMatchPatient>();
	}
 
	protected void checkParameters()
	throws MatchException 
	{
		super.checkParameters();
		if (this.match.parallelMatchingMode == 1)
			throw new MatchException("Parallel mode complete matching is not supported.");
		if (this.match.caliper < 0)
			throw new MatchException("Caliper must be > 0");
	}
	
	protected double[] buildTree(TreeMap<Double, TwoWayMatchPatient> tree, MatchGroup mg) 
	{
    	double[] range = new double[2];
    	
    	range[0] = Double.MAX_VALUE;
    	range[1] = Double.MIN_VALUE;
    	
    	// build a tree of referent patients
		for (MatchPatient p: mg) {
			double ps = p.getPs();
			if (ps < range[0]) range[0] = ps;
			if (ps > range[1]) range[1] = ps;
			tree.put(p.getPsAsDouble(), (TwoWayMatchPatient) p);
		}
		
		return range;
	}
	
    protected void doCompleteMatch()
    {
    	double[] refRange = this.buildTree(this.refTree, this.referentGroup);
    	double[] treatmentRange = this.buildTree(this.txTree, this.treatmentGroup);

    	double[] overallRange = new double[2];
    	
    	overallRange[0] = Math.min(refRange[0], treatmentRange[0]);
    	overallRange[1] = Math.max(refRange[1], treatmentRange[1]);
    	
//    	for (double searchRange = overallRange[0]; searchRange <= overallRange[1]; searchRange += this.match.caliper) {
    	for (double searchRange = 0d; searchRange <= 1d; searchRange += this.match.caliper) 
    	{
    		SortedMap<Double, TwoWayMatchPatient> txSearchTree;
    		SortedMap<Double, TwoWayMatchPatient> refSearchTree;
    		
    		// will get the range of [searchRange to searchRange + caliper)
    		txSearchTree = this.txTree.subMap(searchRange, searchRange + this.match.caliper);
    		
    		// will get the range of [searchRange to searchRange + caliper)
    		refSearchTree = this.refTree.subMap(searchRange, searchRange + this.match.caliper);
    		
    		boolean matchFound;
    		try {
    			txSearchTree.firstKey();
    			refSearchTree.firstKey();
    			matchFound = true;
    		} catch (Exception e) {
    			matchFound = false;
    		}
    		
    		if (matchFound) 
    		{
    			MatchSet ms = new MatchSet(2);
    			
    			for (TwoWayMatchPatient p : txSearchTree.values())  
    				ms.put(p);

    			for (TwoWayMatchPatient p : refSearchTree.values())  
    				ms.put(p);
    			
    			ms.matchInfo = String.format("%.6f to %.6f", searchRange, searchRange + this.match.caliper);
    			this.matches.add(ms);
    		}
    	}
     }
    
    public void printMatchSpecificStatistics()
    {
    	// do nothing
    }
    
	public void match()
	throws Exception
	{
		if (this.match.caliper == Match.INVALID_CALIPER)
			this.match.caliper = DEFAULT_CALIPER;

		this.checkParameters();

	  	this.assignMatchGroups();
	  	this.printPreMatchStatistics();

		this.doCompleteMatch();

       	this.printPostMatchStatistics();
	}
}
