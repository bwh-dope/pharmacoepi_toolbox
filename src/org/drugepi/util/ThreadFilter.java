/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
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

