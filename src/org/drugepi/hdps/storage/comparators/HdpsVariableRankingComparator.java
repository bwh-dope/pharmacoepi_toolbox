/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.hdps.storage.comparators;

import java.util.Comparator;

import org.drugepi.hdps.storage.HdpsVariable;

public class HdpsVariableRankingComparator implements Comparator<HdpsVariable>
{
	private static final double kReallySmall = -99999999.0;
	
	protected int doComparison(HdpsVariable var1, HdpsVariable var2, double v1, double v2)
	{
		// Make NaN is the smallest value
		if ((Double.isNaN(v1)) || (v1 == HdpsVariable.INVALID))
			v1 = kReallySmall;
		
		if ((Double.isNaN(v2)) || (v2 == HdpsVariable.INVALID))
			v2 = kReallySmall;
		
		// break ties with the name of the code
		int c = Double.compare(v2, v1);
		if (c == 0)
			c = var1.code.codeString.compareTo(var2.code.codeString);
		
		return c;
	}
	
	public int compare(HdpsVariable var1, HdpsVariable var2)
	{
		double v1 = var1.activeRankingVariable;
		double v2 = var2.activeRankingVariable;
		
		return(this.doComparison(var1, var2, v1, v2));
	}
}

