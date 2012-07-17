/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.table;

public class TableCell extends TableElement {
	public TableRowCol row;
	public TableRowCol col;

	public TableCell(String id, String description) {
		super(id, description);
	}

	public TableCell(String rowId, String colId, String description) {
		super(TableCell.getCellId(rowId, colId), description);
	}
	
	public static String getCellId(String rowId, String colId) {
		return rowId + "||" + colId;
	}
	
	public String toString() {
		return String.format(
		    "CELL ID=%s, Row = %s, Column = %s, Description = %s\n",
		    this.id, 
		    (this.row != null ? this.row.id : "null"),
		    (this.col != null ? this.col.id : "null"),
		    this.description);
	}
}
