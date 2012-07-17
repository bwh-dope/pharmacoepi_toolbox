/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.table;

import java.util.*;

public class TableRowCol extends TableElement {
	public TableRowCol parent;
	public List<TableRowCol> children;
	boolean isHeader;

	public enum RowColTypes { NORMAL, HEADER };
	public final RowColTypes rcType;
	
	public TableRowCol(String id, String description, RowColTypes rcType) {
		super(id, description);
		children = new ArrayList<TableRowCol>();
		this.rcType = rcType;
	}
	
	public TableRowCol(String id, String description) {
		this(id, description, RowColTypes.NORMAL);
	}
	
	public void addChild(TableRowCol child) {
		this.children.add(child);
	}
	
	public static int getDepth(TableRowCol rc) {
		if (rc.parent == null) 
			return 1;
		else {
			return 1 + getDepth(rc.parent);
		}
	}
	
	public static int getMaxDepth(Collection<TableRowCol> rcList) {
		int maxDepth = 0;
		for (TableRowCol rc: rcList) {
			int depth = TableRowCol.getChildrenDepth(rc);
			if (depth > maxDepth)
				maxDepth = depth;
		}
		return maxDepth;
	}
	
	public static int getChildrenDepth(TableRowCol rc) {
		if (rc.children.size() == 0) 
			return 1;
		else {
			int maxDepth = 0;
			for (TableRowCol subRc: rc.children) {
				int depth = 1 + getChildrenDepth(subRc);
				if (depth > maxDepth) 
					maxDepth = depth;
			}
			return maxDepth;
		}
	}
	
	public static int getNumLeaves(TableRowCol rc) {
		int leaves = 0;
		
		if (rc.children.size() == 0) 
			return 1;
		else {
			for (TableRowCol subRc: rc.children) {
				leaves += getNumLeaves(subRc);
			}
			return leaves;
		}
	}
	
	public String toString() {
		return String.format(
		    "ROWCOL ID=%s, Description = %s\n",
		    this.id, this.description);
	}
}

