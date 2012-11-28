/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

package org.drugepi.emr;

public class EmrToken {
	public String text;
	public String type;
	
	public EmrToken(String text, String type) {
		this.text = text;
		this.type = type;
	}
	
	public String toString() {
		if ((text != null) && (type != null))
			return(text + " [" + type + "]");
		
		return "Token with nulls";
	}
}
