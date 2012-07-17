/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.match.storage;

import java.util.*;

public class MatchGroup extends ArrayList<MatchPatient> {
	private static final long serialVersionUID = -5624288420076257432L;
	
	public int groupNumber;
	public String groupIndicator;
	
	public MatchGroup() { 
		super();
	}
	
	public MatchGroup(String indicator)
	{
		this();
		this.groupIndicator = indicator;
	}
	
	public void shuffle() {
		Collections.shuffle(this);
	}
	
	public void sort(Comparator<Object> comparator) {
		Collections.sort(this, comparator);
	}

	public int hashCode() {
		return this.groupIndicator.hashCode();
	}
	
	public boolean equals(Object o) {
		if (this.getClass() != o.getClass()) 
			return false;
		
		MatchGroup omg = (MatchGroup) o;
		return (omg.groupIndicator.equals(this.groupIndicator));
	}
}
