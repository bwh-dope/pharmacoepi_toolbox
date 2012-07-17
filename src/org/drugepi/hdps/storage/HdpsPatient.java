/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.hdps.storage;

import com.sleepycat.persist.model.*;

@Entity
public class HdpsPatient implements Comparable<HdpsPatient> {
	@PrimaryKey
	public String id;
	public boolean exposed;
	public boolean outcomeDichotomous;
	public int outcomeCount;
	public double outcomeContinuous;
	public int followUpTime;
	
	public HdpsPatient() {
		this(0);
	}
	
	public HdpsPatient(int numDimensions) {
		super();
		
//		numCodes = new int[numDimensions];
//		numUniqueCodes = new int[numDimensions];
	}
	
	public int compareTo(HdpsPatient pat) {
		return (id.compareTo(pat.id));
	}

	public String getId() {
		return id;
	}

	public boolean isExposed() {
		return exposed;
	}

	public boolean isOutcome() {
		return outcomeDichotomous;
	}
}
