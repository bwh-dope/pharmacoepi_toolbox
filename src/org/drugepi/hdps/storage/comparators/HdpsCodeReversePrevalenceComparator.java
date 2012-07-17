/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.hdps.storage.comparators;

import java.util.Comparator;

import org.drugepi.hdps.storage.HdpsCode;

public class HdpsCodeReversePrevalenceComparator implements Comparator<Object>
{
	public int compare(Object code1, Object code2)
	{
		double p1 = ((HdpsCode) code1).prevalence;
		double p2 = ((HdpsCode) code2).prevalence;
		
		if (p1 < p2)
			return 1;

		if (p1 > p2)
			return -1;
		
		return 0;
	}
}

