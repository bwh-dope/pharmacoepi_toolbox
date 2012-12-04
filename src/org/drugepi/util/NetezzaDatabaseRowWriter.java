/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.util;

import java.io.*;
import java.sql.*;
import java.util.Properties;
import java.util.concurrent.PriorityBlockingQueue;

import org.apache.commons.lang.*;

/**
 * Write rows of data to Netezza database.
 * 
 * @author Jeremy A. Rassen
 * @version 1.0.0
 * 
 */
public class NetezzaDatabaseRowWriter extends RowWriter {
	private String tempPath;
	private File tempFile;
    private FileWriter tempFileWriter = null;
    
    private int numCachedRows;
    private static final int ROWS_TO_CACHE = 5000000;

    private static DatabaseWriterQueue writerQueue;
    
    static {
    	writerQueue = null;
    }
    
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
     * Inner class to implement a writer queue.  As temp files are generated, they are put in the queue.
     * 
     * This class operates in a separate thread that will pick items off the queue and write them
     * to the database.  This avoids over-saturation of the network port by having multiple threads
     * trying to write the database simultaneously.
     *
     */
    private static class DatabaseWriterQueue implements Runnable {
    	public static Object lock = new Object();
    	
    	private PriorityBlockingQueue<File> queue;
		private Connection connection;
    	private Statement statement;
    	private String table;
    	
    	public DatabaseWriterQueue() {
    		this.queue = new PriorityBlockingQueue<File>();
    		this.connection = null;
    		this.statement = null;
    		this.table = null;
    	}
    	
    	public void add(File f) {
    		this.queue.add(f);
    	}
    	
		public void init(Connection connection, String table)
		throws Exception {
			this.connection = connection;
    		this.statement = this.connection.createStatement();
			this.table = table;
		}

    	public void run() {
    		System.out.println("Database writing queue started");
    		while (true) {
    			File uploadFile = null;
    			try {
    				uploadFile = this.queue.take();
    			} catch (Exception e) {
    				e.printStackTrace();
    			}
	    		
	    		try {
		    		String sql = String.format(
		    				"INSERT INTO %s " +
		    				"SELECT * FROM EXTERNAL '%s' " +
		    				"USING (" +
		    				"	DELIM '|' REMOTESOURCE 'JDBC' " +
		    				"   MAXERRORS 100 " +
		    				")",
		    				this.table,	uploadFile.getCanonicalPath());
		
		    		System.out.println("Database writer queue beginning write of " + uploadFile.getCanonicalPath());
		    		this.statement.execute(sql);
		    		System.out.println("Database writer queue ending write");
	    			uploadFile.delete();
	    		} catch (Exception e) {
	    			e.printStackTrace();
	    		} 
    		}
    	}
    	
    	public void finalize()
    	throws Exception {
    		if (this.connection != null) {
    			this.connection.close();
    			this.connection = null;
    		}
    	}
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
	public void open(String driverClass, String url, Properties properties, String table,
			String tempPath) 
	throws Exception
	{
		// only open a connection the first time this is called
		// will also start writer queue
		// only allow one thread to actually start the queue
		synchronized(DatabaseWriterQueue.lock) {
			if (writerQueue == null) {
				Class.forName(driverClass);
				Connection connection = DriverManager.getConnection(url, properties);
				writerQueue = new DatabaseWriterQueue();
				writerQueue.init(connection, table);

				new Thread(writerQueue).start();
			}
		}
		
		this.tempPath = tempPath;
		openTempFile();
		
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
//    				"\"" + StringUtils.join(contents, "\"|\"") + "\"\n"
    	    		StringUtils.join(contents, "|") + "\n"
    		);
    		this.numCachedRows++;
    	} else {
    		this.tempFileWriter.close();
    		writerQueue.add(this.tempFile);

    		this.numCachedRows = 0;
    		this.openTempFile();
    	}
    }

    /* (non-Javadoc)
     * @see org.drugepi.util.RowWriter#close()
     */
    public void close()
        throws Exception
    {
        tempFileWriter.close();
        writerQueue.add(this.tempFile);
    }
    
    public String toString()
    {
    	return null;
    }
    
    public static void main(String[] argv)
    throws Exception {
    	String url = argv[0];
    	String username = argv[1];
		String password = argv[2];
		String table = argv[3];
		String file = argv[4];
		
		Class.forName("org.netezza.Driver");
		Properties properties = new Properties();
		properties.put("user", username);
		properties.put("password", password);
		Connection connection = DriverManager.getConnection(url, properties);
		
		String sql = String.format(
				"INSERT INTO %s " +
				"SELECT * FROM EXTERNAL '%s' " +
				"USING (" +
				"	DELIM '|' REMOTESOURCE 'JDBC' " +
				"   MAXERRORS 100 " +
				")",
				table,	file);

		Statement s = connection.createStatement();
		System.out.println("Beginning write of " + file);
		s.execute(sql);
		System.out.println("Ending write");
    }
}
