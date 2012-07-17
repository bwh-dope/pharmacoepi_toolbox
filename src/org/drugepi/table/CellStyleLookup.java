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