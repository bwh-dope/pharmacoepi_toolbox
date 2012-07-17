/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.util;

import java.sql.*;
import java.util.Properties;

/**
 * Read rows of data from a database.
 * 
 * @author Jeremy A. Rassen
 * @version 1.0.0
 * 
 */
public class DatabaseRowReader extends RowReader {
	private Connection connection;
	private Statement statement;
	private ResultSet rs;
	
	private String query;
	
	/**
	 * DatabaseRowReader constructor.
	 * 
	 * @throws Exception
	 */
	public DatabaseRowReader()
	throws Exception
	{
		super();
	}
	
	/**
	 * DatabaseRowReader constructor.  Opens a database connection using the specified parameters.
	 * 
	 * @param driverClass Class of the JDBC driver for this database.
	 * @param url  JDBC URL of the database.
	 * @param properties Properties for opening the database.
	 * @param query SQL query that will yield the data rows.
	 * @throws Exception
	 */
	public DatabaseRowReader(String driverClass, String url, Properties properties, String query)
	throws Exception
	{
		this();
		this.open(driverClass, url, properties, query);
	}

	/**
	 * DatabaseRowReader constructor.  Opens a database connection using the specified parameters.
	 * 
	 * @param driverClass Class of the JDBC driver for this database.
	 * @param url  JDBC URL of the database.
	 * @param username  Username for logging into the database.
	 * @param password  Password for logging into the database.
	 * @param query SQL query that will yield the data rows.
	 * @throws Exception
	 */
	public DatabaseRowReader(String driverClass, String url, String username, String password, String query)
	throws Exception
	{
		this();
		this.open(driverClass, url, username, password, query);
	}
	
	/**
	 * Open a database connection and run the query that will yield the data rows.
	 * 
	 * @param driverClass Class of the JDBC driver for this database.
	 * @param url  JDBC URL of the database.
	 * @param username  Username for logging into the database.
	 * @param password  Password for logging into the database.
	 * @param query SQL query that will yield the data rows.
	 * @throws Exception
	 */
	public void open(String driverClass, String url, String username, String password, String query)
	throws Exception
	{
		Properties properties = new Properties();
		properties.put("user", username);
		properties.put("password", password);
		
		this.query = query;
		
		this.open(driverClass, url, properties, query);
	}

	/**
	 * Open a database connection and run the query that will yield the data rows.
	 * 
	 * @param driverClass Class of the JDBC driver for this database.
	 * @param url  JDBC URL of the database.
	 * @param properties Properties for opening the database.
	 * @param query SQL query that will yield the data rows.
	 * @throws Exception
	 */
	public void open(String driverClass, String url, Properties properties, String query) 
	throws Exception
	{
		Class.forName(driverClass);

		this.connection = DriverManager.getConnection(url, properties);
		this.statement = this.connection.createStatement();
		this.rs = this.statement.executeQuery(query);
		
		this.numColumns = this.rs.getMetaData().getColumnCount();
	}
	
	public void reset()
	throws Exception
	{
		this.rs.first();
	}
	
	/* (non-Javadoc)
	 * @see org.drugepi.util.RowReader#close()
	 */
	public void close() throws Exception {
		try {
			if (this.statement != null)
				this.statement.cancel();
			
			if (this.rs != null) {
				this.rs.clearWarnings();
				this.rs.close();
			}
			
			if (this.statement != null)
				this.statement.close();
			
			if (this.connection != null)
				this.connection.close();
		} catch (Exception e) {
			// do nothing -- this is OK.
		}
	}

	/* (non-Javadoc)
	 * @see org.drugepi.util.RowReader#getNextRow()
	 */
	public String[] getNextRow() throws Exception {
		if (this.rs == null)
			return null;
		
		if (! rs.next()) 
			return null;
		
		int numColumns = this.rs.getMetaData().getColumnCount();
		String[] row = new String[numColumns];
		for (int i = 0; i < numColumns; i++) {
			row[i] = rs.getString(i + 1);
		}
		
		return row;
	}

	public String getQuery() {
		return query;
	}
}
