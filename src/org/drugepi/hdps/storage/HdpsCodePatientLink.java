/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.hdps.storage;

import static com.sleepycat.persist.model.Relationship.MANY_TO_ONE;

import org.drugepi.hdps.local.HdpsLocal;

import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.model.*;


@Entity
public class HdpsCodePatientLink {
//    public HdpsPatient patient;
//	public Map<Class<?>, HdpsVarEntry> patientVarEntries;

	@PrimaryKey
	public String id;
	
	@SecondaryKey(relate=MANY_TO_ONE)
	public String patientId;

	@SecondaryKey(relate=MANY_TO_ONE)
	public String codeId;
	
	public int numOccurrences;
	
	public int onceVarValue;
	public int sporadicVarValue;
	public int frequentVarValue;
	public int anyVarValue;
	public int specialVarValue;

	public HdpsCodePatientLink() {
    	super();
    }

	public String getId()
	{
		return(this.codeId + "|" + this.patientId);
	}
	
	public static String generateId(HdpsCode code, HdpsPatient patient) {
		return(code.id + "|" + patient.id);
	}
	
	// get all records whose id starts with the code id
	// may screw up if code id's aren't the first part of the key
	// hack.
	public static EntityCursor<HdpsCodePatientLink> getCursorForCodeId(HdpsLocal hdps, String codeId)
	{
		// get all everything that begins with the noted code id
		// \u007e is the last ascii character
		return(hdps.getCodePatientLinkDatabase().entities(
				codeId + "|", false, codeId + "|\u007e", true));
	}
	
    public int getValueForVarType(String type)
    {
    	if (type.equals(HdpsVariable.kOnceVarType))
    		return this.onceVarValue;
    	
    	if (type.equals(HdpsVariable.kSporadicVarType))
    		return this.sporadicVarValue;

    	if (type.equals(HdpsVariable.kFrequentVarType))
    		return this.frequentVarValue;

    	return -1;
    }
}
