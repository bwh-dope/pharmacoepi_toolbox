/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.hdps;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;

import org.drugepi.PharmacoepiTool;
import org.drugepi.hdps.storage.*;
import org.drugepi.hdps.storage.comparators.*;
import org.drugepi.util.*;

public abstract class HdpsController extends PharmacoepiTool 
{
	public Hdps hdps;
	
	/*
	 * ===========================================
	 * PROTECTED VARIABLES
	 * ===========================================
	 */
	protected HdpsDimensionController[] dimensionControllers = new HdpsDimensionController[Hdps.MAX_DIMENSIONS];
	protected int numDimensions = 0;
	protected HdpsPatientController patientController;

	protected Map<String, HdpsVariable> variablesToConsider;
	
	/**
	 * Constructor for the hd-PS class using default values for all parameters.
	 */
	public HdpsController(Hdps hdps)
	{
		super();
		this.hdps = hdps;
	}
	
	public abstract void addPatients(RowReader reader)
	throws Exception;
	
	/**
	 * Add a dimension to the hd-PS run, with dimension data stored in a tab-delimited file.
	 * 
	 * @param description	Description of the dimension.
	 * @param filePath		Path of the dimension data file.  The file should contain three columns:
	 * 						patient_id, code, date.  Columns must be stored in this order.
	 * @throws Exception	
	 */
	public void addDimension(String description, String filePath)
	throws Exception
	{
        TabDelimitedFileReader reader = new TabDelimitedFileReader(filePath);
        this.addDimension(description, reader);
	}
	
	/**
	 * Add a dimension to the hd-PS run, with dimension data stored in a database.
	 * 
	 * @param description	Description of the dimension.
	 * @param dbDriverClass	Name of the database driver.
	 * @param dbURL			JDBC URL of the database
	 * @param dbUser		Database user name.
	 * @param dbPassword	Database user's password.
	 * @param dbQuery		The query that will result in the dimension data.  The query should return 
	 * 						three columns: patient_id, code, date.  Columns must be returned in this order.
	 * @throws Exception
	 */
	public void addDimension(String description, String dbDriverClass, 
							String dbURL, String dbUser, String dbPassword,
							String dbQuery)
	throws Exception
	{
		DatabaseRowReader reader = new DatabaseRowReader(dbDriverClass, dbURL, dbUser, dbPassword, dbQuery);
        this.addDimension(description, reader);
	}
		
	/*
	 * ===========================================
	 * PROTECTED METHODS
	 * ===========================================
	 */
	protected abstract void addDimension(String description, RowReader reader);

	protected void readDimensions()
	throws Exception
	{
	    ExecutorService executor;
	    
        this.variablesToConsider = new ConcurrentHashMap<String, HdpsVariable>(); 
        
        executor = Executors.newFixedThreadPool(this.numDimensions);
        for (int i = 0; i < this.numDimensions; i++) {
            final int threadNum = i;
            final HdpsController hdpsController = this;

            Runnable task = new Runnable() {
                public void run() {
                    try {
        		        hdpsController.dimensionControllers[threadNum].readDimension();
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            };

            executor.submit(task);
        }

        try {
            executor.shutdown();
            executor.awaitTermination(100 * 60 * 60, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        this.takeDimensionDoneActions();
    }
	
	public void writeDimensionInfoFile(String outputFileName) throws Exception {
		final String[] outputFields = HdpsCode.outputFieldNames;

		TabDelimitedFileWriter outputWriter = new TabDelimitedFileWriter(
				Utils.getFilePath(this.hdps.tempDirectory, outputFileName),
				outputFields);

		for (int i = 0; i < this.numDimensions; i++) {
			HdpsDimensionController dim = this.dimensionControllers[i];
			dim.writeCodes(outputWriter);
		}

		outputWriter.close();
	}
		
	protected void writeVariableInfoFile(String fileName, List<HdpsVariable> vars)
	throws Exception
	{
		String[] outputFields = HdpsVariable.outputFieldNames; 
		RowWriter outputWriter = new TabDelimitedFileWriter(
        		Utils.getFilePath(hdps.tempDirectory, fileName), outputFields);
 
        for (HdpsVariable var: vars) {
        	String[] outputContents = var.toStringArray(); 
	        outputWriter.writeRow(outputContents);
        }
        outputWriter.close();
	}
		
	protected abstract void generateCohorts(List<HdpsVariable> variablesToOutput)
	throws Exception;
	
	public boolean isRankedByExposureAssoc() {
		return (this.hdps.variableRankingMethod.equalsIgnoreCase(Hdps.RANKING_METHOD_EXP));
	}

	public boolean isRankedByOutcomeAssoc() {
		return (this.hdps.variableRankingMethod.equalsIgnoreCase(Hdps.RANKING_METHOD_OUTCOME));
	}

	public boolean isRankedByBias() {
		return (this.hdps.variableRankingMethod.equalsIgnoreCase(Hdps.RANKING_METHOD_BIAS));
	}

	public boolean outcomeIsDichotomous() {
		return ((this.hdps.outcomeType.equalsIgnoreCase(Hdps.OUTCOME_TYPE_BINARY)) ||
				(this.hdps.outcomeType.equalsIgnoreCase(Hdps.OUTCOME_TYPE_DICHOTOMOUS)));
	}

	public boolean outcomeIsCount() {
		return (this.hdps.outcomeType.equalsIgnoreCase(Hdps.OUTCOME_TYPE_COUNT));
	}

	public boolean outcomeIsContinuous() {
		return (this.hdps.outcomeType.equalsIgnoreCase(Hdps.OUTCOME_TYPE_CONTINUOUS));
	}
	
	protected void checkParams()
	throws Exception
	{
		if (hdps.tempDirectory == null)
			throw new HdpsException("Temp directory not specified.");
		
		if ((! this.isRankedByBias()) &&
			(! this.isRankedByExposureAssoc()) &&
			(! this.isRankedByOutcomeAssoc()))
			throw new HdpsException("Must specify a valid variable ranking method");

		File tempDir = new File(hdps.tempDirectory);
		if (! tempDir.exists())
			throw new HdpsException("Specified directory does not exist.");
		if (! tempDir.isDirectory())
			throw new HdpsException("Specified directory is not a directory.");

		if ((this.hdps.exposureOnlyScreen == 1) && (! this.isRankedByExposureAssoc()))
			throw new HdpsException("Cannot specify exposure only screen and a non-exposure variable ranking.");
		
		if (hdps.numDimensions == 0)
        	throw new HdpsException("No dimensions specified.");
	}
	
	protected List<HdpsVariable> selectVariables()
	throws Exception 
	{
	    List<HdpsVariable> rankedVariables = 
	            new ArrayList<HdpsVariable>(this.variablesToConsider.values());

	    	for (HdpsVariable var: rankedVariables) {
	            if ((this.isRankedByExposureAssoc()) || 
	                	(hdps.exposureOnlyScreen > 0))
	               	var.activeRankingVariable = var.expAssocRankingVariable;
	            else if (this.isRankedByOutcomeAssoc()) 
	               	var.activeRankingVariable = var.outcomeAssocRankingVariable;
	            else if (this.isRankedByBias()) 
	               	var.activeRankingVariable = var.biasRankingVariable;
	            else
	            	var.activeRankingVariable = 0;
	    	}
       	Collections.sort(rankedVariables, new HdpsVariableRankingComparator());

    	List<HdpsVariable> selectedVariables = new ArrayList<HdpsVariable>();
    	for (HdpsVariable var: rankedVariables) {
    		if (selectedVariables.size() >= hdps.k)
    			break;

    		if (var.activeRankingVariable != HdpsVariable.INVALID) {
    			var.selectedForPs = true;
    			selectedVariables.add(var);
    		}
    	}
		
    	return selectedVariables;
	}
	
	protected List<HdpsVariable> getRequestedVariables(List<HdpsVariable> selectedVariables)
	throws Exception 
	{
		List<HdpsVariable> additionalVariables = new ArrayList<HdpsVariable>();
		
		if (this.hdps.requestedVariables.size() > 0) {
			Hashtable<String, HdpsVariable> h = new Hashtable<String, HdpsVariable>();
			// create a hash of all possible variables
			for (HdpsVariable var: this.variablesToConsider.values()) {
				h.put(var.getHashValue(),  var);
			}
			
			// remove those already selected
			for (HdpsVariable var: selectedVariables) {
				h.remove(var.getHashValue());
			}
			
			// find those not selected that have been requested
			for (String requestedVarHash: this.hdps.requestedVariables) {
				HdpsVariable var = h.remove(requestedVarHash);
				if (var != null) 
					additionalVariables.add(var);
			}
		}
		
		System.out.printf("NOTE: %d variables were requested for output; %d were added\n", 
				hdps.requestedVariables.size(), additionalVariables.size());
		
		return additionalVariables;
	}

	protected void doOutput()
	throws Exception
	{
		List<HdpsVariable> selectedVariables = this.selectVariables();
		// add in any requested variables to the output
		selectedVariables.addAll(this.getRequestedVariables(selectedVariables));
    	
    	ZBiasCalculator.scoreVariables(selectedVariables);

    	// sort all of the variables alphabetically and output
    	List<HdpsVariable> variablesForOutput = new ArrayList<HdpsVariable>();
    	variablesForOutput.addAll(this.variablesToConsider.values());
    	Collections.sort(variablesForOutput, new HdpsVariableNameComparator());
    	this.writeVariableInfoFile("output_all_vars.txt", variablesForOutput);
    	this.writeDimensionInfoFile("output_dimension_codes.txt");
    	
    	// sort the PS variables alphabetically 
        Collections.sort(selectedVariables, new HdpsVariableNameComparator());
       	this.generateCohorts(selectedVariables);
	}

	protected abstract void startHdps()
	throws Exception;
	
	protected abstract void takeDimensionDoneActions()
	throws Exception;
	
	protected abstract void closeController()
	throws Exception;
	
	/**
	 * Begin execution of the hd-PS algorithm.
	 * 
	 * @throws Exception
	 */
	public void run()
    throws Exception
    {
		this.startTool();
		try {
			this.checkParams();
			
	        System.out.println("NOTE: hd-PS initializing.");
			this.startHdps();
			
			System.out.println("NOTE: hd-PS reading patients.");
	        this.patientController.readPatients();

			if (this.getNumPatients() == 0)
	        	throw new HdpsException("No patients added.");
	        
	        System.out.println("NOTE: hd-PS building dimensions.");
	        this.readDimensions();

	        System.out.println("NOTE: hd-PS beginning output.");
	        this.doOutput();
	        
	        System.out.println("NOTE: hd-PS cleaning up.");
	        this.closeController();
		} catch (Exception e) {
			e.printStackTrace();
		}
        this.endTool();
    }
	
	/**
	 * @return		The number of patients read from the patient input file or database.
	 */
	public abstract int getNumPatients();
	
	public HdpsPatientController getPatientController() {
		return patientController;
	}
}

