/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.match.storage;

import java.util.Comparator;

public class MatchPatientRandomIDComparator implements Comparator<Object>
{
	public int compare(Object o1, Object o2)
	{
		double d1 = ((MatchPatient) o1).randomId;
		double d2 = ((MatchPatient) o2).randomId;
		
		return (Double.compare(d1, d2));
	}
}
