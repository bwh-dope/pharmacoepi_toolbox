/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.match.controllers;

import java.util.*;

import org.drugepi.match.*;
import org.drugepi.match.storage.*;

/**
 * Optimized multi-way nearest neighbor matching.  EXPERIMENTAL ONLY.  
 * <p>
 * This is experimental code that is not recommended for use.
 * 
 * @author Jeremy A. Rassen
 * @version 0.0.3
 *
 */
public class ThreeWayNearestNeighborMatch extends MatchController {
	public final String version = "Three-Way Nearest Neighbor Match";

	/**
	 * Maximum match distance.
	 */
	private static final double DEFAULT_CALIPER = 0.01;
	
	private PriorityQueue<MatchSet> matchHeap;
	
	private MatchGroup redGroup;
	private MatchGroup blueGroup;
	private MatchGroup greenGroup;

	/**
	 * Constructor for nearest neighbor matching.
	 */
	public ThreeWayNearestNeighborMatch(Match match) {
		super(match, 3); 
		this.matchHeap = new PriorityQueue<MatchSet>(11, new MatchSetDistanceComparator());
	}
	
	protected void checkParameters()
	throws MatchException 
	{
		super.checkParameters();
		if (this.numGroups != 3)
			throw new MatchException("Only 3-way matching is supported for this match mode");
 
	}

	
    List<List<MultiMatchPatient>> findEquivalentPatients(List<MultiMatchPatient> l) 
    {
    	return null;
    }
    
    protected MultiMatchPatient createPatient(String[] row) 
    throws MatchException 
    {
        MultiMatchPatient patient =  new MultiMatchPatient(3);
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
    
    private MatchSet makeMatchSet(MatchPatient p1, MatchPatient p2, 
    		MatchPatient p3, double distance)
    {
    	if (distance > this.match.caliper) 
    		return null;
    	
		MatchSet ms = new MatchSet(3);
		ms.put(p1);
		ms.put(p2);
		ms.put(p3);
		ms.distance = distance;
		
		return ms;
    }
    
	public static int gqs = 0;
	public int cmpCnt = 0;
	public boolean debug = false;
    
    public void addPutativeMatches(MatchPatient pr, MatchPatientKDTree kblue, 
    		MatchPatientKDTree kgreen) 
    throws Exception {
		PriorityQueue<MatchSet> tempq = new PriorityQueue<MatchSet>(11, new MatchSetDistanceComparator());

		MatchPatient nb = kblue.nearest(pr);
		MatchPatient nbg = kgreen.nearest(nb);
		// ????
		gqs++;

		if (nb == null || nbg == null) {
			throw new Exception("KDTree issue.  " + pr.id
					+ " has no nearest neighbor! tree emtpy?");
		}

		// triangle with closest blue and its closest green
		double small = MatchDistanceCalculator.abhiDistance(pr, nb) + 
		 			   MatchDistanceCalculator.abhiDistance(pr, nbg) + 
		 			   MatchDistanceCalculator.abhiDistance(nb, nbg); 
		
		MatchSet ms = this.makeMatchSet(pr, nb, nbg, small);
		if (ms != null)
			tempq.add(ms);
//
//		MatchSet ms = new MatchSet(3);
//		ms.put(pr);
//		ms.put(nb);
//		ms.put(nbg);
//		ms.first = true;
//		ms.distance = small;
//		tempq.add(ms);
		
		if (debug) {
//			System.out
//					.format("AddPutMat r:%03d nb:%03d ng:%03d drb:%.06f drg:%.06f small:%.05f\n",
//							pr.id, nb.id, nbg.id, MatchDistanceCalculator.abhiDistance(pr, nb),
//							MatchDistanceCalculator.abhiDistance(pr, nbg), small);
		}

		// these lists are in sorted order
		List<MatchPatient> nbs = kblue.nearest(pr, small / 2);
		List<MatchPatient> ngs = kgreen.nearest(pr, small / 2);

//		if (pr.i == debugPoint) {
//			// log("\\coordinate (r"+pr.i+") at ("+pr.d[0]+","+pr.d[1]+");\\draw[red!80,opacity=0.6] (r"+pr.i+") node {$\\scriptscriptstyle "+pr.i+"$} circle (0.001pt);\n");
//			System.out.format(
//					"   red %3d has %03d blues and %03d greens w/in %.5f\n",
//					pr.i, nbs.size(), ngs.size(), small / 2);
//		}

		for (MatchPatient cb: nbs) {
			double pcb = MatchDistanceCalculator.abhiDistance(pr, cb);
			
			for (MatchPatient cg: ngs) {
				double dist = pcb + MatchDistanceCalculator.abhiDistance(pr, cg) + 
					MatchDistanceCalculator.abhiDistance(cb, cg);
				
				cmpCnt++;
				if (dist < small) {
					if (debug) {
//						System.out.format(
//								"   candidate : [%3d %3d %3d] %.5f\n", pr.id,
//								cb.id, cg.id, dist);
					// important: only add as putative match if it is smaller
					// else the match is not guaranteed to be optimal
//					tempq.add(new match(pr, cb, cg, dist));
					}
					
					MatchSet ms2 = this.makeMatchSet(pr, cb, cg, dist);
					if (ms2 != null)
						tempq.add(ms2);
				}
			}
		}

		// add the top N matches to the pq
		int cnt = 10;
		if (tempq.size() < cnt) {
			cnt = tempq.size();
		}
		
		for (int i = 0; i < cnt; i++) {
			MatchSet m = tempq.poll();
			if (debug) {
//				System.out.format("   adding: [%3d %3d %3d] %.5f\n", m.r.i,
//						m.b.i, m.g.i, m.dist);
			// if (m.r.i==debugPoint) { log(m.b,"b"); log(m.g,"g");
			// log("\\draw[very thin,dashed] (r"+m.r.i+") -- (b"+m.b.i+") -- (g"+m.g.i+") --cycle;\n");
			// }
			}
			this.matchHeap.add(m);
			m.get(this.redGroup).counter++;
		}
	}

    
    public void match()
    throws Exception
    {
    	System.out.println(version);
    	
    	if (this.match.caliper == Match.INVALID_CALIPER)
    		this.match.caliper = DEFAULT_CALIPER;
    	
    	// select the smallest group
    	for (MatchGroup mg: this.matchGroupsList) {
    		if ((redGroup == null) ||
   				(mg.size() < redGroup.size()))
    				this.redGroup = mg;
    	}
    	List<MatchGroup> otherGroups = new ArrayList<MatchGroup>(matchGroupsList);
    	otherGroups.remove(redGroup);
    	
    	this.blueGroup = otherGroups.get(0);
    	this.greenGroup = otherGroups.get(1);

    	System.out.printf("Building KD Tree(s) at %s\n", new Date().toString());

    	MatchPatientKDTree blueTree = MatchPatientKDTree.makeTree(blueGroup);
    	MatchPatientKDTree greenTree = MatchPatientKDTree.makeTree(greenGroup);
    	
    	System.out.printf("Building match heap at %s\n", new Date().toString());

    	for (MatchPatient redPatient: redGroup) {
    		addPutativeMatches(redPatient, blueTree, greenTree);
    	}
    	
    	System.out.printf("Evaluating match heap at %s\n", new Date().toString());
    	System.out.printf("%d potential matches in the match heap\n", matchHeap.size());
    		
		int count = 0;
		MatchSet m = null;
		while ((m = this.matchHeap.poll()) != null) {
			MatchPatient redPatient = m.get(this.redGroup);
			
			if (m.allAreUnmatched()) {
				m.setAllMatched();

				// remove points from kd trees
				MatchPatient bluePatient = m.get(blueGroup);
				MatchPatient greenPatient = m.get(greenGroup);
				
				blueTree.deletepoint(bluePatient);
				greenTree.deletepoint(greenPatient);
				String prefix = "  ";
				this.matches.add(m);
//				if (m.first) {
//					prefix = "**";
//				}
				if (debug )
					System.out.println(prefix + m + " redcnt:" + redPatient.counter
							+ " " + count + " gqs:" + gqs);
				count++;
			} else if (redPatient.isUnmatched()) {
				MatchPatient bluePatient = m.get(blueGroup);
				MatchPatient greenPatient = m.get(greenGroup);

				if (debug )
					System.out.println("  Red " + redPatient.id + " clean, but "
							+ "green " + greenPatient.id + " and blue " + bluePatient.id + " are not. redcnt="
							+ redPatient.counter);
				redPatient.counter--;
				if (redPatient.counter == 0) {
					addPutativeMatches(redPatient, blueTree, greenTree);
				}
			}
		}
	
 		System.out.printf("%d matches made\n", matches.size());
 		
// 		for (int i = 0; i < 100; i++) {
// 			MatchSet ms = this.matches.get(i);
// 			System.out.println(ms.toString());
// 		}
    }
}
