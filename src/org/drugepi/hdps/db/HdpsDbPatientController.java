/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.hdps.db;

import java.sql.*;
import java.util.*;

import org.drugepi.hdps.*;
import org.drugepi.util.DatabaseRowReader;

public class HdpsDbPatientController extends HdpsPatientController
{
	private HdpsDb hdpsController;
	private Connection connection;

	public String patientViewName;
	public String patientIdFieldName;
	public String exposureFieldName;
	public String outcomeFieldName;
	public String personTimeFieldName;
	
   public HdpsDbPatientController(Hdps hdps, HdpsDb hdpsController)
    {
    	super(hdps);
    	this.hdpsController = hdpsController;
    }

	public void readPatients()
    throws Exception
    {
		this.connection = HdpsDb.connectionFactory(this.hdps);
		
		this.patientViewName = SqlUtils.getTableName("patients", this.hdpsController.randomSuffix);
		
		String sql;
		if (this.reader.getNumColumns() > 3) {
			sql = String.format(
						"CREATE VIEW %s AS %s",
						this.patientViewName,
				  		((DatabaseRowReader) this.reader).getQuery());
		} else {
			sql = String.format(
					"CREATE VIEW %s AS %s",
					this.patientViewName,
			  		((DatabaseRowReader) this.reader).getQuery());
			sql = sql.replaceFirst("\\s[Ff][Rr][Oo][Mm]\\s", ", 1 AS fu_time FROM ");
		}
		Statement s = this.connection.createStatement();
		s.execute(sql);
		System.out.printf("Patients view name is %s\n", this.patientViewName);
		
		sql = String.format(
				"SELECT COUNT(*) AS num_patients FROM %s",
				this.patientViewName);
		ResultSet r = s.executeQuery(sql);
		r.next();
		this.numPatients = r.getInt("num_patients");
		r.close();
		
		sql = String.format(
				"SELECT * FROM %s",
				this.patientViewName);
		r = s.executeQuery(sql);
		this.patientIdFieldName = r.getMetaData().getColumnName(1);
		this.exposureFieldName = r.getMetaData().getColumnName(2);
		this.outcomeFieldName = r.getMetaData().getColumnName(3);
		this.personTimeFieldName = r.getMetaData().getColumnName(4);
		r.close();
		s.close();
		
        System.out.printf("NOTE: hd-PS read %d patients.", this.hdpsController.getNumPatients());
        System.out.println("");
    }
	
	public List<String> getPatients()
	throws Exception
	{
       	Statement s = this.connection.createStatement();
       	String sql = String.format("SELECT %S FROM %s ORDER BY %s",
       			this.patientIdFieldName,
       			this.patientViewName,
       			this.patientIdFieldName
       	);
       	ResultSet r = s.executeQuery(sql);
       	List<String> patients = new ArrayList<String>();
       	while (r.next()) 
       		patients.add(r.getString(1));
       	r.close();
       	s.close();
       
       	return patients;
	}
	
	public void closeController() 
	throws Exception
	{
		if (this.hdps.dbKeepOutputTables == 0) {
	     	Statement s = this.connection.createStatement();
	       	String sql = String.format("DROP VIEW %s",
	       				this.patientViewName);
	       	s.execute(sql);
	       	s.close();
		}
       	
       	this.connection.close();
       	this.connection = null;
  }
	
	public String getPatientViewName() {
		return patientViewName;
	}

	public String getPatientIdFieldName() {
		return patientIdFieldName;
	}

	public String getExposureFieldName() {
		return exposureFieldName;
	}

	public String getOutcomeFieldName() {
		return outcomeFieldName;
	}

	public String getPersonTimeFieldName() {
		return personTimeFieldName;
	}
}

