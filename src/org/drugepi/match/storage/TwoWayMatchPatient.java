/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.match.storage;

import java.util.PriorityQueue;

public class TwoWayMatchPatient extends MatchPatient {
	private static final double[] digitMaskValues = {
		1d,				// 10^0
		10d,			// 10^1
		100d,			// 10^2
		1000d,			// 10^3
		10000d,			// 10^4
		100000d,		// 10^5
		1000000d,		// 10^6
		10000000d,		// 10^7
		100000000d,		// 10^8
		1000000000d,	// 10^9
		10000000000d	// 10^10
	};

	public static final GreedyMatchPatientComparator greedyMatchComparator = new GreedyMatchPatientComparator();

	private static final MatchSetDistanceComparator msDc = new MatchSetDistanceComparator();
	
	public int rightMatchCount;
	public int leftMatchCount;
	public int matchCount;
	
	public PriorityQueue<MatchSet> leftReserveSets;
	public PriorityQueue<MatchSet> rightReserveSets;

	public int maskedNumDigits;
	public double maskedValue;
	
	public TwoWayMatchPatient nextInGroup;
	public TwoWayMatchPatient prevInGroup;
	
	public TwoWayMatchPatient() {
		super(2);
		
		matchCount = 0;
		leftMatchCount = 0;
		rightMatchCount = 0;
		
		maskedNumDigits = -1;
		matchCount = 0;
		
		nextInGroup = null;
		prevInGroup = null;
	}
	
	public void addReserveMatchSet(MatchSet ms, boolean addToLeft)
	{
		PriorityQueue<MatchSet> l = (addToLeft ? leftReserveSets : rightReserveSets);
		if (l == null)
			l = new PriorityQueue<MatchSet>(5, msDc);
		
		l.add(ms);
	}
	
	public void clearReservedMatchSets() 
	{
		if (this.rightReserveSets != null)
			this.rightReserveSets.clear();
	
		if (this.leftReserveSets != null)
			this.leftReserveSets.clear();
	}
	
	public double applyDigitMask(int numDigits)
	{
		if (this.maskedNumDigits != numDigits) {
			double d = this.getPs();
			
			// get the part after the decimal point
			if (d > 1d)
				return -1;
			
			double digitMaskValue;
			
			if (numDigits < digitMaskValues.length) 
				digitMaskValue = digitMaskValues[numDigits];
			else
				digitMaskValue = Math.pow(10d, (double) numDigits);
			
			d = d * digitMaskValue;
			d = Math.round(d);
			d = d / digitMaskValue;
			
			this.maskedNumDigits = numDigits;
			this.maskedValue = d;
		}
		
		return this.maskedValue;
	}
	
	public TwoWayMatchPatient getNextPatientInThisGroup() {
		return nextInGroup;
	}

	public TwoWayMatchPatient getPrevPatientInThisGroup() {
		return prevInGroup;
	}
	
	public MatchSet getReserveMatchSet(boolean leftSide)
	{
		PriorityQueue<MatchSet> l = (leftSide ? leftReserveSets : rightReserveSets);
		
		if (l != null)
			return(l.poll());
		
		return null;
	}

	public boolean isToTheRightOf(TwoWayMatchPatient p)
	{
		return (this.getPs() > p.getPs());
	}
	
	public void removeFromList() {
		if (this.nextInGroup != null)
			this.nextInGroup.prevInGroup = this.prevInGroup;
		
		if (this.prevInGroup != null)
			this.prevInGroup.nextInGroup = this.nextInGroup;
	}

	public void setNextPatientInThisGroup(TwoWayMatchPatient p)
	{
		this.nextInGroup = p;
		p.prevInGroup = this;
		p.nextInGroup = null;
	}	
}
