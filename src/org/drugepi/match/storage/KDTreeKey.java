/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.match.storage;

import java.util.List;

import com.savarese.spatial.GenericPoint;

public class KDTreeKey extends GenericPoint<Double> {
	public KDTreeKey(int dimensions) {
		super(dimensions);
	}

	public KDTreeKey(double[] psList) {
		super(psList.length);

		this.addAll(psList);
	}

//	public KDTreeKey(List<Double> psList) {
//		super(psList.size());
//
//		this.addAll(psList);
//	}

	public void addAll(double[] psList) {
		for (int i = 0; i < psList.length; i++)
			this.setCoord(i, psList[i]);
	}
	
	public void addAll(List<Double> psList) {
		for (int i = 0; i < psList.size(); i++)
			this.setCoord(i, psList.get(i));
	}
}