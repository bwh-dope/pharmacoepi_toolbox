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
 * Fast implementation of 1:1 greedy matching.
 * <p>
 * For a description of the greedy matching algorithm, see:
 * <p>
 * Parsons, LS.  Reducing bias in a propensity score matched-pair sample using greedy matching techniques. 2001.
 * 
 * @author Jeremy A. Rassen
 * @version 1.0.0
 *
 */
public class TwoWayDigitGreedyMatch extends TwoWayMatchController {
	public final String description = "n:1 Digit-Based Greedy Matching";
 
	private int[] numMatchesAtDigit;

	/**
	 * Constructor for OneToOneGreedyMatch.
	 */
	public TwoWayDigitGreedyMatch(Match match) {
		super(match);
	}
	
	protected void checkParameters()
	throws MatchException 
	{
		super.checkParameters();
		if (this.match.startDigit < this.match.endDigit)
			throw new MatchException("Start digit should be greater than end digit (eg, start at digit 5 and end at digit 1).");

		if (this.match.startDigit <= 0)
			throw new MatchException("Start digit should be greater than 0.");

		if (this.match.endDigit <= 0)
			throw new MatchException("End digit should be greater than 0.");
}
	
    protected void doGreedyMatch(MatchGroup treatmentGroup, MatchGroup referentGroup, 
    		int matchPass)
    {
		System.out.printf("Beginning greedy match for 1:%d matches.\n", matchPass);

		numMatchesAtDigit = new int[this.match.startDigit + 1];
		for (int i = 0; i <= this.match.startDigit; i++)
			numMatchesAtDigit[i] = 0;
		
		// clear the match status of the patients in the treatment group, since they're
		// reused between runs.  also, build a tree of treatment patients
		TreeMap<Double, TwoWayMatchPatient> treatmentTree = new TreeMap<Double, TwoWayMatchPatient>();

		for (MatchPatient treatmentPatient: treatmentGroup) {
			treatmentPatient.setStatusUnmatched();
			treatmentTree.put(treatmentPatient.getPsAsDouble(), (TwoWayMatchPatient)treatmentPatient);
		}

		for (int numDigits = this.match.startDigit; numDigits >= this.match.endDigit; numDigits--) 
		{
			List<TwoWayMatchPatient> unmatchedReferentPatients = new ArrayList<TwoWayMatchPatient>();
			
			for (TwoWayMatchPatient treatmentPatient: treatmentTree.values()) {
				if (treatmentPatient.isUnmatched())
					treatmentPatient.applyDigitMask(numDigits);
			}
			
			for (MatchPatient p: referentGroup) {
				if (p.isUnmatched()) {
					TwoWayMatchPatient referentPatient = (TwoWayMatchPatient) p;
					referentPatient.applyDigitMask(numDigits);
					
					unmatchedReferentPatients.add(referentPatient);
				}
			}
			
			if (numDigits == this.match.startDigit)
				System.out.printf("... %d unmatched patients in the referent group\n", unmatchedReferentPatients.size());
			
			// sorts by the masked value, with ties broken by random id
			Collections.sort(unmatchedReferentPatients, TwoWayMatchPatient.greedyMatchComparator);
			
			double searchDelta = Math.pow(10d, -numDigits);
			
			for (TwoWayMatchPatient referentPatient: unmatchedReferentPatients) {
				MatchSet matchSet = null;
				
				// get all referent patients with the same masked value
				Double lowSearchValue = new Double(referentPatient.maskedValue) - searchDelta;
				Double highSearchValue = new Double(referentPatient.maskedValue + searchDelta);
				SortedMap<Double, TwoWayMatchPatient> resultsMap = treatmentTree.subMap(lowSearchValue, highSearchValue);
				
				TwoWayMatchPatient bestTreatmentPatient = null;
				for (TwoWayMatchPatient possibleTreatmentPatient: resultsMap.values()) {
					if (possibleTreatmentPatient.maskedValue == referentPatient.maskedValue) {
						if (bestTreatmentPatient == null)
							bestTreatmentPatient = possibleTreatmentPatient;
						else {
							double existingDist = MatchDistanceCalculator.getDistance(
									bestTreatmentPatient, referentPatient);
							double newDist = MatchDistanceCalculator.getDistance(
									possibleTreatmentPatient, referentPatient);
							
							if (newDist < existingDist)
								bestTreatmentPatient = possibleTreatmentPatient;
						}
					}
				}
				
				if (bestTreatmentPatient != null) {
					matchSet = new MatchSet(this.numGroups);
					matchSet.put(referentPatient);
					matchSet.putTreatmentPatient(bestTreatmentPatient);
					matchSet.matchInfo = String.format("Pass %d; Digit %d", matchPass, numDigits);
					matchSet.distance = MatchDistanceCalculator.getDistance(
							bestTreatmentPatient, referentPatient);
					this.matches.add(matchSet);

					numMatchesAtDigit[numDigits]++;

					matchSet.setAllMatched();
					
					bestTreatmentPatient.matchCount++;
					
					if ((this.match.parallelMatchingMode == 0) ||
						(bestTreatmentPatient.matchCount >= this.match.matchRatio))
						treatmentTree.remove(bestTreatmentPatient.getPsAsDouble());
				}
			}
		}
		
		for (int i = this.match.startDigit; i >= this.match.endDigit; i--)
			System.out.printf("... 1:%d matches at digit %d: %d pairs\n", matchPass, i, numMatchesAtDigit[i]);
    }
    
    public void printMatchSpecificStatistics()
    {
    	// do nothing
    }
    
	public void match()
	throws Exception
	{
		this.checkParameters();
		
	  	this.assignMatchGroups();
	  	this.printPreMatchStatistics();
	  	
       	if (this.match.parallelMatchingMode == 0) {
			for (int i = 1; i <= this.match.matchRatio; i++) {
				this.doGreedyMatch(this.treatmentGroup, this.referentGroup, i);
			}
       	} else {
			this.doGreedyMatch(this.treatmentGroup, this.referentGroup, 1);
       	}
			
    	this.collapseMatchSets();
       	this.printPostMatchStatistics();
	}
}


