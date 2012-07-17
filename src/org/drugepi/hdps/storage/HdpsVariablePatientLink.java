/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.hdps.storage;

import com.sleepycat.persist.model.*;

@Entity
public class HdpsVariablePatientLink {
	@PrimaryKey
	public String id;
	
	@SecondaryKey(relate=Relationship.MANY_TO_ONE)
	public String variableId;

	@SecondaryKey(relate=Relationship.MANY_TO_ONE)
	public String patientId;
	
	public int value;
	
	public HdpsVariablePatientLink() {
		super();
	}
	
	public HdpsVariablePatientLink(String variableId, String patientId,
			int value) {
		super();
		this.variableId = variableId;
		this.patientId = patientId;
		this.value = value;
	}
	
	public String getId()
	{
		return(this.variableId + "|" + this.patientId);
	}

	public void setId()
	{
		this.id = this.variableId + "|" + this.patientId;
	}

	
	public static String generateId(HdpsVariable variable, HdpsPatient patient) {
		return(variable.varName + "|" + patient.id);
	}

	public String getVariableId() {
		return variableId;
	}

	public void setVariableId(String variableId) {
		this.variableId = variableId;
	}

	public String getPatientId() {
		return patientId;
	}

	public void setPatientId(String patientId) {
		this.patientId = patientId;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}
	
}
