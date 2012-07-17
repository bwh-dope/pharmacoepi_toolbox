/*
	The contents of this file are subject to the Mozilla Public License Version 
	1.1 (the "License"); you may not use this file except in compliance with
	the License.  You may obtain a copy of the License at http://www.mozilla.org/MPL.
	
	The Original Code is the DoPE Pharmacoepidemiology Toolbox.
	
	The Initial Developer of the Original Code is the Brigham and Women's Hospital 
	Division of Pharmacoepidemiology.
	
	Contributor(s):
	   Jeremy A. Rassen <jrassen@post.harvard.edu>
 */

package org.drugepi.hdps.db;

import java.sql.*;
import java.util.*;

import org.drugepi.hdps.*;
import org.drugepi.hdps.storage.*;
import org.drugepi.util.*;

public class HdpsDb extends HdpsController
{
	private Connection connection;
	protected String varTableName;
	protected String varTableIdSequenceName;
	protected String patientVarTableName;
	
	protected String randomSuffix;
	
	private Map<Integer, HdpsDimensionController> dimensionsMap;
	
	/**
	 * Constructor for the hd-PS class using default values for all parameters.
	 */
	public HdpsDb(Hdps hdps)
	{
		super(hdps);
		dimensionsMap = new HashMap<Integer, HdpsDimensionController>();
		randomSuffix = SqlUtils.generateRandomName();
	}
	
	public void addPatients(RowReader reader)
	throws Exception
	{
		patientController = new HdpsDbPatientController(this.hdps, this); 
		patientController.reader = reader;
	}
	
	/*
	 * ===========================================
	 * PROTECTED METHODS
	 * ===========================================
	 */
	protected void addDimension(String description, RowReader reader)
	{
		dimensionControllers[numDimensions] = new HdpsDbDimensionController(this.hdps, this); 
		dimensionControllers[numDimensions].dimensionId = numDimensions + 1;
		dimensionControllers[numDimensions].dimensionDescription = description;
		dimensionControllers[numDimensions].reader = reader;
		dimensionControllers[numDimensions].patientController = patientController;

		dimensionsMap.put(dimensionControllers[numDimensions].dimensionId, 
						  dimensionControllers[numDimensions]);
		
		this.numDimensions++;
	}
	
	protected void takeDimensionDoneActions()
	throws Exception
	{
		this.calculateBias();
		this.updateVarNames();

		for (int i = 0; i < hdps.getNumDimensions(); i++) {
			HdpsDbDimensionController c = 
				(HdpsDbDimensionController) dimensionControllers[i];
			
			if (c != null)
				c.updateVariablesToConsider();
		}

		Statement s = this.connection.createStatement();
		String sql = String.format(
				"SELECT * " +
				"FROM %s " +
				"WHERE consider_for_ps = 1",
				this.varTableName
		);
		ResultSet r = SqlUtils.executeSqlQuery(s, sql);

		while (r.next()) {
			String codeName = r.getString("code");
			HdpsCode code = new HdpsCode(codeName);
			code.dimension = this.dimensionsMap.get(r.getInt("dimension_id"));
			if (codeName != null)
				code.codeString = codeName;
			else
				code.codeString = "Other";
			
			HdpsVariable var = new HdpsVariable(code, r);
			var.code = code;
			this.variablesToConsider.put(var.varName, var);
		}
		r.close();
		s.close();
	}

	
	protected void updateAggregateField(Statement s, String aggField, String aggQuery)
	throws Exception
	{
		String sql = String.format(
				"UPDATE %s A " +
				"SET A.%s = B.the_value " +
				"FROM (%s) B " +
				"WHERE A.var_id = B.var_id",
				this.varTableName,
				aggField,
				aggQuery
			);
		s.addBatch(sql);
	}
	
	protected void scaleVariables()
	throws Exception
	{
		HdpsDbPatientController pc = (HdpsDbPatientController) this.patientController;
		
		Statement s = this.connection.createStatement();
		// NULLIF avoids division by 0 (will yield NULL)
		String sql = String.format(
			"UPDATE %s A " +
			"SET A.mean = sumValue / NULLIF(C.n_patients, 0) " +
			"FROM ( " +
			"    SELECT var_id, " +
			"           SUM(var_value) AS sumValue " +
			"     FROM %s P, %s Q " +
			"	  WHERE P.patient_id = Q.%s " +
			"     GROUP BY var_id " +
			") AS B, " +
			"( " +
			"   SELECT COUNT(*) AS n_patients" +
			"	FROM %s" +
			") AS C " +
			"WHERE A.var_id = B.var_id",
			this.varTableName,
			patientVarTableName,
			pc.getPatientViewName(),
			pc.getPatientIdFieldName(),
			pc.getPatientViewName()
		);
		System.out.println(sql);
		s.execute(sql);

		// variance is SUM(x^2) - n(mean ^ 2)
		sql = String.format(
				"UPDATE %s A " +
				"SET A.variance = (sumSquares - (n_patients * (A.mean ^ 2))) / n_patients " +
				"FROM ( " +
				"    SELECT var_id, " +
				"           SUM(var_value ^ 2) AS sumSquares  " +
				"     FROM %s P, %s Q " +
				"	  WHERE P.patient_id = Q.%s " +
				"     GROUP BY var_id " +
				") AS B, " +
				"( " +
				"   SELECT COUNT(*) AS n_patients" +
				"	FROM %s" +
				") AS C " +
				"WHERE A.var_id = B.var_id",
				this.varTableName,
				patientVarTableName,
				pc.getPatientViewName(),
				pc.getPatientIdFieldName(),
				pc.getPatientViewName()
			);
		System.out.println(sql);
		s.execute(sql);		
		
		sql = String.format(
			"UPDATE %s A " +
			"SET A.standardized_value = (A.var_value - B.mean) / NULLIF(SQRT(B.variance), 0) " +
			"FROM %s B " +
			"WHERE A.var_id = B.var_id",
			this.patientVarTableName, 
			this.varTableName
		);
		System.out.println(sql);
		s.execute(sql);
		s.close();
	}
	
	protected void updateVarNames()
	throws Exception 
	{
		// update the variable table with names based on the ordering
		// of the codes
		Statement s = this.connection.createStatement();
		String sql = String.format(
				"UPDATE %s A " +
				"SET var_name = RTRIM('D' || A.dimension_id || 'V' || B.ranking || A.type) " +
				"FROM ( " +
				"    SELECT var_id, " +
				"	        RANK() OVER (PARTITION BY dimension_id ORDER BY code DESC) AS ranking " +
				"    FROM %s " +
				") AS B " +
				"WHERE A.var_id = B.var_id ",
				this.varTableName,
				this.varTableName
			);
		System.out.println(sql);
		s.execute(sql);
		s.close();
	}
	
	protected void calculateBias()
	throws Exception 
	{
		HdpsDbPatientController pc = (HdpsDbPatientController) this.patientController;
		
		Statement s = this.connection.createStatement();
		String sql;
		
		// two ways to require additive handling of data:
		// the outcome is continuous, or any confounder is continuous
		boolean additiveMode = this.outcomeIsContinuous();
		if (! additiveMode) {
			sql = String.format(
					"SELECT COUNT(*) AS c FROM %s WHERE is_dichotomous = 0",
					this.varTableName
				);
			ResultSet r = s.executeQuery(sql);
			r.next();
			additiveMode = (r.getInt("c") > 0);
		}
		
		if (additiveMode) 
			this.scaleVariables();

		sql = String.format(
				"UPDATE %s " +
				"SET n = (SELECT COUNT(*) FROM %s)," +
				"    pt = (SELECT SUM(%s) FROM %s), " +
				"    e1 = (SELECT COUNT(*) FROM %s WHERE %s = 1), " +
				"    d1 = (SELECT COUNT(*) FROM %s WHERE %s >= 1), " +
				"    pt_e1 = (SELECT SUM(%S) FROM %s WHERE %s = 1), " +
				"    pt_e0 = (SELECT SUM(%s) FROM %s WHERE %s = 0), " +
				"    num_events = (SELECT SUM(%s) FROM %s), " +
				"    c1 = 0, " +
				"    e1c1 = 0, " +
				"    e0c1 = 0, " +
				"    d1c1 = 0, " +
				"    d0c1 = 0, " +
				"	 rr_cd = %f, " +
				"	 rr_ce = %f, " +
				"	 bias = %f, " +
				" 	 outcome_assoc_ranking_var = %f, " +
				" 	 exp_assoc_ranking_var = %f, " +
				"	 bias_ranking_var = %f",
				this.varTableName,				// UPDATE
				pc.getPatientViewName(),		// n
				pc.getPersonTimeFieldName(),	// pt
				pc.getPatientViewName(),		// pt
				pc.getPatientViewName(),		// e1
				pc.getExposureFieldName(),		// e1
				pc.getPatientViewName(),		// d1
				pc.getOutcomeFieldName(),		// d1
				pc.getPersonTimeFieldName(),	// pt_e1
				pc.getPatientViewName(),		// pt_e1
				pc.getExposureFieldName(),		// pt_e1
				pc.getPersonTimeFieldName(),	// pt_e0
				pc.getPatientViewName(),		// pt_e0
				pc.getExposureFieldName(),		// pt_e0
				pc.getOutcomeFieldName(),		// num_events
				pc.getPatientViewName(),		// num_events
				HdpsVariable.INVALID, HdpsVariable.INVALID, HdpsVariable.INVALID, 
				HdpsVariable.INVALID, HdpsVariable.INVALID, HdpsVariable.INVALID 
			);
		SqlUtils.addToSqlBatch(s, sql);
		
		this.updateAggregateField(s, "c1", 
				String.format(
						"SELECT var_id, count(DISTINCT patient_id) AS the_value " +
						"FROM %s " +
						"GROUP BY var_id",
						patientVarTableName
				));
		
		this.updateAggregateField(s, "e1c1", 
				String.format(
						"SELECT var_id, count(DISTINCT patient_id) AS the_value " +
						"FROM %s A, %s B " +
						"WHERE A.patient_id = B.%s " +
						"      AND B.%s = 1 " +
						"GROUP BY var_id",
						patientVarTableName,
						pc.getPatientViewName(),
						pc.getPatientIdFieldName(),
						pc.getExposureFieldName()
				));
	
		this.updateAggregateField(s, "e0c1", 
				String.format(
						"SELECT var_id, count(DISTINCT patient_id) AS the_value " +
						"FROM %s A, %s B " +
						"WHERE A.patient_id = B.%s " +
						"      AND B.%s = 0 " +
						"GROUP BY var_id",
						patientVarTableName,
						pc.getPatientViewName(),
						pc.getPatientIdFieldName(),
						pc.getExposureFieldName()
				));
	
		
		this.updateAggregateField(s, "d1c1", 
				String.format(
						"SELECT var_id, count(DISTINCT patient_id) AS the_value " +
						"FROM %s A, %s B " +
						"WHERE A.patient_id = B.%s " +
						"      AND B.%s = 1 " +
						"GROUP BY var_id",
						patientVarTableName,
						pc.getPatientViewName(),
						pc.getPatientIdFieldName(),
						pc.getOutcomeFieldName()
				));
	
		this.updateAggregateField(s, "d0c1", 
				String.format(
						"SELECT var_id, count(DISTINCT patient_id) AS the_value " +
						"FROM %s A, %s B " +
						"WHERE A.patient_id = B.%s " +
						"      AND B.%s = 0 " +
						"GROUP BY var_id",
						patientVarTableName,
						pc.getPatientViewName(),
						pc.getPatientIdFieldName(),
						pc.getOutcomeFieldName()
				));

		this.updateAggregateField(s, "c1_num_events", 
				String.format(
						"SELECT var_id, SUM(B.%s) AS the_value " +
						"FROM %s A, %s B " +
						"WHERE A.patient_id = B.%s " +
						"GROUP BY var_id",
						pc.getOutcomeFieldName(),
						patientVarTableName,
						pc.getPatientViewName(),
						pc.getPatientIdFieldName()
				));
		
		this.updateAggregateField(s, "pt_c1", 
				String.format(
						"SELECT var_id, SUM(B.%s) AS the_value " +
						"FROM %s A, %s B " +
						"WHERE A.patient_id = B.%s " +
						"GROUP BY var_id",
						pc.getPersonTimeFieldName(),
						patientVarTableName,
						pc.getPatientViewName(),
						pc.getPatientIdFieldName()
				));
		
		sql = String.format(
				"UPDATE %s " +
				"SET e0 = n - e1," +
				"    d0 = n - d1," +
				"    c0 = n - c1",
				this.varTableName
			);
		SqlUtils.addToSqlBatch(s, sql);
		
		sql = String.format(
				"UPDATE %s " +
				"SET e1c0 = e1 - e1c1," +
				"    e0c0 = e0 - e0c1," +
				"    d1c0 = d1 - d1c1," +
				"    d0c0 = d0 - d0c1, " +
				"    pt_c0 = pt - pt_c1, " +
				"    c0_num_events = num_events - c1_num_events ",
				this.varTableName
			);
		SqlUtils.addToSqlBatch(s, sql);

		sql = String.format(
				"UPDATE %s " +
				"SET pc_e1 = e1c1 / e1," +
				"    pc_e0 = e0c1 / e0",
				this.varTableName
			);
		SqlUtils.addToSqlBatch(s, sql);

		sql = String.format(
				"UPDATE %s " +
				"SET pc_e1 = 1.00000000000 - pc_e1 " +
				"WHERE pc_e1 > 0.5",
				this.varTableName
			);
		SqlUtils.addToSqlBatch(s, sql);
	
		sql = String.format(
				"UPDATE %s " +
				"SET pc_e0 = 1.00000000000 - pc_e0 " +
				"WHERE pc_e0 > 0.5",
				this.varTableName
			);
		SqlUtils.addToSqlBatch(s, sql);
				
		if (! additiveMode) {
			sql = String.format(
					"UPDATE %s " +
					"SET rr_ce = pc_e1 / pc_e0 " +
					"WHERE pc_e1 > 0 AND pc_e0 > 0",
					this.varTableName
				);
			SqlUtils.addToSqlBatch(s, sql);

			// exp(abs(ln(rr_cd))) will make all rr_cd > 1.0
			if (this.hdps.useOutcomeZeroCellCorrection == 1) 
				sql = String.format(
						"UPDATE %s " +
						"SET rr_cd = exp(abs(ln(((c1_num_events + 0.1) / (pt_c1 + 0.1)) / ((c0_num_events + 0.1) / (pt_c0 + 0.1))))) ",
						this.varTableName
					);
			else
				sql = String.format(
						"UPDATE %s " +
						"SET rr_cd = exp(abs(ln((c1_num_events / pt_c1) / (c0_num_events / pt_c0)))) " +
						"WHERE c1_num_events > 0 AND pt_c1 > 0 AND c0_num_events > 0 AND pt_c0 > 0",
						this.varTableName
					);
			SqlUtils.addToSqlBatch(s, sql);
			
			sql = String.format(
					"UPDATE %s " +
					"SET exp_assoc_ranking_var = abs(ln(pc_e1 / pc_e0)) " +
					"WHERE pc_e1 > 0 AND pc_e0 > 0",
					this.varTableName
				);
			SqlUtils.addToSqlBatch(s, sql);
		
			sql = String.format(
					"UPDATE %s " +
					"SET outcome_assoc_ranking_var = abs(ln(rr_cd)) " +
					"WHERE rr_cd > 0",
					this.varTableName
				);
			SqlUtils.addToSqlBatch(s, sql);
			
			sql = String.format(
					"UPDATE %s " +
					"SET bias = ((pc_e1 * (rr_cd - 1.0)) + 1.0) / " +
					"           ((pc_e0 * (rr_cd - 1.0)) + 1.0) " +
					"WHERE pc_e1 > 0 AND pc_e0 > 0 AND rr_cd > 0",
					this.varTableName
				);
			SqlUtils.addToSqlBatch(s, sql);
		} 
		
		if (additiveMode) {
			// regression coefficients for continuous confounder or outcome
			// requires that variables have been standardized already
			// see http://en.wikipedia.org/wiki/Ordinary_least_squares#Simple_regression_model
			// for formula
			this.updateAggregateField(s, "ce_regression_beta",
					String.format(
						"SELECT var_id, SUM(standardized_value * %s) AS the_value " +
						"FROM %s A, %s B " +
						"WHERE A.patient_id = B.%s " +
						"GROUP BY var_id",
						pc.getExposureFieldName(),
						patientVarTableName,
						pc.getPatientViewName(),
						pc.getPatientIdFieldName()
					));
			
			this.updateAggregateField(s, "cd_regression_beta",
					String.format(
						"SELECT var_id, SUM(standardized_value * %s) AS the_value " +
						"FROM %s A, %s B " +
						"WHERE A.patient_id = B.%s " +
						"GROUP BY var_id",
						pc.getOutcomeFieldName(),
						patientVarTableName,
						pc.getPatientViewName(),
						pc.getPatientIdFieldName()
					));
	
			sql = String.format(
					"UPDATE %s " +
					"SET exp_assoc_ranking_var = abs(ce_regression_beta), " +
					"    outcome_assoc_ranking_var = abs(cd_regression_beta) ",
					this.varTableName
				);
			SqlUtils.addToSqlBatch(s, sql);
		
			// FIX!!!
			sql = String.format(
					"UPDATE %s " +
					"SET bias = (ce_regression_beta * cd_regression_beta) ",
					this.varTableName
				);
			SqlUtils.addToSqlBatch(s, sql);
		}

		sql = String.format(
				"UPDATE %s " +
				"SET bias_ranking_var = abs(ln(bias)) " +
				"WHERE bias > 0",
				this.varTableName
			);
		SqlUtils.addToSqlBatch(s, sql);

		SqlUtils.executeSqlBatch(s);
		s.close();
	}

	protected void generateCohorts(List<HdpsVariable> variablesToOutput)
	throws Exception
	{
		// set up full output file
        String[] fullOutputFields = new String[variablesToOutput.size() + 1];
       	fullOutputFields[0] = "patient_id";
       	
       	int k = 1;
       	for (HdpsVariable var: variablesToOutput) {
       		fullOutputFields[k++] = var.varName;
       	}
       	
       	RowWriter fullOutputWriter = null;
       	if (this.hdps.doFullOutput == 1) {
       		String path = Utils.getFilePath(hdps.tempDirectory, hdps.fullOutputFilename);
       		
       		System.out.printf("Writing full output file to %s\n", path);
       		fullOutputWriter = new TabDelimitedFileWriter(
       			path, fullOutputFields);
       	}
       	
       	// set up sparse output file
        String[] sparseOutputFields = new String[2];
       	sparseOutputFields[0] = "patient_id";
       	sparseOutputFields[1] = "var_list";
       	
       	RowWriter sparseOutputWriter = null;
       	if (this.hdps.doSparseOutput == 1)  {
       		String path = Utils.getFilePath(hdps.tempDirectory, hdps.sparseOutputFilename);
       		
       		System.out.printf("Writing sparse output file to %s\n", path);
       		sparseOutputWriter = new TabDelimitedFileWriter(
       			path, sparseOutputFields);
       	}
       	
       	Connection c = HdpsDb.connectionFactory(this.hdps);
       	Statement s = c.createStatement();
       	String sql;
       	
		// variablesToOutput some come in sorted
		// build a variable -> index lookup table
		final SortedMap<String, Integer> varMap = new TreeMap<String, Integer>();
		int i = 1;  		// first index is 1
		for (HdpsVariable var: variablesToOutput) {
//			System.out.printf("Adding var %s\n", var.varName);
			varMap.put(var.varName, new Integer(i));
			i++;
		}
		
		sql = String.format(
				"UPDATE %s " +
				"SET selected_for_ps = 1 " +
				"WHERE var_name IN ( %s )",
				this.varTableName,
				(varMap.keySet().size() == 0) ? 
						"'BAD_VARIABLE_NAME'" : 
						Utils.join(varMap.keySet(), ",", "'")
		);
		SqlUtils.executeSql(s, sql);
       	
        final String oneStr = "1";
        final String zeroStr = "0";
        final String quoteStr = "\"";
       	
        // do an outer join to get patients who have all 
        // variables = 0
   		sql = String.format(
   				"SELECT P.%s AS patient_id, T.var_name, T.standardized_value " +
   				"FROM %s P " +
   				"LEFT OUTER JOIN " +
   				"     (SELECT PV.patient_id, V.var_name, PV.standardized_value " +
   				"      FROM %s PV, %s V " +
   				"      WHERE PV.var_id = V.var_id AND" +
   				"            V.selected_for_ps = 1 " +
   				") T ON P.%s = T.patient_id " +
   				"ORDER BY patient_id, var_name ",
   				((HdpsDbPatientController) this.patientController).patientIdFieldName,
   				((HdpsDbPatientController) this.patientController).patientViewName,
   				this.patientVarTableName,
   				this.varTableName,
   				((HdpsDbPatientController) this.patientController).patientIdFieldName
   		);
   		ResultSet r = SqlUtils.executeSqlQuery(s, sql);
   		
   		String currentPatient = null;
   		boolean rowInProgress = false;
   		List<String> currentPatientVars = new ArrayList<String>();
   		SortedMap<String, Integer> mapSegment = varMap;
   		
   		while (r.next()) {
   			// trim strings because they come back with spaces appended
   			String patientId = r.getString(1).trim();
   			String varName = r.getString(2);
   			double varValue = r.getDouble(3);
   			
   			if (! patientId.equals(currentPatient)) {
   				// start new row
   				if (rowInProgress) {
   					if (fullOutputWriter != null)
   	   					fullOutputWriter.writeRow(fullOutputFields);
   					
   					if (sparseOutputWriter != null) {
   						sparseOutputFields[1] = Utils.join(currentPatientVars, ",");
   						sparseOutputWriter.writeRow(sparseOutputFields);
   					}
   				}

   				rowInProgress = true;
   				mapSegment = varMap;
   				currentPatient = patientId;
   				currentPatientVars.clear();

   				fullOutputFields[0] = quoteStr + currentPatient + quoteStr;
   				for (int j = 1; j < fullOutputFields.length; j++)
   					fullOutputFields[j] = zeroStr;

   				sparseOutputFields[0] = fullOutputFields[0];
   			}
   			
   			if (varName != null) {
   				// trim because the DB can pad the value
   				varName = varName.trim();
   				
   				// make the segment smaller and smaller in order to speed up the search
	   			mapSegment = mapSegment.tailMap(varName);

	   			// get the first item in the map
	   			Map.Entry<String, Integer> entry = mapSegment.entrySet().iterator().next();
	   			String lookupVarName = entry.getKey();
	   			int varIndex = entry.getValue();
	   			if (! lookupVarName.equals(varName))
	   				throw new HdpsException("Fatal variable lookup error.");
	   			if (varValue == 1.0d) 
	   				fullOutputFields[varIndex] = oneStr;
	   			else
	   				fullOutputFields[varIndex] = Double.toString(varValue);

	   			currentPatientVars.add(varName);
   			}
       	}
       	
   		if (rowInProgress) {
			if (fullOutputWriter != null)
   				fullOutputWriter.writeRow(fullOutputFields);
				
			// !!! BUG -- WILL NOT OUTPUT CORRECT STANDARDIZED VALUE!!!
			if (sparseOutputWriter != null) {
				sparseOutputFields[1] = Utils.join(currentPatientVars, ",");
				sparseOutputWriter.writeRow(sparseOutputFields);
			}
   		}
   		
		r.close();
		s.close();
		c.close();
		c = null;
		        
		System.out.printf("NOTE: hd-PS wrote %d patients to cohort.\n",
				this.getNumPatients());

		if (fullOutputWriter != null)
			fullOutputWriter.close();
		
		if (sparseOutputWriter != null)
			sparseOutputWriter.close();
	}
	
	protected void createTables()
	throws Exception
	{
		String sql;
		Statement s = this.connection.createStatement();
		
		this.varTableName = SqlUtils.getTableName("Vars", this.randomSuffix);
		this.patientVarTableName = SqlUtils.getTableName("Patient_Vars", this.randomSuffix);
		
		System.out.printf("Variables table name is %s\n", this.varTableName);
		System.out.printf("Patient variables table name is %s\n", this.patientVarTableName);
		
		sql = String.format(
				"CREATE TABLE %s (" +
				"   var_id	            int, " +
				"   dimension_name      varchar(255), " +
				"   var_name            varchar(255), " +
				"   type                varchar(255), " +
				"   dimension_id        int, " +
				"   code                varchar(255), " +
				"   is_dichotomous      int, " +
				"   n                   double, " +
				"   pt                  double, " +
				"	e1                  double, " +
				"   e0                  double, " +
				"   c1                  double, " +
				"   c0                  double, " +
				"   d1                  double, " +
				"   d0                  double, " +
				"   pt_e1               double, " +
				"   pt_e0               double, " +
				"   pt_c1               double, " +
				"   pt_c0               double, " +
				"   e1c1                double, " +
				"   e1c0                double, " +
				"   e0c1                double, " +
				"   e0c0                double, " +
				"   d1c1                double, " +
				"   d1c0                double, " +
				"   d0c1                double, " +
				"   d0c0                double, " +
				"   num_events          double, " +
				"   c1_num_events       double, " +
				"   c0_num_events       double, " +
				"   pc_e0               double, " +
				"   pc_e1               double, " +
				"   rr_ce               double, " +
				"   rr_cd               double, " +
				"   mean	            double, " +
				"   variance            double, " +
				"   ce_regression_beta  double, " +
				"   cd_regression_beta  double, " +
				"   exp_assoc_ranking_var 			double, " +
				"   outcome_assoc_ranking_var       double, " +
				"   bias_ranking_var		        double, " +
				"   bias                double, " +
				"   consider_for_ps     integer, " +
				"   selected_for_ps     integer" +
				")",
				this.varTableName
			);
		SqlUtils.executeSql(s, sql);
		
		this.varTableIdSequenceName = this.varTableName + "_sequence";
		sql = String.format(
				"CREATE SEQUENCE %s AS integer START WITH 1000 INCREMENT BY 1",
				this.varTableIdSequenceName);
		SqlUtils.executeSql(s, sql);
		
		// create a patient <-> variable linkage table
		sql = String.format(
				"CREATE TABLE %s (" +
				"   var_id            	int, " +
				"   patient_id          varchar(255), " +
				"   var_value           double, " +
				"   standardized_value  double " +
				")",
				this.patientVarTableName
			);
		SqlUtils.executeSql(s, sql);
		
		s.close();
	}
		
	protected void checkParams()
	throws Exception
	{
		super.checkParams();
	}
	
	protected void startHdps()
	throws Exception
	{
		this.connection = HdpsDb.connectionFactory(this.hdps);
		this.createTables();
	}
	
	protected void closeController()
	throws Exception
	{
		try {
			this.patientController.closeController();
			
			for (HdpsDimensionController dc: this.dimensionsMap.values()) {
				System.out.printf("Closing dc %s\n", dc.dimensionId);
				dc.closeController();
			}
			
			if (this.hdps.dbKeepOutputTables == 0) {
		     	Statement s = this.connection.createStatement();
		       	String sql = String.format("DROP TABLE %s",
		       				this.patientVarTableName);
		       	s.execute(sql);
		
		       	sql = String.format("DROP TABLE %s",
		   				this.varTableName);
	
		       	s.execute(sql);
		       	
		       	s.close();
			}
	       	
	       	this.connection.close();
	       	this.connection = null;
		} catch (SQLException e) {
			// ignore errors
		}
	}
	
	public static Connection connectionFactory(Hdps hdps)
	throws Exception {
		Properties properties = new Properties();
		properties.put("user", hdps.dbUsername);
		properties.put("password", hdps.dbPassword);
		
		Class.forName(hdps.dbDriverClass);

		Connection c = DriverManager.getConnection(hdps.dbUrl, properties);
		
		return c;
	}
	
	/**
	 * @return		The number of patients read from the patient input file or database.
	 */
	public int getNumPatients() {
		return(this.patientController.getNumPatients());
	}
	
	/**
	 * @return  The name of the table (view) containing the patient information.
	 */
	public String getPatientTableName() {
		if (this.patientController != null) {
			HdpsDbPatientController pc = (HdpsDbPatientController) this.patientController;
			return pc.getPatientViewName();
		}
		
		return null;
	}
	
	/**
	 * @return  The name of the table containing the variable information.
	 */
	public String getVarTableName() {
		return this.varTableName;
	}
	
	/**
	 * @return  The name of the table containing the patient/variable linkage information.
	 */
	public String getPatientVarTableName() {
		return this.patientVarTableName;
	}
}

