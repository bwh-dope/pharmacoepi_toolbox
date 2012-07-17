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

//package org.drugepi.util;
//
//import java.util.LinkedList;
//import java.util.List;
//
//public class ThreadFilter
//{
//    public int numThreads;
//
//    private List<String[]> filterList;
//
//    public ThreadFilter()
//    {
//        this.numThreads = 0;
//        filterList = new LinkedList<String[]>();
//    }
//
//    public void autoAddFilters(int numFilters)
//    {
//    	for (char c = 'A'; c <= 'Z'; c++)
//    		this.addFilter(Character.toString(c), Character.toString(c));
//    	for (char c = '0'; c <= '9'; c++)
//    		this.addFilter(Character.toString(c), Character.toString(c));
//    }
//    
//    public void addFilter(String filterMin, String filterMax)
//    {
//    	String[] filterInfo = new String[2];
//    	
//        filterInfo[0] = filterMin;
//        filterInfo[1] = filterMax;
//        
//        filterList.add(filterInfo);
//        
//        numThreads = filterList.size();
//    }
//
//    public String getFilterMin(int threadNum)
//    {
//    	String[] filterInfo = filterList.get(threadNum);
//        return filterInfo[0];
//    }
//
//    public String getFilterMax(int threadNum)
//    {
//    	String[] filterInfo = filterList.get(threadNum);
//        return filterInfo[1];
//    }
//    
//    public static boolean meetsFilterCriteria(String key, String filterMin, String filterMax, int filterLength)
//    {
//    	String s = key.substring(0, filterLength);
//    	
//    	boolean meetsFilterCriteria = (s.compareToIgnoreCase(filterMin) >= 0 &&
//    								   s.compareToIgnoreCase(filterMax) <= 0);
//    
//    	return meetsFilterCriteria;
//    }
//    	
//
//    public static boolean meetsFilterCriteria(String key, String filterMin, String filterMax)
//    {
//    	return(ThreadFilter.meetsFilterCriteria(key, filterMin, filterMax, filterMin.length()));
//    }
//}

