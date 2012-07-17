/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.match;

import org.drugepi.PharmacoepiToolException;

public class MatchException extends PharmacoepiToolException {
	private static final long serialVersionUID = -4709969871002861471L;

	public MatchException() {
		super();
	}
	
	public MatchException(String s) {
		super(s);
	};
}
