/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.util;

import java.io.FileWriter;

import org.apache.commons.lang.StringUtils;

/**
 * Write rows of data to a tab-delimited file.
 * 
 * @author Jeremy A. Rassen
 * @version 1.0.0
 * 
 */
public class DelimitedFileWriter extends RowWriter {
    private FileWriter fw = null;
    private String delimiter;

    /**
     * TabDelimitedFileWriter constructor.
     * 
     * @throws Exception
     */
    public DelimitedFileWriter()
    throws Exception
    {
    	super();
    }
    
    
    /**
     * TabDelimitedFileWriter constructor.  Opens a file at the specified path for writing.
     * 
     * @param filePath  Path of the tab-delimited file to write.  If it already exists,
     * 					the file will be replaced.
     * @throws Exception
     */
    public DelimitedFileWriter(String filePath)
    throws Exception
    {
    	this(filePath, "\\t", null);
    }
    
    
    /**
     * TabDelimitedFileWriter constructor.  Opens a file at the specified path for writing
     * and writes a header row of field names.
     * 
     * @param filePath  Path of the tab-delimited file to write.  If it already exists,
     * 					the file will be replaced.
     * @param fieldNames  Names of the fields for the header row of the file.
     * @throws Exception
     */
    public DelimitedFileWriter(String filePath, String[] fieldNames)
    throws Exception
    {
    	this(filePath, "\\t", fieldNames);
    }
    
    /**
     * TabDelimitedFileWriter constructor.  Opens a file at the specified path for writing
     * and writes a header row of field names.
     * 
     * @param filePath  Path of the tab-delimited file to write.  If it already exists,
     * 					the file will be replaced.
     * @param delimiter  Delimiter for this file.
     * @throws Exception
     */
    public DelimitedFileWriter(String filePath, String delimiter)
    throws Exception
    {
    	this(filePath, delimiter, null);
    }
    	    
    /**
     * TabDelimitedFileWriter constructor.  Opens a file at the specified path for writing
     * and writes a header row of field names.
     * 
     * @param filePath  Path of the tab-delimited file to write.  If it already exists,
     * 					the file will be replaced.
     * @param delimiter  Delimiter for this file.
     * @param fieldNames  Names of the fields for the header row of the file.
     * @throws Exception
     */
    public DelimitedFileWriter(String filePath, String delimiter, String[] fieldNames)
    throws Exception
    {
    	this();
    	this.delimiter = delimiter;
    	
        fw = new FileWriter(filePath);
        if (fieldNames != null) 
        	this.writeHeaderRow(fieldNames);
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
        this.fw.write(Utils.tabJoin(fieldNames));
    }

    /* (non-Javadoc)
     * @see org.drugepi.util.RowWriter#writeRow(java.lang.String[])
     */
    public synchronized void writeRow(String[] contents)
        throws Exception
    {
    	fw.write(StringUtils.join(contents, this.delimiter));
    }

    /* (non-Javadoc)
     * @see org.drugepi.util.RowWriter#close()
     */
    public void close()
        throws Exception
    {
        fw.close();
    }
    
    public String toString()
    {
    	return null;
    }
}
