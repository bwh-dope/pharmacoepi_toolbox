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
public class TwoWayCaliperGreedyMatch extends TwoWayMatchController {
	public final String description = "n:1 Caliper-Based Greedy Matching";
	
	protected TreeMap<Double, TwoWayMatchPatient> refTree;

	/**
	 * Maximum match distance.
	 */
	private static final double DEFAULT_CALIPER = 0.05;

	/**
	 * Constructor for nearest neighbor matching.
	 */
	public TwoWayCaliperGreedyMatch(Match match) {
		super(match);
		refTree = new TreeMap<Double, TwoWayMatchPatient>();
	}

	protected void checkParameters()
	throws MatchException  
	{
		super.checkParameters();
		if (this.match.parallelMatchingMode == 1)
			throw new MatchException("Parallel mode caliper-based greedy matching is not supported.");
		if (this.match.caliper < 0)
			throw new MatchException("Caliper must be > 0");
	}
	
    protected void doGreedyMatch(int matchPass)
    {
		// sort the treated patients randomly
		Collections.sort(this.treatmentGroup, new MatchPatientRandomIDComparator());

		for (MatchPatient p: treatmentGroup) {
			TwoWayMatchPatient treatmentPatient = (TwoWayMatchPatient) p;
			
			// find the nearest available referent patient, to the left and the right
			double possRefKey1 = refTree.headMap(treatmentPatient.getPsAsDouble()).lastKey();
			double possRefKey2 = refTree.tailMap(treatmentPatient.getPsAsDouble()).firstKey();
			
			double dist1 = Double.MAX_VALUE;
			double dist2 = Double.MAX_VALUE;
			
			dist1 = MatchDistanceCalculator.getDistance(treatmentPatient.getPs(), possRefKey1);
			dist2 = MatchDistanceCalculator.getDistance(treatmentPatient.getPs(), possRefKey2);
			
			if ((dist1 < this.match.caliper) || (dist2 < this.match.caliper)) {
				Double nearestKey = (dist1 <= dist2 ? possRefKey1 : possRefKey2);
				
				// get the patient and remove him from the tree
				TwoWayMatchPatient bestRefPatient = refTree.remove(nearestKey);
				
				MatchSet matchSet = new MatchSet(2);
				matchSet.putTreatmentPatient(treatmentPatient);
				matchSet.put(bestRefPatient);
				matchSet.distance = Math.min(dist1, dist2);
				matchSet.matchInfo = String.format("Pass %d", matchPass);
				this.matches.add(matchSet);
			}
		}
		
	   	TwoWayMatchController.unpadPatientsInGroup(this.referentGroup);
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

    	// make sure the referent group has the most extreme patients
    	TwoWayMatchController.padPatientsInGroup(this.referentGroup, 1);

    	// build a tree of referent patients
		for (MatchPatient refPatient: this.referentGroup) {
			refTree.put(refPatient.getPsAsDouble(), (TwoWayMatchPatient) refPatient);
		}

	  	// no such thing as parallel model
	  	for (int i = 1; i <= this.match.matchRatio; i++) {
			this.doGreedyMatch(i);
		}
			
    	this.collapseMatchSets();
       	this.printPostMatchStatistics();
	}
}
