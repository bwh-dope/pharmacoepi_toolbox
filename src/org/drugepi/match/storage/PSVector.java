/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.match.storage;

import org.drugepi.match.MatchRandomizer;
import org.drugepi.match.controllers.MatchController;

import Jama.Matrix;

public class PSVector {
	// make a 2D array in order to transform to matrix quickly
	private double[][] psVector;
	private KDTreeKey kdTreeKey;
	private int numPsAdded;

	private Matrix psMatrix;

	public PSVector(int numGroups) 
	{
		psMatrix = null;
		psVector = new double[1][numGroups - 1];
		numPsAdded = 0;
//		super();
	}
	
	public void addToVector(double d) 
	{
		this.psVector[0][numPsAdded] = d;
		this.numPsAdded++;
		psMatrix = null;
		
		this.kdTreeKey = new KDTreeKey(psVector[0]);
	}
	
	public void perturbPs() 
	{
		MatchRandomizer randomizer = MatchController.getRandomizer();
		
		// perturb d very slightly
		double r = (randomizer.nextDouble() / (randomizer.nextBoolean() ? 10000000000000d : -10000000000000d));
		for (int i = 0; i < this.numPsAdded; i++)
			psVector[0][i] += r;
	}
	
	public double[] asDoubleArray() {
		return this.psVector[0];
	}
	
	public Matrix asMatrix() {
		// create a matrix on demand
		if (this.psMatrix == null) {
			this.psMatrix = new Matrix(psVector); 
		}
		
		return this.psMatrix;
	}
	
	public double getPs(int i)
	{
		return this.psVector[0][i];
	}

	public KDTreeKey getKdTreeKey() {
		return kdTreeKey;
	}
}
