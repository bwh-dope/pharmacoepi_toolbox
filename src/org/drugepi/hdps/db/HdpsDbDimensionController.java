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
import org.drugepi.hdps.storage.HdpsCode;
import org.drugepi.util.*;

public class HdpsDbDimensionController extends HdpsDimensionController {
	// INTERNAL VARIABLES
	private HdpsDb hdpsController;
	
	private Connection connection;
	private String dimensionViewName;
	private String codeTableName;
	private String patientCodeTableName;
	private String patientProfileScoreTableName;
	private String codeFieldName;
	private String patientIdFieldName;
	private String dateFieldName;  
	
	public HdpsDbDimensionController(Hdps hdps, HdpsDb hdpsController) 
	{
		super(hdps);
		this.hdpsController = hdpsController;
	}
	
	public void readDimension() throws Exception {
		this.connection = HdpsDb.connectionFactory(this.hdps);
		
		this.dimensionViewName = SqlUtils.getTableName(this.getFormattedDimId(), 
				this.hdpsController.randomSuffix);
		
		int numRowsRead = 0;
		String sql = String.format(
					"CREATE VIEW %s AS %s",
					this.dimensionViewName,
			  		((DatabaseRowReader) this.reader).getQuery());
		Statement s = this.connection.createStatement();
		SqlUtils.executeSql(s, sql);
		
		sql = String.format(
				"SELECT COUNT(*) AS num_rows FROM %s",
				this.dimensionViewName);
		ResultSet r = s.executeQuery(sql);
		r.next();
		numRowsRead = r.getInt("num_rows");
		r.close();
		
		sql = String.format(
				"SELECT * FROM %s WHERE 1=0",
				this.dimensionViewName);
		r = s.executeQuery(sql);
		this.patientIdFieldName = r.getMetaData().getColumnName(1);
		this.codeFieldName = r.getMetaData().getColumnName(2);
		if (r.getMetaData().getColumnCount() > 2)
			this.dateFieldName = r.getMetaData().getColumnName(3);
		else
			// hack: "1" will sub OK in the SQL
			this.dateFieldName = "1";

		r.close();
		s.close();
	
        reader.close();
        System.out.printf("NOTE: hd-PS dimension %s has %d rows in view %s.", this.dimensionDescription,
        		numRowsRead, this.dimensionViewName);
        System.out.println("");
		
		this.createCodeDatabase();
		
		
		if (this.hdps.inferServiceIntensityVars == 1) {
			this.createServiceIntensityVariables(true);
			this.createServiceIntensityVariables(false);
		}

		if (this.hdps.createProfileScores == 1)
			this.createProfileScoreVariables(6);

		if (this.hdps.createTimeInteractions == 1)
			this.createTimeInteractionVariables();
		
		System.out.printf(
				"NOTE: hd-PS dimension %s building finished into table %s.\n",
				this.dimensionDescription, this.codeTableName);

	}	

	private void createCodeDatabase() throws Exception
	{
		this.codeTableName = SqlUtils.getTableName(this.getFormattedDimId() + "_Codes", this.hdpsController.randomSuffix);
		System.out.printf("Code table name is %s\n", this.codeTableName);
		this.patientCodeTableName = SqlUtils.getTableName(
				this.getFormattedDimId() + "_Patient_Codes", this.hdpsController.randomSuffix);
		System.out.printf("Patient code table name is %s\n", this.patientCodeTableName);

		Statement s = this.connection.createStatement();

		// make basic code table
		String sql = String.format(
				"CREATE TABLE %s ( " +
				"    code               varchar(512)," +
				"    frequency          int," +
				"    consider_for_ps    int," +
				"    prevalence         double," +
				"    median_occurrences double," +
				"    q3_occurrences     double" +
				")",
				this.codeTableName
			);
		SqlUtils.executeSql(s, sql);

		// make basic code table
		sql = String.format(
				"INSERT INTO %s (code, frequency, consider_for_ps) " +
				"( " +
				"       SELECT %s AS code, " +
				"	    COUNT(DISTINCT %s) AS frequency, " +
				"       1 AS consider_for_ps " +
				"       FROM %s " +
				"       GROUP BY %s " +
				")",
				this.codeTableName,
				this.codeFieldName,
				this.patientIdFieldName,
				this.dimensionViewName,
				this.codeFieldName
			);
		SqlUtils.executeSql(s, sql);
		
		// mark any codes with too little frequency
		sql = String.format(
				"UPDATE %s " +
				"SET consider_for_ps = 0 " +
				"WHERE frequency <= %d",
				this.codeTableName,
				this.hdps.frequencyMin
			);
		SqlUtils.executeSql(s, sql);
		
		// update code prevalence
		sql = String.format(
				"UPDATE %s " +
				"SET prevalence = (frequency / %d.0)",
				this.codeTableName,
				this.hdpsController.getNumPatients()
			);
		SqlUtils.executeSql(s, sql);
		
		// flip prevalences > 50%
		sql = String.format(
				"UPDATE %s " +
				"SET prevalence = 1.0 - prevalence " +
				"WHERE prevalence > 0.5",
				this.codeTableName
			);
		SqlUtils.executeSql(s, sql);
		
		// mark codes with top N prevalence for consideration
		sql = String.format(
				"UPDATE %s " +
				"SET consider_for_ps = 0 " +
				"WHERE code IN ( " +
				"  SELECT code FROM ( " +
				"    SELECT code, RANK() OVER (ORDER BY prevalence DESC) AS ranking " +
				"    FROM %s ) AS subsel " +
				"  WHERE ranking > %d " +
				")",
				this.codeTableName,
				this.codeTableName,
				this.hdps.topN
			);
		SqlUtils.executeSql(s, sql);
		
		// create a code <-> patient table
		sql = String.format(
				"CREATE TABLE %s AS " +
				"SELECT %s AS code, %s AS patient_id, COUNT(*) as frequency, " +
				"MIN(%s) AS latest_occurrence " +
				"FROM %s " +
				"WHERE code IN ( " +
				"   SELECT code FROM %s " +
				"   WHERE consider_for_ps = 1 " +
				") "+
				"GROUP BY %s, %s",
				this.patientCodeTableName,
				this.codeFieldName,
				this.patientIdFieldName,
				this.dateFieldName,
				this.dimensionViewName,
				this.codeTableName,
				this.codeFieldName,
				this.patientIdFieldName
			);
		SqlUtils.executeSql(s, sql);

		sql = String.format(
				"UPDATE %s A " +
				"SET median_occurrences = B.median, " +
				"    q3_occurrences = B.q3 " +
				"FROM ( " +
				"   SELECT code, " +
				"   percentile_cont(0.50) WITHIN GROUP(ORDER BY frequency) AS median, " +
				"   percentile_cont(0.75) WITHIN GROUP(ORDER BY frequency) AS q3 " +
				"   FROM %s GROUP BY code " +
				") AS B " +
				"WHERE A.code = B.code AND" +
				"      A.consider_for_ps = 1",
				this.codeTableName,
				this.patientCodeTableName
		);
		SqlUtils.executeSql(s, sql);
		
		// create each of the three types of variables: once, sporadic, and frequent
		sql = String.format(
				"INSERT INTO %s(dimension_name, var_id, type, dimension_id, code, is_dichotomous) (" +
				"   SELECT '%s' as dimension_name, NEXT VALUE FOR %s, " +
				"			'Once' AS type, %d as dimension_id, code, 1 " +
				"   FROM %s " +
				"   WHERE consider_for_ps = 1 " +
				"   UNION ALL " +
				"   SELECT '%s' as dimension_name, NEXT VALUE FOR %s, " +
				"           'Spor' AS type, %d as dimension_id, code, 1 " +
				"   FROM %s " +
				"   WHERE consider_for_ps = 1 AND median_occurrences > 1 " +
				"   UNION ALL " +
				"   SELECT '%s' as dimension_name, NEXT VALUE FOR %s, " +
				"           'Freq' AS type, %d as dimension_id, code, 1 " +
				"   FROM %s " +
				"   WHERE consider_for_ps = 1 AND q3_occurrences > median_occurrences" +
				")",
				hdpsController.varTableName,
				this.dimensionDescription,
				this.hdpsController.varTableIdSequenceName,
				this.dimensionId,
				this.codeTableName,
				this.dimensionDescription,
				this.hdpsController.varTableIdSequenceName,
				this.dimensionId,
				this.codeTableName,
				this.dimensionDescription,
				this.hdpsController.varTableIdSequenceName,
				this.dimensionId,
				this.codeTableName
		);
		SqlUtils.executeSql(s, sql);

		// update the linkage table for Once, Sporadic, and Frequent occurrence types
		sql = String.format(
				"INSERT INTO %s(patient_id, var_id, var_value, standardized_value) (" +
				"SELECT DISTINCT pc.patient_id, v.var_id, 1, 1 " +
				"FROM %s pc, %s c, %s v " +
				"WHERE pc.code = c.code AND c.code = v.code AND v.type = 'Once' AND v.dimension_id = %d AND " +
				"pc.frequency >= 1 " +

				"UNION ALL " +
				
				"SELECT DISTINCT pc.patient_id, v.var_id, 1, 1 " +
				"FROM %s pc, %s c, %s v " +
				"WHERE pc.code = c.code AND c.code = v.code AND v.type = 'Spor' AND v.dimension_id = %d AND " +
				"pc.frequency >= c.median_occurrences " +

				"UNION ALL " +
				
				"SELECT DISTINCT pc.patient_id, v.var_id, 1, 1 " +
				"FROM %s pc, %s c, %s v " +
				"WHERE pc.code = c.code AND c.code = v.code AND v.type = 'Freq' AND v.dimension_id = %d AND " +
				"pc.frequency >= c.q3_occurrences " +
				
				")",
				
				hdpsController.patientVarTableName,
				this.patientCodeTableName, this.codeTableName, hdpsController.varTableName, this.dimensionId,
				this.patientCodeTableName, this.codeTableName, hdpsController.varTableName, this.dimensionId,
				this.patientCodeTableName, this.codeTableName, hdpsController.varTableName, this.dimensionId
			);
		SqlUtils.executeSql(s, sql);
		
		s.close();
	}
	

	protected void createTimeInteractionVariables()
	throws Exception
	{
		Statement s = this.connection.createStatement();
		String sql;
		
		sql = String.format(
				"INSERT INTO %s(dimension_name, var_id, type, dimension_id, code, is_dichotomous) (" +
				"   SELECT dimension_name, " +
				"           NEXT VALUE FOR %s, " +  
				"		   'TimeInt' AS type, " +
				"		    dimension_id, code, " +
				"			0 AS is_dichotomous " +
				"   FROM %s " +
				"   WHERE type = 'Once' AND dimension_id = %d" +
				")",
				hdpsController.varTableName,
				hdpsController.varTableIdSequenceName,
				hdpsController.varTableName,
				this.dimensionId
		);
//		System.out.println(sql);
		SqlUtils.executeSql(s, sql);
		
		sql = String.format(
				"INSERT INTO %s(patient_id, var_id, var_value) (" +
				"SELECT DISTINCT pc.patient_id, v.var_id, " +
				"				CASE WHEN pc.latest_occurrence = 0 THEN 1" +
				"               ELSE (1 / pc.latest_occurrence) " +
				"               END AS var_value  " +
				"FROM %s pc, %s c, %s v " +
				"WHERE pc.code = c.code AND c.code = v.code AND v.type = 'TimeInt' AND v.dimension_id = %d AND " +
				"pc.frequency >= 1 " +
				")",
				
				hdpsController.patientVarTableName,
				this.patientCodeTableName, this.codeTableName, hdpsController.varTableName, this.dimensionId
			);
//		System.out.println(sql);
		SqlUtils.executeSql(s, sql);
		
		s.close();
	
	}
	
	/**
	 * @throws Exception
	 * 
	 * Per Suissa:
	 * 1) Calculate how many times each patient has a code
	 * 2) Calculate the frequency of codes in months 1..12
	 *     N.B.: Month 12 is closest and Month 1 is most remote
	 * 3) Calculate the percentage of the total dispensed in each month
	 * 4) Calculate the cumulative percentage dispensed at each month
	 * 5) Calculate the profile score as 12 - SUM(cumulative percentages);
	 *    this will be a value from 0 to 11.  A profile score of 0 indicates
	 *    that all of the occurrences of the code were in Month 1 (most remote time);
	 *    a profile score of 12 indicates that all of the occurrences of the code
	 *    were just before exposure (Month 12)
	 * 6) Dichotomize the profile score as being <5.5 (remote occurrences) or >= 5.5
	 *    (more recent occurrences)
	 */
	protected void createProfileScoreVariables(int numPeriods)
	throws Exception
	{
		this.patientProfileScoreTableName = 
			SqlUtils.getTableName(this.getFormattedDimId() + "_ProfileScores", 
					this.hdpsController.randomSuffix);
		System.out.printf("Patient profile score table name is %s\n", this.patientProfileScoreTableName);
		
		Statement s = this.connection.createStatement();
		String sql;
		StringBuffer sqlBuf = new StringBuffer();
		
		sqlBuf.append(String.format(
				"CREATE TABLE %s AS " +
				"SELECT %s AS code, %s AS patient_id, SUM(1) AS sum_total, ",
				this.patientProfileScoreTableName, this.codeFieldName, this.patientIdFieldName
			));
		
		// counts and percents by period
		for (int i = 1; i <= numPeriods; i++) {
			// Month 1 is most remote; month 12 is closest in time
			int reverseMonth = (numPeriods + 1) - i;
			int minDay = (reverseMonth - 1) * 30;
			int maxDay = reverseMonth * 30;
			
			sqlBuf.append(String.format(
					"SUM(CASE WHEN days_before_index >= %d AND " +
					"				days_before_index < %d THEN 1" +
					"    ELSE 0 " +
					"    END) AS sum_period%d, " +
					"sum_period%d / sum_total AS pct_period%d, ",
					minDay, maxDay,  i, i, i
			));
		}
		
		// cumulative counts by period
		sqlBuf.append("pct_period1 AS cum_period1, ");
		for (int i = 2; i <= numPeriods; i++) {
			sqlBuf.append(String.format(
					"	pct_period%d + cum_period%d AS cum_period%d, ",
					i, i-1, i
			));
		}
		
		// the values will range from 0 to numPeriods; chop at 3/4 of the way
		double splitPoint = (3 * (numPeriods - 1)) / 4;
		
		// total cumulative counts and profile_score
		ArrayList<String> cumPeriodFields = new ArrayList<String>(numPeriods);
		for (int i = 1; i <= numPeriods; i++) {
			cumPeriodFields.add(String.format("cum_period%d", i));
		}
		// c1 + c2 + ... + cn
		sqlBuf.append(Utils.join(cumPeriodFields, "+"));
		sqlBuf.append(String.format(
				" AS acd, " +
				"%d - acd AS profile_score, " +
				"CASE WHEN profile_score < %f THEN 0 ELSE 1 END AS binary_prof_score ",
				numPeriods, splitPoint
		));
		
		// finalize the sql
		sqlBuf.append(String.format(
				"FROM %s " +
				"GROUP BY patient_id, code " +
				"HAVING sum_total > 0",
				this.dimensionViewName
		));
		
		SqlUtils.executeSql(s, sqlBuf.toString());
		
		// create variables
		sql = String.format(
				"INSERT INTO %s(dimension_name, var_id, type, dimension_id, code, is_dichotomous) (" +
				"   SELECT dimension_name, " +
				"		    NEXT VALUE FOR %s, " +
				"		   'ProfileScore' AS type, " +
				"		    dimension_id, code, " +
				"			1 AS is_dichotomous " +
				"   FROM %s " +
				"   WHERE type = 'Once' AND dimension_id = %d" +
				")",
				hdpsController.varTableName,
				hdpsController.varTableIdSequenceName,
				hdpsController.varTableName,
				this.dimensionId
		);
		SqlUtils.executeSql(s, sql);
		
		// NOTE: Only insert values of 1
		// CalculateBias() will assume a value of 0 for other patients
		// !!! missing values?!?
		sql = String.format(
				"INSERT INTO %s(patient_id, var_id, var_value) (" +
				"SELECT DISTINCT profscore.patient_id, v.var_id, profscore.binary_prof_score " +
				"FROM %s profscore, %s c, %s v " +
				"WHERE profscore.code = c.code AND c.code = v.code AND v.type = 'ProfileScore' AND v.dimension_id = %d " +
				"      AND profscore.binary_prof_score = 1" +
				")",
				hdpsController.patientVarTableName,
				this.patientProfileScoreTableName, this.codeTableName, hdpsController.varTableName, this.dimensionId
			);
		SqlUtils.executeSql(s, sql);
		
		s.close();
	}
	
	protected void createServiceIntensityVariables(boolean uniqueOnly)
	throws Exception
	{
		Statement s = this.connection.createStatement();
		String sql;
		
		for (int i = 1; i <= 4; i++) {
			sql = String.format(
					"INSERT INTO %s(dimension_id, dimension_name, var_id, " +
					"               code, is_dichotomous, type) VALUES ( " +
					"    %d as dimension_id, " +
					"    '%s' as dimension_name, " +
					"    NEXT VALUE FOR %s, " +
					"    RTRIM('D%d_INT_%s_Q%d') AS code, " +
					"    1 as is_dichotomous, " +
					"    'ServiceInt' " +
					")",
					hdpsController.varTableName, 
					this.dimensionId,
					this.dimensionDescription,
					this.hdpsController.varTableIdSequenceName,
					this.dimensionId,
					(uniqueOnly ? "UNIQ" : "ALL"),
					i
				);

			SqlUtils.addToSqlBatch(s, sql);
		}
		
		String countQuery = null;
		
		if (uniqueOnly)
			countQuery = String.format("COUNT(DISTINCT %s)",
					this.codeFieldName);
		else countQuery = ("COUNT(*)");
			
		// is OK that this will be fractional -- it doesn't exactly matter that
		// the groups are equally sized
		sql = String.format(
				"INSERT INTO %s(patient_id, var_id, var_value, standardized_value) " +
				"SELECT patient_id, V.var_id, 1, 1 " +
				"FROM " +
				"   (SELECT *, " +
				"		RTRIM('D%d_INT_%s_Q' || " +
				"        CASE " +
				"			WHEN percent_rank >= 0.00 AND percent_rank <= 0.25 THEN 1 " +
				"			WHEN percent_rank >  0.25 AND percent_rank <= 0.50 THEN 2 " +
				"			WHEN percent_rank >  0.50 AND percent_rank <= 0.75 THEN 3 " +
				"			WHEN percent_rank >  0.75 AND percent_rank <= 1.00 THEN 4 " +
				"         END " +
				"		  ) AS code " +
				"   FROM (" +
				"      SELECT %s as patient_id, percent_rank()  " +
				"             OVER(ORDER BY %s) AS percent_rank " +
				"      FROM %s " +
				"      GROUP BY %s" +
				"   ) AS T2) AS T1, %s AS V " +
				"   WHERE T1.code = V.code ", 
				hdpsController.patientVarTableName,
				this.dimensionId,
				(uniqueOnly ? "UNIQ" : "ALL"),
				this.patientIdFieldName,
				countQuery,
				this.dimensionViewName,
				this.patientIdFieldName,
				this.hdpsController.varTableName
		);
		System.out.println(sql);
		SqlUtils.addToSqlBatch(s, sql);

		SqlUtils.executeSqlBatch(s);
		
		s.close();
	}
	
	public void writeCodes(RowWriter writer)
	throws Exception
	{
		String sql = String.format(
				"SELECT * " +
				"FROM %s " +
				"ORDER BY code",
				this.codeTableName
		);
		Statement s = this.connection.createStatement();
		ResultSet r = s.executeQuery(sql);

		while (r.next()) {
			String id = r.getString("code");
			HdpsCode code = new HdpsCode(id, r);
			code.dimension = this;
			writer.writeRow(code.toStringArray());
		}
		r.close();
		s.close();
	}
	
	public List<HdpsCode> getCodes() throws Exception
	{
		List<HdpsCode> codes = new ArrayList<HdpsCode>();
		
		String sql = String.format(
				"SELECT * " +
				"FROM %s " +
				"ORDER BY code",
				this.codeTableName
		);
		Statement s = this.connection.createStatement();
		ResultSet r = s.executeQuery(sql);

		while (r.next()) {
			String id = r.getString("code");
			HdpsCode code = new HdpsCode(id, r);
			code.dimension = this;
			codes.add(code);
		}
		r.close();
		s.close();
		
		return codes;
	}
	
	public void freeUselessCodes() {
		// nothing to do
	}

	public void updateVariablesToConsider()
	throws Exception
	{
		Statement s = this.connection.createStatement();
		String sql = String.format(
				"UPDATE %s " +
				"SET consider_for_ps = 1 " +
				"WHERE var_id IN (" +
				"   SELECT var_id " +
				"   FROM %s C, %s V " +
				"   WHERE (C.code = V.code AND C.consider_for_ps = 1) OR " +
				"          V.type NOT IN ('Once', 'Spor', 'Freq')" +
				")",
				hdpsController.varTableName,
				this.codeTableName,
				hdpsController.varTableName
		);
		SqlUtils.executeSql(s, sql);
		s.close();
	}

	private String getFormattedDimId()
	{
		return (String.format("Dim_%d", this.dimensionId));
	}
	
	public void closeController() 
	throws Exception
	{
		if (this.hdps.dbKeepOutputTables == 0) {
	     	Statement s = this.connection.createStatement();
	       	String sql = String.format("DROP VIEW %s",
	       				this.dimensionViewName);
	       	SqlUtils.addToSqlBatch(s, sql);
	
	       	sql = String.format("DROP TABLE %s",
	   				this.codeTableName);
	       	SqlUtils.addToSqlBatch(s, sql);
	       	
	       	sql = String.format("DROP TABLE %s",
	   				this.patientCodeTableName);
	       	SqlUtils.addToSqlBatch(s, sql);
	       	
	       	if ((this.hdps.createProfileScores == 1) && 
	       		(this.patientProfileScoreTableName != null)) {
		       	sql = String.format("DROP TABLE %s",
		   				this.patientProfileScoreTableName);
		       	SqlUtils.addToSqlBatch(s, sql);
	       	}
	       	SqlUtils.executeSqlBatch(s);
	       	s.close();
		}
       	
       	this.connection.close();
       	this.connection = null;
	}

}
