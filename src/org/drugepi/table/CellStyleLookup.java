/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.table;

import java.util.Hashtable;

import org.apache.poi.ss.usermodel.CellStyle;

public final class CellStyleLookup {
	private Hashtable<Integer, CellStyle> styleTable;
	
	public CellStyleLookup() {
		this.styleTable = new Hashtable<Integer, CellStyle>();
	}
	
	public CellStyle getExistingStyle(CellStyle style)
	{
		return styleTable.get(style.hashCode());
	}
	
	public void putNewStyle(CellStyle origStyle, CellStyle newStyle)
	{
		styleTable.put(origStyle.hashCode(), newStyle);
	}
}