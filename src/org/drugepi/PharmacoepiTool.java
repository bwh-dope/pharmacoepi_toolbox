/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

package org.drugepi;

import java.util.Date;

import org.drugepi.util.*;

/**
 * Abstract class from which all tools in the toolbox are derived.
 *  
 * @author Jeremy A. Rassen
 * @version 2.4.15
 *
 */
public abstract class PharmacoepiTool {
	public static String description = "Pharamcoepi Toolbox";
	public static String version = "2.4.18";
	
	private long startTime;
	
	public PharmacoepiTool()
	{
		// not implemented
	}
		
	/**
	 * Add patient information, with patient data stored in a tab-delimited file.
	 * 
	 * @param filePath		Path of the patient data file.  See the documentation on each
	 * 						subclass for the expected order of patient columns.
	 * @throws Exception	
	 */
	public void addPatients(String filePath)
	throws Exception
	{		
		RowReader reader = new TabDelimitedFileReader(filePath);
		this.addPatients(reader);

		// note: the implementing tool must close the reader
	}
	
	/**
	 * Add patient information, with patient data stored in a string buffer.
	 * 
	 * @param buf			Patient data.  See the documentation on each
	 * 						subclass for the expected order of patient columns.
	 * @throws Exception	
	 */
	public void addPatientsFromBuffer(String buf)
	throws Exception
	{		
		RowReader reader = new StringBufferRowReader(buf);
		this.addPatients(reader);

		// note: the implementing tool must close the reader
	}
	
	/**
	 * Add patient information, with patient data stored in a database.
	 * 
	 * @param dbDriverClass	Name of the database driver.
	 * @param dbURL			JDBC URL of the database
	 * @param dbUser		Database user name.
	 * @param dbPassword	Database user's password.
	 * @param dbQuery		The query that will result in the patient data.  See the documentation on each
	 * 						subclass for the expected order of patient columns.
	 * @throws Exception
	 */
	public void addPatients(String dbDriverClass, String dbURL, String dbUser, String dbPassword,
			String dbQuery)
	throws Exception
	{
		RowReader reader = new DatabaseRowReader(dbDriverClass, dbURL, dbUser, dbPassword, dbQuery);
		this.addPatients(reader);
		
		// note: the implementing tool must close the reader
	}
	
	/**
	 * Add patient information from specified row reader object.
	 * 
	 * @param reader  The row reader object.
	 * @throws Exception
	 */
	public void addPatients(RowReader reader)
	throws Exception 
	{
		// do nothing.  override by subclass if desired.
	}
	 
	protected void startTool()
	{
        System.out.printf("NOTE: %s version %s starting at %s.\n", 
        		description, version, new Date().toString());
		this.startTime = System.currentTimeMillis();
	}
	
	protected void endTool()
	{
        long eTime = System.currentTimeMillis() - startTime;
        double minutes = Math.floor(eTime / (60 * 1000F));
        eTime -= minutes * 60 * 1000F;
        double seconds = eTime / 1000F;
        System.out.printf("NOTE: %s finished at %s.  Run time: %02d:%02.3f.\n", 
        		description, new Date().toString(), (int) minutes, seconds);
	}

	/**
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}
}
