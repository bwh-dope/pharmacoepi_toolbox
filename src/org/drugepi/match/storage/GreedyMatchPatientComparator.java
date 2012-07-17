/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.match.storage;

import java.util.Comparator;

public class GreedyMatchPatientComparator implements Comparator<TwoWayMatchPatient>
{
	public int compare(TwoWayMatchPatient o1, TwoWayMatchPatient o2)
	{
		double d1 = o1.maskedValue;
		double d2 = o2.maskedValue;
		
		int compare = Double.compare(d1, d2);
		
		if (compare == 0) {
			d1 = o1.randomId;
			d2 = o2.randomId;
			compare = Double.compare(d1, d2);
		}
		
		return compare;
	}
}
