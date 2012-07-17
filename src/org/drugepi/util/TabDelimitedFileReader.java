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

package org.drugepi.util;

import java.io.*;

/**
 * Read rows of data from a tab-delimited file.
 * 
 * @author Jeremy A. Rassen
 * @version 1.0.0
 * 
 */
public class TabDelimitedFileReader extends RowReader {
    private FileReader fr;
    private BufferedReader br;
    private String filePath;
 
    /**
     * TabDelimitedFileReader constructor.
     * 
     * @throws Exception
     */
    public TabDelimitedFileReader()
    throws Exception
    {
    	super();
    	fr = null;
    	br = null;
    }
    
    /**
     * TabDelimitedFileReader.  Opens a file at the specified path.
     * 
     * @param filePath  Path of the tab-delimited file to be read.
     * @throws Exception
     */
    public TabDelimitedFileReader(String filePath)
    throws Exception
    {
    	this();
    	
    	this.filePath = filePath;
    	
        fr = new FileReader(filePath);
        br = new BufferedReader(fr);

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

        fr = new FileReader(this.filePath);
        br = new BufferedReader(fr);

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
        fr.close();
    }
}
