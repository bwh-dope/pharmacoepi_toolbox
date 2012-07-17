/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.match.controllers;

import org.drugepi.match.*;
import org.drugepi.match.storage.*;

/**
 * Optimized, left-right balanced two-way nearest neighbor matching. 
 * <p>
 * This is experimental code that is not recommended for use.
 * 
 * @author Jeremy A. Rassen
 * @version 0.0.1
 *
 */
public class TwoWayBalancedNearestNeighborMatch extends TwoWayNearestNeighborMatch {
	public final String description = "n:1 Nearest Neighbor Matching With Balancing";
	
	/**
	 * Constructor for nearest neighbor matching.
	 */
	public TwoWayBalancedNearestNeighborMatch(Match match) {
		super(match);
	}
	
	protected void checkParameters()
	throws MatchException 
	{
		super.checkParameters();
		
//		if (this.match.parallelMatchingMode != 1) 
//			throw new MatchException("WARNING: Balanced matching implies parallel matching, so parallel mode should be 1.");
	}
	  
	protected void processAllUnmatchedCase(MatchSet ms, TwoWayMatchPatient refPt, 
			TwoWayMatchPatient txPt) 
	{
		boolean makeMatch = false;
		boolean isRightSideMatch = refPt.isToTheRightOf(txPt);
		boolean isLeftSideMatch = (! isRightSideMatch);

		// odd matches (first, third, etc.) can go on either side
		if (txPt.rightMatchCount == txPt.leftMatchCount)
			makeMatch = true;
		
		boolean txIsLeftHeavy = (txPt.leftMatchCount > txPt.rightMatchCount);
		boolean txIsRightHeavy = (! txIsLeftHeavy);
		
		// if this is a right side match and we're low on right side matches,
		// make the match
		if (isRightSideMatch && txIsLeftHeavy) 
			makeMatch = true;
		
		// if this is a left side match and we're low on left side matches,
		// make the match
		if (isLeftSideMatch && txIsRightHeavy) 
			makeMatch = true;
		
		// no need to store away sets if the match ratio is only 2 --
		// the reserved sets will never be used
		if ((! makeMatch) && (this.match.matchRatio > 2)) 
			txPt.addReserveMatchSet(ms, ! isLeftSideMatch);
		
		if (makeMatch) {
//			System.out.printf("%d (%d) [%d]\n", matches.size(), matchHeap.size(), 0);
   			txPt.matchCount++;
   			if (isLeftSideMatch)
   				txPt.leftMatchCount++;
  			else
  				txPt.rightMatchCount++;

   			// remove the referent patient from his binary tree
			refTree.remove(refPt.getPsAsDouble());
			refPt.setStatusMatched();

   			// check the treatment patient to see whether his max number of 
   			// matches has been reached
   			if ((this.match.parallelMatchingMode == 0) || 
   				(txPt.matchCount >= this.match.matchRatio)) {
   				txPt.setStatusMatched();
   				txTree.remove(txPt.getPsAsDouble());
   			}
   			
			matches.add(ms);

			// release extra matches that were gathered up on the left
			// while looking for a match on the right
   			MatchSet reserveMatchToAdd = txPt.getReserveMatchSet(isRightSideMatch);
   			if (reserveMatchToAdd != null)
   				this.matchHeap.add(reserveMatchToAdd);
		}
	}
	
    public void printMatchSpecificStatistics()
    {
		int numBalancedSets = 0;
		int numLeftHeavySets = 0;
		int numRightHeavySets = 0;

		for (MatchSet ms: this.matches)
		{
			TwoWayMatchPatient treatmentPatient = (TwoWayMatchPatient) ms.getTreatmentPatient();

			if (treatmentPatient.leftMatchCount == treatmentPatient.rightMatchCount)
				numBalancedSets++;
			else if (treatmentPatient.leftMatchCount > treatmentPatient.rightMatchCount)
				numLeftHeavySets++;
			else if (treatmentPatient.leftMatchCount < treatmentPatient.rightMatchCount)
				numRightHeavySets++;
		}
		
		System.out.printf("Total number of balanced sets: %d\n", numBalancedSets);
		System.out.printf("Total number of right-heavy sets: %d\n",
				numRightHeavySets);
		System.out.printf("Total number of left-heavy  sets: %d\n",
				numLeftHeavySets);
	}
}
