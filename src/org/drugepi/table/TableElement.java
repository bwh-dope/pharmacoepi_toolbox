/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.table;

import java.util.*;

public class TableElement {
	String id;
	String description;
	public List<TableFootnote> footnotes;
	

	public TableElement(String id, String description) {
		this.id = TableElement.makeId(id);
		if (description != null)
			this.description = description.trim();
		else 
			this.description = "";
		
		this.footnotes = new ArrayList<TableFootnote>();
	}
	
	public void addFootnote(TableFootnote footnote) {
		this.footnotes.add(footnote);
	}
	
	public String getFootnotesSymbolList() {
		StringBuffer symbolList = new StringBuffer();
		
		boolean commaNeeded = false;
		for (TableFootnote f: this.footnotes) {
			if (commaNeeded == true)
				symbolList.append(",");
			symbolList.append(f.id);
			commaNeeded = true;
		}
		
		return symbolList.toString();
	}
	
	public static String makeId(String s) {
		if (s == null) 
			return null;
		
		return s.toUpperCase().trim();
	}
	
	public boolean equals(Object o) {
		if (! (o instanceof TableElement))
			return false;
		
		TableElement te = (TableElement) o;
		if (te.id.equalsIgnoreCase(this.id))
			return true;
		
		return false;
	}
	
	public String toString() {
		return String.format(
		    "ID=%s, Description = %s\n",
		    this.id, this.description);
	}
}
