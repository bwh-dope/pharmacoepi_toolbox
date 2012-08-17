/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.hdps.local;

import java.util.*;

import org.drugepi.hdps.*;
import org.drugepi.hdps.storage.*;
import org.drugepi.util.*;

import com.sleepycat.persist.*;

public class HdpsLocal extends HdpsController
{
	/*
	 * ===========================================
	 * PROTECTED VARIABLES
	 * ===========================================
	 */
	protected HdpsLocalDatabase database;
	
	/**
	 * Constructor for the hd-PS class using default values for all parameters.
	 */
	public HdpsLocal(Hdps hdps)
	{
		super(hdps);
	}
	
	public void addPatients(RowReader reader)
	throws Exception
	{
		patientController = new HdpsLocalPatientController(this.hdps, this);
		patientController.reader = reader;
	}

		
	/*
	 * ===========================================
	 * PROTECTED METHODS
	 * ===========================================
	 */
	
	protected void addDimension(String description, RowReader reader)
	{
		dimensionControllers[numDimensions] = new HdpsLocalDimensionController(this.hdps, this);
		dimensionControllers[numDimensions].dimensionId = numDimensions + 1;
		dimensionControllers[numDimensions].dimensionDescription = description;
		dimensionControllers[numDimensions].reader = reader;
		dimensionControllers[numDimensions].patientController = patientController;

		this.numDimensions++;
	}
	
	protected void takeDimensionDoneActions()
	throws Exception
	{
		for (int i = 0; i < this.hdps.getNumDimensions(); i++) {
			HdpsLocalDimensionController c = 
				(HdpsLocalDimensionController) this.dimensionControllers[i];
			
	        this.variablesToConsider.putAll(c.getVariablesToConsider());
		}
	}
	
	protected void generateSparseOutput(List<HdpsVariable> variablesToOutput)
	throws Exception
	{
        String[] outputFields = new String[2];
       	outputFields[0] = "patient_id";
       	outputFields[1] = "var_list";

       	RowWriter outputWriter = new TabDelimitedFileWriter(
       			Utils.getFilePath(hdps.tempDirectory, hdps.sparseOutputFilename),
       			outputFields);
		
        final String quoteStr = "\"";
        final String commaStr = ",";

		Map<String, HdpsCodePatientLink> codeMap = new HashMap<String, HdpsCodePatientLink>();
		EntityCursor<HdpsPatient> cursor = this.getPatientDatabase().entities();
        for (HdpsPatient patient: cursor) { 
        	outputFields[0] = quoteStr + patient.id + quoteStr;
        	
        	StringBuffer varListString = new StringBuffer(quoteStr);
        	
        	codeMap.clear();
        	EntityCursor<HdpsCodePatientLink> c = 
        		this.getCodePatientLinkByPatientLookup().subIndex(patient.id).entities();
        	for (HdpsCodePatientLink cpl: c)
        		codeMap.put(cpl.codeId, cpl);
        	c.close();
        	
        	for (HdpsVariable var: variablesToOutput) {
        		// get whether this patient has this code
        		HdpsCodePatientLink codePatientLink = codeMap.get(var.code.id);
        		
        		if (codePatientLink != null) {
	        		int value = codePatientLink.getValueForVarType(var.type);
	
	        		if (value == HdpsVariable.valueOne) {
						if (varListString.length() > 1)
							varListString.append(commaStr);
						varListString.append(var.varName);
	        		}
        		}
        	}
        	
        	varListString.append(quoteStr);
        	outputFields[1] = varListString.toString();
        	
	        outputWriter.writeRow(outputFields);
        }	
        cursor.close();
        
		System.out.printf("NOTE: hd-PS wrote %d patients to sparse output cohort.\n",
				this.getPatientDatabase().count());
        outputWriter.close();
	}
	
	protected void generateFullOutput(List<HdpsVariable> variablesToOutput)
	throws Exception
	{
        String[] outputFields = new String[variablesToOutput.size() + 1];
       	outputFields[0] = "patient_id";
       	
       	int k = 1;
       	for (HdpsVariable var: variablesToOutput) {
       		outputFields[k++] = var.varName;
       	}

       	RowWriter outputWriter = new TabDelimitedFileWriter(
       			Utils.getFilePath(hdps.tempDirectory, hdps.fullOutputFilename),
       			outputFields);
		
        final String oneStr = "1";
        final String zeroStr = "0";
        //final String missingStr = "";
        final String quoteStr = "\"";
      
		Map<String, HdpsCodePatientLink> codeMap = new HashMap<String, HdpsCodePatientLink>();
		EntityCursor<HdpsPatient> cursor = this.getPatientDatabase().entities();
        for (HdpsPatient patient: cursor) 
        {
        	outputFields[0] = quoteStr + patient.id + quoteStr;
        	int fieldIndex = 1;
        	
        	// load all variables for this patient into memory
        	codeMap.clear();
        	EntityCursor<HdpsCodePatientLink> c = 
        		this.getCodePatientLinkByPatientLookup().subIndex(patient.id).entities();
        	for (HdpsCodePatientLink cpl: c)
        		codeMap.put(cpl.codeId, cpl);
        	c.close();
        	
        	for (HdpsVariable var: variablesToOutput) {
        		// default of 0
        		outputFields[fieldIndex] = zeroStr;

        		// get whether this patient has this variable
        		HdpsCodePatientLink cpl = codeMap.get(var.code.id);
        		if (cpl != null) {
        			int varValue = HdpsVariable.valueZero;
        			
//        			if (var.isTypeAny()) 
//        				varValue = cpl.anyVarValue;
        			if (var.isTypeOnce()) 
        				varValue = cpl.onceVarValue;
        			else if (var.isTypeSporadic()) 
        				varValue = cpl.sporadicVarValue;
        			else if (var.isTypeFrequent()) 
        				varValue = cpl.frequentVarValue;
        			else if (var.isTypeSpecial()) 
        				varValue = cpl.specialVarValue;
        			
					if (varValue == HdpsVariable.valueMissing) 
						outputFields[fieldIndex] = zeroStr;
					else if (varValue == HdpsVariable.valueOne)
						outputFields[fieldIndex] = oneStr;
        		} 
         		fieldIndex++;
        	}
        	
	        outputWriter.writeRow(outputFields);
        }	
        cursor.close();
        
		System.out.printf("NOTE: hd-PS wrote %d patients to full output cohort.\n",
				this.getPatientDatabase().count());
        outputWriter.close();
	}	
	
	protected void generateCohorts(List<HdpsVariable> variablesToOutput)
	throws Exception
	{
        if (hdps.doFullOutput == 1)
        	this.generateFullOutput(variablesToOutput);

        if (hdps.doSparseOutput == 1)
        	this.generateSparseOutput(variablesToOutput);
	}
		
	protected void checkParams()
	throws Exception
	{
		super.checkParams();
	}
	
	protected void startHdps()
	throws Exception
	{
		this.database = new HdpsLocalDatabase(hdps.tempDirectory);
	}
	
	protected void closeController()
	throws Exception
	{
        this.database.close();
	}
	
	/**
	 * @return		The number of patients read from the patient input file or database.
	 */
	public int getNumPatients() {
		if (this.getPatientDatabase() == null)
			return 0;
		
		return (int) this.getPatientDatabase().count();
	}
	
	public PrimaryIndex<String, HdpsPatient> getPatientDatabase() {
		return this.database.patientById;
	}

	public PrimaryIndex<String, HdpsCodePatientLink> getCodePatientLinkDatabase() {
		return database.codePatientLinkById;
	}

	public SecondaryIndex<String, String, HdpsCodePatientLink> getCodePatientLinkByCodeLookup() {
		return database.codePatientLinkByCode;
	}

	public SecondaryIndex<String, String, HdpsCodePatientLink> getCodePatientLinkByPatientLookup() {
		return database.codePatientLinkByPatient;
	}	
}

