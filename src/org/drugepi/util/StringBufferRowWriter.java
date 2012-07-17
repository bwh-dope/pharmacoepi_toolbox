/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.util;

import java.io.*;

/**
 * Write rows of data to a tab-delimited file.
 * 
 * @author Jeremy A. Rassen
 * @version 1.0.0
 * 
 */
public class StringBufferRowWriter extends RowWriter {
    private StringWriter sw;

    /**
     * StringBufferRowWriter constructor.
     * 
     * @throws Exception
     */
    public StringBufferRowWriter()
    throws Exception
    {
    	super();
    	sw = new StringWriter();
    }
    
    
    /**
     * StringBufferRowWriter constructor.  Creates a buffer for writing and writes
     * a header row with the field names.
     * 
     * @param fieldNames  Names of the fields for the header row of the file.
     * @throws Exception
     */
    public StringBufferRowWriter(String[] fieldNames)
    throws Exception
    {
    	this();
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
        this.sw.write(Utils.tabJoin(fieldNames));
    }
    
    /* (non-Javadoc)
     * @see org.drugepi.util.RowWriter#writeRow(java.lang.String[])
     */
    public synchronized void writeRow(String[] contents)
        throws Exception
    {
    	sw.write(Utils.tabJoin(contents));
    }
    
    public String toString()
    {
    	if (sw != null) 
    		return sw.toString();
    	return null;
    }

    /* (non-Javadoc)
     * @see org.drugepi.util.RowWriter#close()
     */
    public void close()
        throws Exception
    {
    }

}
