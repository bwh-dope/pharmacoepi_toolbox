/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.match.controllers;

import java.util.*;

import org.drugepi.match.*;
import org.drugepi.match.Match.MatchType;
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
public abstract class TwoWayMatchController extends MatchController {
	public final String description = "Generic Two-Way Match";
	
	protected PriorityQueue<MatchSet> matchHeap;

	protected TreeMap<Double, TwoWayMatchPatient> txTree;
	protected TreeMap<Double, TwoWayMatchPatient> refTree;

	protected MatchGroup treatmentGroup;
	protected MatchGroup referentGroup;
	
	protected int treatmentGroupSize = 0;
	protected int referentGroupSize = 0;
	
	protected static final double LOW_PAD_VALUE = -100.0;
	protected static final double HIGH_PAD_VALUE = 100.0;

	/**
	 * Constructor for nearest neighbor matching.
	 */
	public TwoWayMatchController(Match match) {
		super(match, 2);
	}
	
	protected void checkParameters()
	throws MatchException 
	{
 		super.checkParameters();
		
		if (this.numGroups != 2)
			throw new MatchException("Only 2-way matching is supported for this match mode");
	}

	protected TwoWayMatchPatient createPatient(String[] row) 
	throws MatchException 
	{
		TwoWayMatchPatient patient = new TwoWayMatchPatient();

		try {
			patient.id = row[KEY_COLUMN];
		} catch (Exception e) {
			throw new MatchException("Failed to read patient IDs for all patients.");
		}
		
		try {
			patient.matchGroup = this.matchGroupMap.get(row[EXP_COLUMN]);
			if (patient.matchGroup == null)
				throw new MatchException("Failed to read valid match groups for all patients.");
		} catch (Exception e) {
			throw new MatchException("Failed to read valid match groups for all patients.");
		}

		try {
			// get PSs for all but the last group --
			// don't need the PS for the last group
			double ps = Double.parseDouble(row[PS_COLUMN]);
			// setPs will perturb slightly to avoid duplicates
			patient.addPs(ps); 
		} catch (Exception e) {
			throw new MatchException("Failed to read propensity scores for all patients.");
		}

		patient.setStatusUnmatched();

		return patient;
	}

	protected static void padPatientsInGroup(MatchGroup mg, int numToPad) {
		for (int i = 0; i < numToPad; i++) {
			TwoWayMatchPatient p = new TwoWayMatchPatient();
			p.id = String.format("BAD ID LOW %d", i);
			p.addPs(LOW_PAD_VALUE);
			p.matchGroup = mg;
			mg.add(p);
		}

		for (int i = 0; i < numToPad; i++) {
			TwoWayMatchPatient p = new TwoWayMatchPatient();
			p.id = String.format("BAD ID HIGH %d", i);
			p.addPs(HIGH_PAD_VALUE);
			p.matchGroup = mg;
			mg.add(p);
		}
	}

	protected static void unpadPatientsInGroup(MatchGroup mg) {
		for (Iterator<MatchPatient> iter = mg.iterator(); iter.hasNext();) {
			MatchPatient p = iter.next();
			if (p.id.startsWith("BAD ID"))
				iter.remove();
		}
	}

	public void assignMatchGroups()
	{
    	// set the treatment group to be the first-specified group
    	this.treatmentGroup = this.matchGroupsList.get(0);

	  	// set the referent group to be the second-specified group
    	this.referentGroup = this.matchGroupsList.get(1);

		this.treatmentGroupSize = treatmentGroup.size();
		this.referentGroupSize = referentGroup.size();
	}
	
	public void printPreMatchStatistics() 
	{
	  	System.out.printf("%d:1 match beginning using the %s match method in %s ratio, %s mode\n", 
	  			this.match.matchRatio, this.getClass().getSimpleName(),
	  			(this.match.fixedRatio == 0 ? "variable" : "fixed"),
	  			(this.match.parallelMatchingMode == 0 ? "sequential" : "parallel"));
	    
       	System.out.printf("%d items in the treatment (indicator=%s) group\n", this.treatmentGroup.size(), treatmentGroup.groupIndicator);
    	System.out.printf("%d items in the referent (indicator=%s) group\n", this.referentGroup.size(), referentGroup.groupIndicator);
	}
	
	public void printPostMatchStatistics() 
	{
		int[] matchedSetsPerLevel = new int[this.match.matchRatio + 1];
		int numRefPatientsMatched = 0;
		int numTreatmentPatientsMatched = 0;
		double totalMatchDistance = 0;

		if (this.match.getMatchType() == MatchType.COMPLETE) {
			for (MatchSet ms: this.matches)
			{
				numRefPatientsMatched += ms.getAll(this.referentGroup).size();
				numTreatmentPatientsMatched += ms.getAll(this.treatmentGroup).size();
			}
		} else {
			for (MatchSet ms: this.matches)
			{
				int numReferentPatients = ms.patients.size() - 1;
				matchedSetsPerLevel[numReferentPatients]++;
				numRefPatientsMatched += numReferentPatients;
				numTreatmentPatientsMatched++;
				totalMatchDistance += ms.distance;
			}
			
			for (int i = 1; i <= this.match.matchRatio; i++) {
				System.out.printf("Number of 1:%d matched sets: %d\n", i,
						matchedSetsPerLevel[i]);
			}
		}

		System.out.printf("Treated patients matched: %d (%3.1f%%)\n",
				numTreatmentPatientsMatched, (float) numTreatmentPatientsMatched
						/ (float) this.treatmentGroupSize * 100f);
		System.out.printf("Referent patients matched: %d (%3.1f%%)\n",
				numRefPatientsMatched, (float) numRefPatientsMatched
						/ (float) this.referentGroupSize * 100f);

		System.out.printf("Total number of matched sets: %d\n", matches.size());
		System.out.printf("Total match distance: %.4f\n", totalMatchDistance);
		
		this.printMatchSpecificStatistics();
	}
	
    public abstract void printMatchSpecificStatistics();

	
    protected String[] getOutputFields()
    {
    	final String[] outputFields = { "set_num", "pat_id", "group_indicator", "ps", 
    									"match_distance", "match_info" };
    	
    	return outputFields;
    }
        
    protected String[] getOutputData(int setNumber, MatchSet ms, MatchPatient p) {
       	final String quoteStr = "\"";
       	
    	String[] outputData = {
				Integer.toString(setNumber),
				quoteStr + p.id + quoteStr,
				quoteStr + p.matchGroup.groupIndicator + quoteStr,
				Double.toString(p.getPs()),
				Double.toString(ms.distance),
				quoteStr + ms.matchInfo + quoteStr
		};
    	
    	return outputData;
    }	

	public abstract void match()
	throws Exception;
}
