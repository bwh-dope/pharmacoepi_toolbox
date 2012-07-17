/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.match.controllers;

import java.util.*;
import java.util.concurrent.*;

import org.drugepi.match.*;
import org.drugepi.match.storage.*;

/**
 * Optimized N-way nearest neighbor matching.  EXPERIMENTAL ONLY.  
 * <p>
 * This is experimental code that is not recommended for use.
 * 
 * @author Jeremy A. Rassen
 * @version 0.0.1
 *
 */
public class NWayNearestNeighborMatch extends MatchController {
	public final String version = "N-Way Nearest Neighbor Match.  EXPERIMENTAL ONLY.";

	/**
	 * Maximum match distance.
	 */
	private static final double DEFAULT_CALIPER = 0.01;
	
	public PriorityBlockingQueue<MatchSet> matchHeap;
	private static final int NUM_HEAP_BUILDERS = 2;
	
	private NWayNearestNeighborHeapBuilder[] heapBuilders = new NWayNearestNeighborHeapBuilder[NUM_HEAP_BUILDERS];

	// need to be public for threading
	public MatchGroup baseGroup;
	private MultiMatchKDTree trees[];
	
	/**
	 * Constructor for nearest neighbor matching.
	 */
	public NWayNearestNeighborMatch(Match match, int numGroups) {
		super(match, numGroups);
		this.matchHeap = new PriorityBlockingQueue<MatchSet>(11, new MatchSetDistanceComparator());
	}
	
	protected void checkParameters()
	throws MatchException 
	{
		super.checkParameters();
		if (this.numGroups < 3)
			throw new MatchException("Only 3 or more-way matching is supported for this match mode");

	}

    List<List<MultiMatchPatient>> findEquivalentPatients(List<MultiMatchPatient> l) 
    {
    	return null;
    }
    
    protected MultiMatchPatient createPatient(String[] row) 
    throws MatchException
    {
        MultiMatchPatient patient =  new MultiMatchPatient(this.numGroups);
        
		try {
			patient.id = row[KEY_COLUMN];
		} catch (Exception e) {
			throw new MatchException("Failed to read patient IDs for all patients.");
		}
		
		try {
			patient.matchGroup = this.matchGroupMap.get(row[EXP_COLUMN]);
			if (patient.matchGroup == null)
				throw new MatchException("Failed to read valid match groups for all patients.");
		    patient.setMatchGroups(this.matchGroupsList);
		} catch (Exception e) {
			throw new MatchException("Failed to read valid match groups for all patients.");
		}

		try {
	        // get PSs for all but the last group --
	        // don't need the PS for the last group
	        for (int i = 0; i < this.matchGroupsList.size() - 1; i++) 
	        	patient.addPs(Double.parseDouble(row[PS_COLUMN + i]));
		} catch (Exception e) {
			throw new MatchException("Failed to read propensity scores for all patients.");
		}
	    
        return patient;
    }
    
    private MultiMatchKDTree buildInitialKDTree(MatchGroup group)
    {
    	int numDimensions = this.matchGroupsList.size() -1 ;
    	MultiMatchKDTree tree = new MultiMatchKDTree(numDimensions);
    	for (MatchPatient mp: group) {
    		MultiMatchPatient patient = (MultiMatchPatient) mp;
    		tree.addPatient(patient);
    	}
    	
    	tree.optimize();
    	
    	return tree;
    }

    
    public void match()
    throws Exception
    {
    	System.out.println(version);
    	
    	if (this.match.caliper == Match.INVALID_CALIPER)
    		this.match.caliper = DEFAULT_CALIPER;
    	
    	// select the smallest group
    	this.baseGroup = null;
    	for (MatchGroup mg: this.matchGroupsList) {
    		if ((baseGroup == null) ||
   				(mg.size() < baseGroup.size()))
    				baseGroup = mg;
    	}
    	List<MatchGroup> otherGroups = new ArrayList<MatchGroup>(matchGroupsList);
    	otherGroups.remove(baseGroup);
    	
    	int numOtherGroups = otherGroups.size();

    	// check!
    	MatchGroup[] groups = new MatchGroup[numOtherGroups];
    	for (int i = 0; i < numOtherGroups; i++) 
    		groups[i] = otherGroups.get(i);

    	System.out.printf("Building KD Tree(s) at %s\n", new Date().toString());

    	// build KD Trees
    	this.trees = new MultiMatchKDTree[numOtherGroups];
    	for (int i = 0; i < numOtherGroups; i++) {
    		trees[i] = buildInitialKDTree(groups[i]);
    	}
    	
    	System.out.printf("Building match heap at %s\n", new Date().toString());

    	// fork off worker threads to build local heaps, then merge all the heaps
    	// together
    	ExecutorService executor = Executors.newFixedThreadPool(NUM_HEAP_BUILDERS);
        for (int i = 0; i < NUM_HEAP_BUILDERS; i++) {
            final int threadNum = i;
            final NWayNearestNeighborMatch nnMatch = this;
            final int baseGroupSize = nnMatch.baseGroup.size();
            final int chunkSize = Math.round(baseGroupSize / NUM_HEAP_BUILDERS);
            final int startPatient = ((i == 0) ? i * chunkSize : i * chunkSize + 1);
            final int endPatient = ((i == (NUM_HEAP_BUILDERS - 1)) ? (baseGroupSize - 1) : ((i + 1) * chunkSize));
            
            this.heapBuilders[threadNum] = new NWayNearestNeighborHeapBuilder(this, i, this.trees);

            Runnable task = new Runnable() {
                public void run() {
                    try {
//                    	nnMatch.heapBuilders[threadNum].controller = nnMatch;
//                    	nnMatch.heapBuilders[threadNum].threadNum = threadNum;
                    	nnMatch.heapBuilders[threadNum].baseGroup = nnMatch.baseGroup.subList(startPatient, endPatient);
                    	nnMatch.heapBuilders[threadNum].buildHeap();
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            };

            executor.submit(task);
        }

        try {
            executor.shutdown();
            executor.awaitTermination(100 * 60 * 60, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
//    	System.out.printf("Merging heaps at %s\n", new Date().toString());
//        for (int i = 0; i < NUM_HEAP_BUILDERS; i++) 
//        	this.matchHeap.addAll(heapBuilders[i].matchHeap);

    	System.out.printf("Evaluating match heap at %s\n", new Date().toString());
    	System.out.printf("%d potential matches in the match heap\n", matchHeap.size());

		// go through the heap and collect all valid matches
		for (MatchSet ms: matchHeap) {
			if (ms.allAreUnmatched()) {
				matches.add(ms);
				ms.setAllMatched();
			}
		}
    		
    	System.out.printf("Evaluating match heap at %s\n", new Date().toString());

 		System.out.printf("%d matches made\n", matches.size());
    }
    
    protected String[] getOutputFields()
    {
    	final String[] outputFields = { "set_num", "pat_id", "group_indicator", "ps",
    									"match_distance" };
    	
    	return outputFields;
    }
    
    
    protected String[] getOutputData(int setNumber, MatchSet ms, MatchPatient p) {
       	final String quoteStr = "\"";
    	String[] outputData = {
				Integer.toString(setNumber),
				quoteStr + p.id + quoteStr,
				quoteStr + p.matchGroup.groupIndicator + quoteStr,
				Double.toString(p.getPs()),
				Double.toString(ms.distance)
		};
    	
    	return outputData;
    }
}
