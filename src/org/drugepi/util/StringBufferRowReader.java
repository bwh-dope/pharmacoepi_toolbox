/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

package org.drugepi.util;

import java.io.*;

/**
 * Read rows of data from a supplied string of character data.
 * 
 * @author Jeremy A. Rassen
 * @version 1.0.0
 * 
 */
public class StringBufferRowReader extends RowReader {
	private String buf;
	private StringReader sr;
	private BufferedReader br;
	
    /**
     * StringBufferRowReader constructor.
     * 
     * @throws Exception
     */
    public StringBufferRowReader()
    throws Exception
    {
    	super();
    	this.sr = null;
    	this.br = null;    	
    }
    
    /**
     * TabDelimitedFileReader.  Opens a file at the specified path.
     * 
     * @param filePath  Path of the tab-delimited file to be read.
     * @throws Exception
     */
    public StringBufferRowReader(String buf)
    throws Exception
    {
    	this();
    	this.buf = buf;

        sr = new StringReader(buf);
        br = new BufferedReader(sr);

        // toss the first line
        String line = br.readLine();
        if (line != null) {
            String[] row = line.split("\\t");
            this.numColumns = row.length;
        }
    }
    
     /* (non-Javadoc)
     * @see org.drugepi.util.RowReader#getNextRow()
     */
    public String[] getNextRow()
        throws Exception
    {
        String[] row = null;

        String line = br.readLine();
        if (line != null) {
            row = line.split("\\t");
        }
        
//        // trim quotes around the string
//        for (int i = 0; i < row.length; i++) {
//        	if ((row[i].startsWith("\"")) &&
//        		(row[i].endsWith("\"")))
//        		row[i] = row[i].substring(1, row[i].length() - 1);
//        }

        return row;
    }
    
    public void reset()
    throws Exception
    {
    	this.close();

        sr = new StringReader(this.buf);
        br = new BufferedReader(sr);

        // toss the first line
        br.readLine();
    }

    /* (non-Javadoc)
     * @see org.drugepi.util.RowReader#close()
     */
    public void close()
        throws Exception
    {
        br.close();
        sr.close();
        
        sr = null;
    }
}
