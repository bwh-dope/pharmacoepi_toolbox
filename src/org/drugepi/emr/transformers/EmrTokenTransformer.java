/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

package org.drugepi.emr.transformers;

import org.drugepi.emr.EmrToken;

/**
 * Generic class to transform EMR tokens.  New transformers can be subclassed from here.  
 * This transform does nothing; returns the token input.
 * 
 * @author Elaine Angelino
 * @author Jeremy A. Rassen
 */
public class EmrTokenTransformer {
	public EmrToken transform(EmrToken t) {
		return t;
	}
}
