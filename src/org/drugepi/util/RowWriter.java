/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.util;

/**
 * Abstract class to write rows of data to a database or file.
 * 
 * @author Jeremy A. Rassen
 * @version 1.0.0
 */
public abstract class RowWriter {
	/**
	 * RowWriter constructor.
	 * 
	 * @throws Exception
	 */
	public RowWriter()
	throws Exception 
	{
	}

	/**
	 * Write a row of data with one column per element in the contents specified.
	 * 
	 * @param contents  The contents to write.
	 * @throws Exception
	 */
	public abstract void writeRow(String[] contents)
	throws Exception;
	
	
    /**
     * Writes a header row of field names to the output stream.
     * 
     * @param fieldNames  Names of the fields for the header row of the file.
	 * @throws Exception
	 */
	public abstract void writeHeaderRow(String[] fieldNames)
	throws Exception;
	
	/**
	 * Close the writing target.
	 * 
	 * @throws Exception
	 */
	public abstract void close() 
	throws Exception;
	
	public abstract String toString();
}
