/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.util;

import java.io.*;
import java.sql.*;
import java.util.Properties;

import org.apache.commons.lang.*;

/**
 * Write rows of data to Netezza database.
 * 
 * @author Jeremy A. Rassen
 * @version 1.0.0
 * 
 */
public class NetezzaDatabaseRowWriter extends RowWriter {
	private Connection connection;
	private Statement statement;
	
	private String table;
	private String tempPath;
	private File tempFile;
    private FileWriter tempFileWriter = null;
    
    private int numCachedRows;
    private static final int ROWS_TO_CACHE = 50000000;

    /**
     * TabDelimitedFileWriter constructor.
     * 
     * @throws Exception
     */
    public NetezzaDatabaseRowWriter()
    throws Exception
    {
    	super();
    }
    
    
	/**
	 * NetezzaDatabaseRowWriter constructor.  Opens a database connection using the specified parameters.
	 * 
	 * @param driverClass Class of the JDBC driver for this database.
	 * @param url  JDBC URL of the database.
	 * @param properties Properties for opening the database.
	 * @param table SQL table to write
	 * @param tempPath Writeable temporary directory to utilize
	 * @throws Exception
	 */
	public NetezzaDatabaseRowWriter(String driverClass, String url, Properties properties, 
			String table, String tempPath)
    throws Exception
    {
    	this.open(driverClass, url, properties, table, tempPath);
    }
    
    
	/**
	 * DatabaseRowReader constructor.  Opens a database connection using the specified parameters.
	 * 
	 * @param driverClass Class of the JDBC driver for this database.
	 * @param url  JDBC URL of the database.
	 * @param username  Username for logging into the database.
	 * @param password  Password for logging into the database.
	 * @param table SQL table to write
	 * @param tempPath Writeable temporary directory to utilize
	 * @throws Exception
	 */
	public NetezzaDatabaseRowWriter(String driverClass, String url, String username, 
			String password, String table, String tempPath)
    throws Exception
    {
    	this.open(driverClass, url, username, password, table, tempPath);
    }
    
	/**
	 * Open a database connection and run the query that will yield the data rows.
	 * 
	 * @param driverClass Class of the JDBC driver for this database.
	 * @param url  JDBC URL of the database.
	 * @param username  Username for logging into the database.
	 * @param password  Password for logging into the database.
	 * @param table SQL table to write
	 * @param tempPath Writeable temporary directory to utilize
	 * @throws Exception
	 */
	public void open(String driverClass, String url, String username, String password, String table,
			String tempPath)
	throws Exception
	{
		Properties properties = new Properties();
		properties.put("user", username);
		properties.put("password", password);
		
		this.open(driverClass, url, properties, table, tempPath);
	}
	
	/**
	 * Open a database connection and run the query that will yield the data rows.
	 * 
	 * @param driverClass Class of the JDBC driver for this database.
	 * @param url  JDBC URL of the database.
	 * @param properties Properties for opening the database.
	 * @param query SQL query that will yield the data rows.
	 * @param tempPath Writeable temporary directory to utilize
	 * @throws Exception
	 */
	public synchronized void open(String driverClass, String url, Properties properties, String table,
			String tempPath) 
	throws Exception
	{
		Class.forName(driverClass);

		this.connection = DriverManager.getConnection(url, properties);
		this.statement = this.connection.createStatement();
		
		this.tempPath = tempPath;
		openTempFile();
		
//		String sql = String.format(
//				"CREATE EXTERNAL TABLE '%s' SAMEAS %s USING " +
//				"(" +
//				"	DELIM ',' REMOTESOURCE 'JDBC' QUOTEDVALUE DOUBLE" +
//				")", this.tempFile.getName(), table);
//		statement.execute(sql);
		
		this.table = table;
		
		this.numCachedRows = 0;
	}
	
	private synchronized void openTempFile()
	throws Exception
	{
		this.tempFile = new File(this.tempPath, "emr-" + 
				RandomStringUtils.randomAlphanumeric(10) + ".tmp");
		this.tempFileWriter = new FileWriter(this.tempFile);
	}

    /**
     * Writes a header row of field names to the output stream.
     * 
     * @param fieldNames  Names of the fields for the header row of the file.
     * @throws Exception
     */
    public void writeHeaderRow(String[] fieldNames)
    throws Exception
    {
        // nothing to do
    }

    /* (non-Javadoc)
     * @see org.drugepi.util.RowWriter#writeRow(java.lang.String[])
     */
    public synchronized void writeRow(String[] contents)
        throws Exception
    {
    	if (this.numCachedRows < ROWS_TO_CACHE) {
    		tempFileWriter.write(
    				"\"" + StringUtils.join(contents, "\",\"") + "\"\n"
    		);
    		this.numCachedRows++;
    	} else {
    		this.tempFileWriter.close();
    		this.numCachedRows = 0;

    		String sql = String.format(
    				"INSERT INTO %s " +
    				"SELECT * FROM EXTERNAL '%s' " +
    				"USING (" +
    				"	DELIM ',' REMOTESOURCE 'JDBC' QUOTEDVALUE DOUBLE REQUIREQUOTES TRUE " +
    				")",
    				this.table,	this.tempFile.getCanonicalPath());
    		System.out.println(sql);
    		this.statement.execute(sql);

    		this.tempFile.delete();
    		this.openTempFile();
    	}
    }

    /* (non-Javadoc)
     * @see org.drugepi.util.RowWriter#close()
     */
    public void close()
        throws Exception
    {
    	this.connection.close();
        tempFileWriter.close();
    }
    
    public String toString()
    {
    	return null;
    }
}
