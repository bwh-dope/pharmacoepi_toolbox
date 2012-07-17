/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.match.storage;

import java.util.*;

import com.savarese.spatial.*;

public class MultiMatchKDTree extends KDTree<Double, KDTreeKey, MultiMatchPatient> {
	private NearestNeighbors<Double, KDTreeKey, MultiMatchPatient> nearestNeighbor;
	
	public MultiMatchKDTree() {
		this(2);
	}

	public MultiMatchKDTree(int dimensions) {
		super(dimensions);
		nearestNeighbor = new NearestNeighbors<Double, KDTreeKey, MultiMatchPatient>();
	}

	public void addPatient(MultiMatchPatient patient) {
		this.put(patient.getKDTreeKey(), patient);
	}
	
	public void addPatient(KDTreeKey key, MultiMatchPatient patient) {
		this.put(key, patient);
	}
	
	public void doneAddingPatients() {
		this.optimize();
	}

	public MultiMatchPatient getNearestPatient(KDTreeKey k) {
		return this.getNearestPatient(k, Double.MAX_VALUE);
	}
	
	public MultiMatchPatient getNearestPatient(KDTreeKey k, double caliper) {
		List<MultiMatchPatient> l = this.getNearestPatients(k, caliper, 1);
		
		if ((l != null) && (l.size() > 0))
			return l.get(0);
		else 
			return null;
	}

	public MultiMatchPatient getNearestPatient(MultiMatchPatient p, double caliper) {
		return this.getNearestPatient(p.getKDTreeKey(), caliper);
	}

	public List<MultiMatchPatient> getNearestPatients(MultiMatchPatient p) {
		return this.getNearestPatients(p, Double.MAX_VALUE, this.size());
	}

	public List<MultiMatchPatient> getNearestPatients(MultiMatchPatient p, double caliper) {
		return this.getNearestPatients(p, caliper, this.size());
	}
	
	public List<MultiMatchPatient> getNearestPatients(MultiMatchPatient p, double caliper, 
			int maxPatients) {
		return this.getNearestPatients(p.getKDTreeKey(), caliper, this.size());
	}
	
	public List<MultiMatchPatient> getNearestPatients(KDTreeKey k, double caliper, 
			int maxPatients) {
		NearestNeighbors.Entry<Double, KDTreeKey, MultiMatchPatient> nearestList[] = null;
		
		nearestList = this.nearestNeighbor.get(this, k, maxPatients);
		
		if (nearestList.length == 0)
			return null;

		double caliper2 = caliper * caliper;
		
		List<MultiMatchPatient> l = new ArrayList<MultiMatchPatient>();

		for (int i = 0; i < nearestList.length; i++) {
			// getDistance calls sqrt, so better to compare squares
			if (nearestList[i].getDistance2() > caliper2)
				break;
			else
				l.add(nearestList[i].getNeighbor().getValue());
		}
		
		return l;
	}
}
