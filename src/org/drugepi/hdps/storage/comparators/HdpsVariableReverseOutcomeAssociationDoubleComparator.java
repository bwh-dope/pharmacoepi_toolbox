/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.hdps.storage.comparators;

import java.util.Comparator;

import org.drugepi.hdps.storage.HdpsVariable;

public class HdpsVariableReverseOutcomeAssociationDoubleComparator 
implements Comparator<Double>
{
	private static final double kReallySmall = -99999999.0;
	
	public int compare(Double var1, Double var2)
	{
		// Make NaN is the smallest value
		if ((Double.isNaN(var1)) || (var1 == HdpsVariable.INVALID))
			var1 = kReallySmall;
		
		if ((Double.isNaN(var2)) || (var2 == HdpsVariable.INVALID))
			var2 = kReallySmall;

		return Double.compare(var2, var1);
	}
}

