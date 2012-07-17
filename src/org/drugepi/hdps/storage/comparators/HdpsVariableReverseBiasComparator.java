/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.hdps.storage.comparators;

import org.drugepi.hdps.storage.HdpsVariable;

public class HdpsVariableReverseBiasComparator extends HdpsVariableRankingComparator
{
	public int compare(HdpsVariable var1, HdpsVariable var2)
	{
		double v1 = var1.biasRankingVariable;
		double v2 = var2.biasRankingVariable;
		
		return(this.doComparison(var1, var2, v1, v2));
	}
}

