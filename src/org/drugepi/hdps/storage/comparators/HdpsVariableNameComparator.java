/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.hdps.storage.comparators;

import java.util.Comparator;

import org.drugepi.hdps.storage.HdpsVariable;

public class HdpsVariableNameComparator implements Comparator<HdpsVariable>
{
	public int compare(HdpsVariable var1, HdpsVariable var2)
	{
		String v1 = var1.varName;
		String v2 = var2.varName;
		
		return (v1.compareTo(v2));
	}
}

