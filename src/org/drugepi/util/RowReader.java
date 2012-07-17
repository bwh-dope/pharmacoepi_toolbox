/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.util;

/**
 * Abstract class to read rows of data from a data source.
 * 
 * @author Jeremy A. Rassen
 * @version 1.0.0
 * 
 */
public abstract class RowReader {
	protected int numColumns;

	/**
	 * RowReader constructor. 
	 * 
	 * @throws Exception
	 */
	public RowReader()
	throws Exception 
	{
		this.numColumns = -1;
	}

	/**
	 * Read the next row from the data source and return elements as an array of strings.
	 * 
	 * @return Elements read from the row.
	 * @throws Exception
	 */
	public abstract String[] getNextRow()
	throws Exception;
	
	/**
	 * Reset the reader to the first row..
	 * 
	 * @return Elements read from the row.
	 * @throws Exception
	 */
	public abstract void reset()
	throws Exception;
	
	
	/**
	 * Close the reading source.
	 * 
	 * @throws Exception
	 */
	public abstract void close()
	throws Exception;
	
	
	/**
	 * Gets the number of columns available in the data.
	 * 
	 * @return Number of columns available.
	 * @throws Exception
	 */
	public int getNumColumns()
	{
		return this.numColumns;
	}
	
}

