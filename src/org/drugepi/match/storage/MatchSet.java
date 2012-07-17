/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.match.storage;

import java.util.*;

public class MatchSet {
	public List<MatchPatient> patients;
	public String matchInfo;
	public double distance;
	
//	// !!! hack!!!
//	public boolean first;
	
	
	private int treatmentPatientIndex;
	
	public MatchSet(int numGroups) {
		this.patients = new ArrayList<MatchPatient>();
		this.treatmentPatientIndex = -1;
	}
	
	public void put(MatchPatient p) {
		patients.add(p);
	}
	
	public void putTreatmentPatient(MatchPatient p) {
		patients.add(p);
		treatmentPatientIndex = patients.size() - 1;
	}
	
	public void remove(MatchPatient p) {
		patients.remove(p);
	}
	
	public void removeTreatmentPatient() {
		if (this.treatmentPatientIndex == -1)
			return;
		
		patients.remove(this.treatmentPatientIndex);
		this.treatmentPatientIndex = -1;
	}

	public MatchPatient get(MatchGroup g) {
		return this.getFirst(g);
	}
	
	public MatchPatient getTreatmentPatient() {
		if (this.treatmentPatientIndex == -1)
			return null;
		
		return this.patients.get(treatmentPatientIndex);
	}

	public MatchPatient getNonTreatmentPatient() {
		if (this.treatmentPatientIndex == -1)
			return null;
		
		MatchPatient treatmentPatient = this.getTreatmentPatient();

		for (MatchPatient p: this.patients) {
			if (p != treatmentPatient)
				return p;
		}
		
		return null;
	}

	public MatchPatient getFirst(MatchGroup g) {
		for (MatchPatient mp: this.patients)
			if (mp.matchGroup == g)
				return mp;
		
		return null;
	}

	public List<MatchPatient> getAll(MatchGroup g) {
		List<MatchPatient> p = new ArrayList<MatchPatient>();

		for (MatchPatient mp: this.patients)
			if (mp.matchGroup == g)
				p.add(mp);
		
		return p;
	}

    public boolean allAreUnmatched()
    {
    	for (MatchPatient p: this.patients)
    		if (p.isMatched()) return false;
    	
    	return true;
    }
    
    public boolean allAreMatched()
    {
    	for (MatchPatient p: this.patients)
    		if (p.isUnmatched()) return false;
    	
    	return true;
    }
    
    public boolean someAreMatched()
    {
    	boolean hasMatched = false;
    	boolean hasUnmatched = false;
    	
    	for (MatchPatient p: this.patients) {
    		hasMatched = hasMatched || p.isMatched();
    		hasUnmatched = hasUnmatched || p.isUnmatched();
    		
    		if (hasMatched && hasUnmatched) return true;
    	}
    	
    	return false;
    }
     
    public MatchPatient getFirstMatchedPatient()
    {
    	for (MatchPatient p: this.patients) {
    		if (p.isMatched())
    			return p;
    	}
    	
    	return null;
    }

    public MatchPatient getFirstUnmatchedPatient()
    {
    	for (MatchPatient p: this.patients) {
    		if (p.isUnmatched())
    			return p;
    	}
    	
    	return null;
    }

    
    public boolean someAreMatched(MatchGroup mg)
    {
    	boolean hasMatched;
    	
    	for (MatchPatient p: this.patients) {
    		if (p.matchGroup == mg) {
	    		hasMatched = p.isMatched();
    		
	    		if (hasMatched) return true;
    		}
    	}
    	
    	return false;
    }
    
    public void setAllMatched()
    {
    	for (MatchPatient p: this.patients)
    		p.setStatusMatched();
    }
    
	public String toString() {
		StringBuffer sb = new StringBuffer();
		
		sb.append("[\n");
				
		for (MatchPatient p: patients) {
			
			sb.append(String.format("    [ %s | %s: %1.6f ] \n",
					p.matchGroup.groupIndicator,
					p.id,
					p.getPs()
					));
		}
		
		sb.append(String.format("    --> distance = %1.6f\n", this.distance));
		
		sb.append("]\n");
		return (sb.toString());
	}
}
