/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.util;

import java.io.File;
import java.util.*;

import org.drugepi.hdps.storage.HdpsVariable;

/**
 * Miscellaneous utilities for the Pharamcoepi Toolbox.
 * 
 * @author Jeremy A. Rassen
 * @version 1.0.0
 */
public class Utils {
	/**
	 * Pick the top N items from the specified list.
	 * 
	 * @param originalList  The list to pick the top N items from.
	 * @param topN	Number of items to pick.
	 * @param comparator  Comparator with which to rank the items in the list.
	 * @return A new list containing the top N items.
	 */
	public static ArrayList<?> selectTopN(List<?> originalList, int topN, Comparator<Object> comparator)
	{
    	ArrayList<Object> newList = new ArrayList<Object>(topN);
    	
    	int numIncluded = 0;
    	Object lastObject = null;
    	
    	for (Object o: originalList) {
    		if (numIncluded < topN) {
    			newList.add(o);
    			
    			// allow for ties
    			if (comparator.compare(o, lastObject) != 0) {
    				numIncluded++;
    				lastObject = o;
    			}
    		}
    	}
		
		return newList;
	}
	
	/**
	 * Given a directory and a file name, construct a canonical file path.
	 * 
	 * @param directory	Directory name.
	 * @param fileName File name.
	 * @return  File path combining directory name and file name.
	 */
	public static String getFilePath(String directory, String fileName)
	{
		File f = new File(directory, fileName);
		return f.toString();
	}
	
	/**
	 * Join a collection using the specified delimiter.
	 * 
	 * @param s The collection to join.
	 * @param delimiter The delimiter.
	 * @return
	 */
	public static String join(Collection<String> s, String delimiter) {
		return Utils.join(s, delimiter, "");
    }
	
	/**
	 * Join a collection using the specified delimiter, enclosing each element in the 
	 * specified enclosing string.
	 * 
	 * @param s The collection to join.
	 * @param delimiter The delimiter.
	 * @return
	 */
	public static String join(Collection<String> s, String delimiter, final String enclosure) {
        StringBuffer buffer = new StringBuffer();
        Iterator<String> iter = s.iterator();
        while (iter.hasNext()) {
            buffer.append(enclosure + iter.next() + enclosure);
            if (iter.hasNext()) {
                buffer.append(delimiter);
            } 
        }
        return buffer.toString();
    }

	/**
	 * Formats a double for file output.  Changes NaN to .
	 * 
	 * @throws Exception
	 */
	public static String formatOutputDouble(double d) {
		if ((Double.isNaN(d)) ||
			(Double.isInfinite(d)) ||
			(d == HdpsVariable.INVALID))
			return ".";
		
		return String.format("%.10f", d);
	}	
	
	/**
	 * Reads a double from an output file.  Changes . to NaN.
	 * 
	 * @throws Exception
	 */
	public static double parseInputDouble(String s) {
		if (s.equals("."))
			return Double.NaN;
		else 
			return Double.parseDouble(s);
	}	
	
    
	/**
	 * Joins an array of strings into a single tab-delimited string.
	 * 
	 * @throws Exception
	 */
	public static String tabJoin(String[] contents)
    {
    	if (contents.length == 0)
    		return null;
    	
    	final String tab = "\t";
    	
    	StringBuilder sb = new StringBuilder(contents[0]);
    	for (int i = 1; i < contents.length; i++) {
    		sb.append(tab + contents[i]);
    	}
    	sb.append("\n");
    	
    	return sb.toString();
    }
}
