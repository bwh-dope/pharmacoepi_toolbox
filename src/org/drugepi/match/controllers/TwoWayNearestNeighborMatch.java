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
public class TwoWayNearestNeighborMatch extends TwoWayMatchController {
	public final String description = "n:1 Nearest Neighbor Matching";
	
	protected static final double DEFAULT_CALIPER = 0.05;

	protected PriorityQueue<MatchSet> matchHeap;

	protected TreeMap<Double, TwoWayMatchPatient> txTree;
	protected TreeMap<Double, TwoWayMatchPatient> refTree;
	
	protected MatchGroup workingReferentGroup;

	/**
	 * Constructor for nearest neighbor matching.
	 */
	public TwoWayNearestNeighborMatch(Match match) {
		super(match);
		matchHeap = new PriorityQueue<MatchSet>(11,
				new MatchSetDistanceComparator());
		txTree = new TreeMap<Double, TwoWayMatchPatient>();
		refTree = new TreeMap<Double, TwoWayMatchPatient>();
	}

	protected void checkParameters()
	throws MatchException 
	{
		super.checkParameters();
	}
	
	protected void addToHeap(TwoWayMatchPatient p1, TwoWayMatchPatient p2) {
		TwoWayMatchPatient txPt = (p1.matchGroup == treatmentGroup) ? p1 : p2;
		TwoWayMatchPatient refPt = (p1.matchGroup == treatmentGroup) ? p2 : p1;

		MatchSet ms = new MatchSet(this.numGroups);
		ms.putTreatmentPatient(txPt);
		ms.put(refPt);

		ms.distance = MatchDistanceCalculator.getDistance(txPt, refPt);
		if (ms.distance < this.match.caliper)
			matchHeap.add(ms);
	}

	protected TwoWayMatchPatient processPatients(MatchGroup mg, TreeMap<Double, TwoWayMatchPatient> tree) {
		ArrayList<TwoWayMatchPatient> groupList = new ArrayList<TwoWayMatchPatient>();

		for (MatchPatient p : mg) {
			TwoWayMatchPatient patient = (TwoWayMatchPatient) p;
			patient.setStatusUnmatched();
			groupList.add(patient);
			tree.put(patient.getPsAsDouble(), patient);
		}

		TwoWayMatchPatient firstPatient = null;
		TwoWayMatchPatient lastPatient = null;
		for (TwoWayMatchPatient p : tree.values()) {
			// store the first patient in the list
			if (firstPatient == null)
				firstPatient = p;

			// set the last patient's next in list to be the current patient
			if (lastPatient != null)
				lastPatient.setNextPatientInThisGroup(p);

			// update pointers
			lastPatient = p;
		}

		return firstPatient;
	}

	protected void processAllUnmatchedCase(MatchSet ms, TwoWayMatchPatient refPt, TwoWayMatchPatient txPt) {
		txPt.matchCount++;

		// remove the treated patient from his binary tree
		refTree.remove(refPt.getPsAsDouble());
		refPt.setStatusMatched();

		// check the referent patient to see whether his max number of
		// matches has been reached
		if ((this.match.parallelMatchingMode == 0) ||
			(txPt.matchCount >= this.match.matchRatio)) {
			txPt.setStatusMatched();
			txTree.remove(txPt.getPsAsDouble());
		}

		matches.add(ms);
	}

	protected void processAllMatchedCase(MatchSet ms, TwoWayMatchPatient refPt, TwoWayMatchPatient txPt)  
	{
		// do nothing
	}

	protected void processSomeUnmatchedCase(MatchSet ms, TwoWayMatchPatient refPt, TwoWayMatchPatient txPt) 
	{
		TwoWayMatchPatient matchedPatient = (TwoWayMatchPatient) ms
				.getFirstMatchedPatient();
		TwoWayMatchPatient unmatchedPatient = (matchedPatient == txPt) ? refPt
				: txPt;

		TreeMap<Double, TwoWayMatchPatient> searchTree = (matchedPatient.matchGroup == treatmentGroup) ? txTree
				: refTree;

		// Replace the matched patient in the match with the next best
		// unmatched choice
		SortedMap<Double, TwoWayMatchPatient> ltMap = null;
		SortedMap<Double, TwoWayMatchPatient> gtMap = null;
		TwoWayMatchPatient leftPatient = null;
		TwoWayMatchPatient rightPatient = null;
		double distanceToLeft = Double.MAX_VALUE;
		double distanceToRight = Double.MAX_VALUE;

		TwoWayMatchPatient searchPatient = unmatchedPatient;
		while (searchPatient != null) {
			TwoWayMatchPatient p = null;
			ltMap = searchTree.headMap(searchPatient.getPs());
			// bad, bad, bad -- but no way to go backwards
			// through a tree in Java 1.5 (required for SAS)

			// test whether the map has anything in it
			// the size() function is very expensive, for some reason.
			Double lastKey = null;
			try {
				lastKey = ltMap.lastKey();
			} catch (NoSuchElementException e) {
				// do nothing
			}
			if (lastKey != null) {
				p = ltMap.get(lastKey);
				if (p.isUnmatched()) {
					leftPatient = p;
					break;
				}
			}
			searchPatient = p;
		}

		if (leftPatient != null)
			distanceToLeft = MatchDistanceCalculator.getDistance(leftPatient,
					unmatchedPatient);

		gtMap = searchTree.tailMap(unmatchedPatient.getPs());
		for (TwoWayMatchPatient p : gtMap.values()) {
			if (p.isUnmatched()) {
				rightPatient = p;
				break;
			}
		}
		
		if (rightPatient != null)
			distanceToRight = MatchDistanceCalculator.getDistance(rightPatient,
					unmatchedPatient);

		if (distanceToLeft < distanceToRight)
			this.addToHeap(leftPatient, unmatchedPatient);
		else if (distanceToRight < distanceToLeft)
			this.addToHeap(rightPatient, unmatchedPatient);
	}

	protected void processHeap() {
		TwoWayMatchPatient refPt;
		TwoWayMatchPatient txPt;

		while (! this.matchHeap.isEmpty()) {
			MatchSet ms = this.matchHeap.poll();

			txPt = (TwoWayMatchPatient) ms.getTreatmentPatient();
			refPt = (TwoWayMatchPatient) ms.getNonTreatmentPatient();

			if (ms.allAreUnmatched()) {
				this.processAllUnmatchedCase(ms, refPt, txPt);
			} else if (ms.allAreMatched()) {
				this.processAllMatchedCase(ms, refPt, txPt);
			} else if (ms.someAreMatched()) {
				this.processSomeUnmatchedCase(ms, refPt, txPt);
			}
		}
	}

	protected void buildInitialHeap(TwoWayMatchPatient firstPatientInTreatmentGroup,
			TwoWayMatchPatient firstPatientInReferentGroup) 
	{
		TwoWayMatchPatient refPt = firstPatientInReferentGroup;
		TwoWayMatchPatient txPt = firstPatientInTreatmentGroup;

		// create initial matches
		do {
			// find the patient just to the right of the current treatment
			// patient
			while ((refPt != null) && (refPt.getPs() < txPt.getPs()))
				refPt = refPt.getNextPatientInThisGroup();

			TwoWayMatchPatient rightPatient = refPt;
			TwoWayMatchPatient leftPatient = rightPatient.getPrevPatientInThisGroup();

			// add right- and left-side patients to the heap
			for (int i = 0; i < this.match.matchRatio; i++) {
				if (rightPatient != null) {
					this.addToHeap(txPt, rightPatient);
					rightPatient = rightPatient.getNextPatientInThisGroup();
				}

				if (leftPatient != null) {
					this.addToHeap(txPt, leftPatient);
					leftPatient = leftPatient.getPrevPatientInThisGroup();
				}
			}

			// advance the treatment patient
			txPt = txPt.getNextPatientInThisGroup();
		} while (txPt != null);
	}
	
	protected void runMatchingProcess(int passNum)
	{
		System.out.printf("Beginning match pass %d\n", passNum);
		
		TwoWayMatchPatient firstPatientInTreatmentGroup = processPatients(this.treatmentGroup, this.txTree);
		TwoWayMatchPatient firstPatientInReferentGroup = processPatients(this.workingReferentGroup, this.refTree);

		this.buildInitialHeap(firstPatientInTreatmentGroup, firstPatientInReferentGroup);
		this.processHeap();
	}
	
    public void printMatchSpecificStatistics()
    {
		// do nothing
	}

	public void match()
	throws Exception
	{
		this.checkParameters();
		
		if (this.match.caliper == Match.INVALID_CALIPER)
			this.match.caliper = DEFAULT_CALIPER;

		this.assignMatchGroups();
		this.printPreMatchStatistics();
		
		this.workingReferentGroup = new MatchGroup();
		this.workingReferentGroup.addAll(this.referentGroup);

		// ensure that the referent group also has the patient with the lowest
		// and highest PSs
		// no need to unpad, since the original referent group will be left as-is
		TwoWayMatchController.padPatientsInGroup(this.workingReferentGroup, this.match.matchRatio);
		
		if (this.match.parallelMatchingMode == 0) {
			for (int i = 1; i <= this.match.matchRatio; i++) {
				runMatchingProcess(i);
				
				// rebuild the referent group from those remaining
				this.workingReferentGroup.clear();
				this.workingReferentGroup.addAll(this.refTree.values());
				refTree.clear();
				
				for (TwoWayMatchPatient p: this.txTree.values()) {
					p.setStatusUnmatched();
					// needed for balanced nn matching
					p.clearReservedMatchSets();
				}
			}
      	} else 
       		runMatchingProcess(1);
      	
		System.out.printf("Matching complete.\n");

		this.collapseMatchSets();
		Collections.sort(this.matches, new MatchSetDistanceComparator());
		
		this.printPostMatchStatistics();
	}
}
