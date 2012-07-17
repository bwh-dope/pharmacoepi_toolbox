/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.hdps;

import org.drugepi.PharmacoepiToolException;

public class HdpsException extends PharmacoepiToolException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6630593112287052936L;

	public HdpsException() {
		super();
	}
	
	public HdpsException(String s) {
		super(s);
	};
}
