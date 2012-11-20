/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.hdps.local;

import org.drugepi.hdps.*;
import org.drugepi.hdps.storage.HdpsPatient;

public class HdpsLocalPatientController extends HdpsPatientController
{
	public HdpsLocalController hdpsController;
	
	public HdpsLocalPatientController(Hdps hdps, HdpsLocalController hdpsController)
    {
    	super(hdps);
    	this.hdpsController = hdpsController;
    }

	public void readPatients()
    throws Exception
    {
//    	patientList = new HashMap<String, HdpsPatient>();

        String[] row;

        nExposed = 0;
        nOutcome = 0;
        while ((row = reader.getNextRow()) != null) {
            String key = row[KEY_COLUMN_NUM];
            HdpsPatient patient = this.hdpsController.getPatientDatabase().get(key);
            if (patient == null) {
            	try {
	            	patient = new HdpsPatient(this.hdps.getNumDimensions());
	            	patient.id = key;
	            	patient.exposed = (Integer.parseInt(row[EXPOSED_COLUMN_NUM]) != 0); 
	            	patient.outcomeDichotomous = (Integer.parseInt(row[OUTCOME_COLUMN_NUM]) != 0);
	            	patient.outcomeCount = (Integer.parseInt(row[OUTCOME_COLUMN_NUM]));
	            	patient.outcomeContinuous = Double.parseDouble(row[OUTCOME_COLUMN_NUM]);
	            	if (reader.getNumColumns() > 3)
	            		patient.followUpTime = Integer.parseInt(row[TIME_COLUMN_NUM]);
	            	else
	            		patient.followUpTime = 1;
            	} catch (Exception e) {
            		throw new HdpsException("Failed to read patient information.  Check formatting and content of patient file.");
            	}
            	
            	ptTotal += patient.followUpTime;
               	if (patient.exposed){
           			nExposed++;
           			ptExposed += patient.followUpTime;
               	}

            	sumOfOutcomes += patient.outcomeContinuous;
            	numEvents += patient.outcomeCount;
            	if (patient.outcomeDichotomous) nOutcome++;

            	this.hdpsController.getPatientDatabase().put(patient);
            }
        }
        this.numPatients = (int) this.hdpsController.getPatientDatabase().count();
        
        if (this.numPatients == 0)
        	throw new HdpsException("No patients read.");
        
        reader.close();
        System.out.printf("NOTE: hd-PS patient read finished.  %d rows read.", 
        		this.hdpsController.getNumPatients());
        System.out.println("");
    }
	
	public void closeController() 
	throws Exception
	{
	}
}

