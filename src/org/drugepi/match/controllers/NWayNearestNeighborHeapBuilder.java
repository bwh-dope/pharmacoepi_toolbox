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
 * Optimized N-way nearest neighbor matching.  EXPERIMENTAL ONLY.  
 * <p>
 * This is experimental code that is not recommended for use.
 * 
 * @author Jeremy A. Rassen
 * @version 0.0.1
 *
 */
public class NWayNearestNeighborHeapBuilder {
	public List<MatchPatient> baseGroup;
	private MultiMatchKDTree trees[];
	private NWayNearestNeighborMatch controller;
	private int threadNum;
//	private PriorityBlockingQueue<MatchSet> matchHeap;

	public NWayNearestNeighborHeapBuilder(NWayNearestNeighborMatch controller, int threadNum, MultiMatchKDTree trees[]) {
		this.controller = controller;
//		this.matchHeap = this.controller.matchHeap;
//			new PriorityQueue<MatchSet>(11, new MatchSetDistanceComparator());
		this.trees = new MultiMatchKDTree[trees.length];
		
		// the KD tree class is not thread-safe, so copy the trees
		for (int i = 0; i < trees.length; i++) {
			this.trees[i] = new MultiMatchKDTree();
			this.trees[i].putAll(trees[i]);
		}
	}

	public void buildHeap()
    throws Exception
    {
		int numOtherGroups = this.trees.length;
		
    	// for each patient in the smallest group, find all patients in Group 2
    	// within the width of the caliper
    	int numPatients = 0;
    	for (MatchPatient p: this.baseGroup) {
    		if (numPatients % 100 == 0) 
    	    	System.out.printf("[%d] Adding heap items for patient %d (%d items in heap) at %s\n", 
    	    			this.threadNum, numPatients, this.controller.matchHeap.size(), new Date().toString());

    		MultiMatchPatient basePatient = (MultiMatchPatient) p;
    		
    		// find the nearest patient in each of the other groups
    		boolean hasNearestPatients = true;
    		MultiMatchPatient nearestPatients[] = new MultiMatchPatient[numOtherGroups + 1];
    		for (int i = 0; i < numOtherGroups; i++) {
    			nearestPatients[i] = this.trees[i].getNearestPatient(basePatient, this.controller.match.caliper);
    			if (nearestPatients[i] == null)
    				hasNearestPatients = false;
    		}
    		
    		double penalizedCaliper = Math.pow(this.controller.match.caliper, 2);
    		
    		if (hasNearestPatients) {
	    		// hack: put the base patient in this array
	    		nearestPatients[numOtherGroups] = basePatient;
	    		
//	    		double delta = MatchDistanceCalculator.getDistanceFromCentroid(nearestPatients);
//	    		delta = delta * this.match.searchExpansionFactor;
	    		double delta = MatchDistanceCalculator.getEuclideanDistanceFromCentroid(nearestPatients);
	    		
	    		MatchPermuter permuter = new MatchPermuter(numOtherGroups);
	    		for (int i = 0; i < numOtherGroups; i++) {
	        		List<MultiMatchPatient> patientsWithinDelta = 
	        			this.trees[i].getNearestPatients(basePatient, delta);
	        		permuter.addPatientGroup(patientsWithinDelta);
	    		}
	    			
	    		// add each permutation to the heap
	    		MatchSet ms = null;
	    		boolean permutationsPossible = permuter.startPermutations();
	    		if (permutationsPossible) {
		    		do {
		    			ms = permuter.getNextPermutation();
		    			if (ms != null) {
		    				ms.put(basePatient);
		    				ms.distance = MatchDistanceCalculator.getPenalizedDistanceFromCentroid(
		    						ms.patients.toArray(new MatchPatient[this.controller.numGroups]));
		    				if (ms.distance < penalizedCaliper)
		    					this.controller.matchHeap.add(ms);
		    			}
		    		} while (ms != null);
	    		}
	    	}

    		numPatients++;
    	}
    }
}
