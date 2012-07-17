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
 * Optimized two-way nearest neighbor matching.  EXPERIMENTAL ONLY.  
 * <p>
 * This is experimental code that is not recommended for use.
 * 
 * @author Jeremy A. Rassen
 * @version 0.0.1
 *
 */
public class BruteForceTwoWayMatch extends MatchController {
	public final String description = "Brute Force Nearest Neighbor Match";
	
	private static final double DEFAULT_CALIPER = 0.075;
	
	/**
	 * Maximum match distance.
	 */ 
	private PriorityQueue<MatchSet> matchHeap;
	private HashMap<String, MatchPatient> matchedPatients;
		
	/**
	 * Constructor for nearest neighbor matching.
	 */
	public BruteForceTwoWayMatch(Match match) {
		super(match, 2);
		matchHeap = new PriorityQueue<MatchSet>(11, new MatchSetDistanceComparator());
		matchedPatients = new HashMap<String, MatchPatient>();
	}
	
    protected MatchPatient createPatient(String[] row) {
    	MatchPatient patient =  new MatchPatient(2);
        patient.id = row[KEY_COLUMN];
        patient.matchGroup = this.matchGroupMap.get(row[EXP_COLUMN]);
        if (patient.matchGroup == null)
        	return null;
        
        // get PSs for all but the last group --
        // don't need the PS for the last group
        double ps = Double.parseDouble(row[PS_COLUMN]);
        // setPs() will perturb slightly to avoid duplicates
		patient.addPs(ps);
		
        return patient;
    }	
	
	private void addToHeap(MatchPatient p1, MatchPatient p2)
	{
   		MatchSet ms = new MatchSet(this.numGroups);
		ms.put(p1);
		ms.put(p2);
		
		ms.distance = MatchDistanceCalculator.getDistance(p1, p2);
		if (ms.distance < this.match.caliper) {
			matchHeap.add(ms);
   			if (this.matchHeap.size() % 100000 == 0)
   				System.out.printf("Heap size: %d\n", this.matchHeap.size());
		}
		
		//System.out.printf("Adding: p1=%f p2=%f dist=%f\n", p1.ps, p2.ps, ms.distance);
	}
	
    public void match()
    {
    	System.out.println("Using two-way brute force match algorithm");

    	if (this.match.caliper == Match.INVALID_CALIPER)
    		this.match.caliper = DEFAULT_CALIPER;
    	
    	// !!! what if equal?
    	MatchGroup smallerGroup;
    	MatchGroup largerGroup;
    	if (this.matchGroupsList.get(0).size() < this.matchGroupsList.get(1).size()) {
    		smallerGroup = this.matchGroupsList.get(0);
    		largerGroup = this.matchGroupsList.get(1);
    	} else {
    		smallerGroup = this.matchGroupsList.get(1);
    		largerGroup = this.matchGroupsList.get(0);
    	}

       	System.out.printf("%d items in the smaller group\n", smallerGroup.size());
       	System.out.printf("%d items in the larger group\n", largerGroup.size());
       	
       	for (int i = 0; i < smallerGroup.size(); i++) {
       		for (int j = 0; j < largerGroup.size(); j++) {
       			this.addToHeap(smallerGroup.get(i), largerGroup.get(j));
       		}
       	}
       	
       	System.out.printf("%d possible matches created\n", matchHeap.size());
       	

    	System.out.println("Finding optimal matches");
    	while (! matchHeap.isEmpty()) {
       		// get the smallest distance match
       		MatchSet ms = matchHeap.poll();
       		
       		//System.out.printf("Pulled from heap. Distance = %f\n", ms.distance);
       		
       		MatchPatient s = ms.get(smallerGroup);
       		MatchPatient l = ms.get(largerGroup);
       		
       		if ((! matchedPatients.containsKey(s.id)) &&
       			(! matchedPatients.containsKey(l.id))) {
       			matches.add(ms);
       			if (this.matches.size() % 100 == 0)
       		       	System.out.printf("%d matched pairs found; %d possibilities remain\n", 
       		       			matches.size(),
       		       			matchHeap.size());
       			
       			matchedPatients.put(s.id, s);
       			matchedPatients.put(l.id, l);
       		}
    	}
       			
       	System.out.printf("%d matched pairs found\n", matches.size());
    }
}
