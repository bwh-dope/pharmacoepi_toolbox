/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.match.storage;

import java.util.Comparator;

import org.drugepi.match.controllers.MatchController;

import Jama.Matrix;

public class MatchPatient implements Comparable<Object> {
	public String id;

	public MatchGroup matchGroup;
	
	private static final MatchPatientRandomIDComparator comparator = new MatchPatientRandomIDComparator();
	
	private final static int STATUS_UNMATCHED = 0;
	private final static int STATUS_MATCHED = 1;
	private int matchStatus;
	
	public double randomId;
	
	// !!! hack
	public int counter;
	
	private PSVector psVector;
	
	public MatchPatient(int numGroups) {
		this.randomId = MatchController.getRandomizer().nextDouble();
		this.matchStatus = STATUS_UNMATCHED;
		this.psVector = new PSVector(numGroups);
		this.counter = 0;
	}
	
	public void addPs(double ps) {
		this.psVector.addToVector(ps);
		this.psVector.perturbPs();
	}
	
	public double getPs() {
		return this.getPs(0);
	}
	
	public double getPs(int i) {
		return psVector.asDoubleArray()[i];
	}

	public Double getPsAsDouble() {
		return this.getPsAsDouble(0);
	}
	
	public Double getPsAsDouble(int i) {
		return psVector.getPs(i);
	}
	
	public double[] getPsAsDoubleArray() {
		return psVector.asDoubleArray();
	}
	
	public Matrix getPsAsMatrix() {
		return psVector.asMatrix();
	}
	
	public KDTreeKey getKDTreeKey() {
		return this.psVector.getKdTreeKey();
	}

	public void setStatusUnmatched() {
		this.matchStatus = STATUS_UNMATCHED;
	}
	
	public void setStatusMatched() {
		this.matchStatus = STATUS_MATCHED;
	}
	
	public boolean isUnmatched() {
		return (this.matchStatus == STATUS_UNMATCHED);
	}
	
	public boolean isMatched() {
		return (this.matchStatus == STATUS_MATCHED);
	}

	public static Comparator<Object> getComparator()
	{
		return MatchPatient.comparator;
	}
		
	public int compareTo(Object o)
	{
		return comparator.compare(this, o);
	}
	
	public int hashCode() 
	{
		return this.id.hashCode();
	}
	
	public boolean equals(Object o) 
	{
		if (o.getClass() != this.getClass())
			return false;
		
		return (((MatchPatient) o).id.equals(this.id));
	}
}
