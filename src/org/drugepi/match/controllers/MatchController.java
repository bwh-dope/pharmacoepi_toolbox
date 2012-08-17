/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.match.controllers;

import java.util.*;

import org.drugepi.match.*;
import org.drugepi.match.storage.*;
import org.drugepi.util.*;

public abstract class MatchController {
	public final String description = "Match Controller";
	
	protected int numGroups;
	protected static final int KEY_COLUMN = 0;
	protected static final int EXP_COLUMN = 1;
	protected static final int PS_COLUMN = 2;
	
	protected Map<String, MatchGroup> matchGroupMap;
	protected List<MatchGroup> matchGroupsList;
	
	protected List<MatchSet> matches;
	
	protected Match match;
	
	protected static MatchRandomizer randomizer = new MatchRandomizer();

	/**
	 * Generic constructor for matching algorithms.
	 */
	public MatchController(Match match, int numGroups) {
		super();
		
		this.match = match;
		this.numGroups = numGroups;

		this.numGroups = numGroups;
		this.matchGroupMap = new HashMap<String, MatchGroup>();
		this.matchGroupsList = new ArrayList<MatchGroup>();
		
		matches = new ArrayList<MatchSet>();
	}
	
	public static MatchRandomizer getRandomizer()
	{
		return randomizer;
	}
	
	protected void checkParameters()
	throws MatchException 
	{
	}   
	
	/**
	 * Add a match group to the match.
	 * 
	 * @param indicator		Value of the exposure group that indicates this match group.  Example: 
	 * 						0 for referent or 1 for exposed.
	 */
	public void addMatchGroup(String indicator)
	{
		MatchGroup mg = new MatchGroup(indicator);
		mg.groupNumber = this.matchGroupsList.size();
		this.matchGroupMap.put(indicator, mg);
		this.matchGroupsList.add(mg);
	}
	
    public void addPatients(RowReader reader)
    throws Exception
    {
		HashSet<MatchPatient> patientList = new HashSet<MatchPatient>();

        String[] row;
        while ((row = reader.getNextRow()) != null) {
        	MatchPatient patient = this.createPatient(row);
        	
        	if (patient != null) {
	            boolean patientIsNew = patientList.add(patient);
	            if (! patientIsNew)
	            	throw new MatchException("Input cohort contains duplicate patients");
	            
	            if (patient.matchGroup != null) {
	            	patient.matchGroup.add(patient);
	            }
	        }
        }
        
        for (MatchGroup mg: matchGroupMap.values())
        	mg.sort(this.getComparator());
        
        reader.close();
    }	        
    
    protected abstract MatchPatient createPatient(String[] row)
    throws MatchException;
    
    protected Comparator<Object> getComparator() {
    	return MatchPatient.getComparator();
    }

    protected String[] getOutputFields()
    {
    	final String[] outputFields = { "set_num", "pat_id", "group_indicator", "match_distance" };
    	
    	return outputFields;
    }
    
    protected String[] getOutputData(int setNumber, MatchSet ms, MatchPatient p) {
       	final String quoteStr = "\"";
    	String[] outputData = {
				Integer.toString(setNumber),
				quoteStr + p.id + quoteStr,
				quoteStr + p.matchGroup.groupIndicator + quoteStr,
				Double.toString(ms.distance)
		};
    	
    	return outputData;
    }
    
    public void writePatients(RowWriter writer)
    throws Exception
    {
    	writer.writeHeaderRow(this.getOutputFields());
    	
       	int setNumber = 0;
       	
       	for (MatchSet ms: this.matches) {
       		setNumber++;
       		
    		for (MatchPatient patient: ms.patients) {
	    		String[] line = this.getOutputData(setNumber, ms, patient);
 		
	    		writer.writeRow(line);
    		}
        } 
  
        writer.close();
    }
    
    public abstract void match()
    throws Exception;
    
    public void begin() 
	throws Exception
	{
    	if (this.matchGroupsList.size() == 0)
    		throw new MatchException("No match groups specified.");
    	
    	System.out.printf("Beginning %s match with %d match groups\n", 
    			this.description, matchGroupsList.size());

    	for (int i = 0; i < this.matchGroupsList.size(); i++) {
    		MatchGroup mg = matchGroupsList.get(i);
    		
    		if (mg.size() == 0)
    			throw new MatchException("Match group " + mg.groupIndicator + " has no patients");
    		
        	System.out.printf("Match group %s has %d patients\n", mg.groupIndicator, mg.size());
    	}
	}

	protected void collapseMatchSets() {
    	// sort the match sets by the referent patient
    	List<MatchSet> tempMatches = new ArrayList<MatchSet>();
    	tempMatches.addAll(this.matches);
    	Collections.sort(tempMatches, new MatchSetTreatmentPatientIdComparator());

    	// clear the list and start fresh
    	this.matches.clear();
    	
    	// collapse match sets with common treatment patients
    	MatchSet outputMs = tempMatches.get(0);
    	for (MatchSet ms: tempMatches) {
			boolean sameTxPatient = (outputMs.getTreatmentPatient() == ms.getTreatmentPatient());
    		
    		if ((outputMs != ms) && sameTxPatient) {
    			outputMs.patients.add(ms.getNonTreatmentPatient());
    			outputMs.distance += ms.distance;
    			outputMs.matchInfo = String.format("%s // %s", outputMs.matchInfo, ms.matchInfo);
    		}
    		
    		if (! sameTxPatient) {
    			this.matches.add(outputMs);
    			outputMs = ms;
    		}
    	}
    	this.matches.add(outputMs);
    	
    	// clear and keep only the fixed ratio matches, if requested
    	if (this.match.fixedRatio == 1) {
        	tempMatches = new ArrayList<MatchSet>();
        	tempMatches.addAll(this.matches);

        	// clear the list and start fresh
        	this.matches.clear();
        	
        	for (MatchSet ms: tempMatches) {
        		if (ms.patients.size() == this.match.matchRatio + 1)
        			this.matches.add(ms);
        	}
    	}
	}
}
