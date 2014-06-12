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
	
	public static String styleToString(CellStyle style) {
		StringBuffer sb = new StringBuffer();
		sb.append("getDataFormatString=" + style.getDataFormatString()+ "\n");
		sb.append("getFontIndex=" + style.getFontIndex()+ "\n");
		sb.append("getHidden=" + style.getHidden()+ "\n");
		sb.append("getAlignment=" + style.getAlignment()+ "\n");
		sb.append("getWrapText=" + style.getWrapText()+ "\n");
		sb.append("getVerticalAlignment=" + style.getVerticalAlignment()+ "\n");
		sb.append("getRotation=" + style.getRotation()+ "\n");
		sb.append("getIndention=" + style.getIndention()+ "\n");
		sb.append("getBorderLeft=" + style.getBorderLeft()+ "\n");
		sb.append("getBorderRight=" + style.getBorderRight()+ "\n");
		sb.append("getBorderTop=" + style.getBorderTop()+ "\n");
		sb.append("getBorderBottom=" + style.getBorderBottom()+ "\n");
		sb.append("getLeftBorderColor=" + style.getLeftBorderColor()+ "\n");
		sb.append("getRightBorderColor=" + style.getRightBorderColor()+ "\n");
		sb.append("getTopBorderColor=" + style.getTopBorderColor()+ "\n");
		sb.append("getBottomBorderColor=" + style.getBottomBorderColor()+ "\n");
		sb.append("getFillPattern=" + style.getFillPattern()+ "\n");
		sb.append("getFillBackgroundColor=" + style.getFillBackgroundColor()+ "\n");
		sb.append("getFillForegroundColor=" + style.getFillForegroundColor()+ "\n");
		
		return sb.toString();
	}
	
	public CellStyle getExistingStyle(CellStyle style)
	{
		String styleString = styleToString(style);
		return styleTable.get(style.hashCode());
	}
	
	public void putNewStyle(CellStyle origStyle, CellStyle newStyle)
	{
		styleTable.put(origStyle.hashCode(), newStyle);
	}
}