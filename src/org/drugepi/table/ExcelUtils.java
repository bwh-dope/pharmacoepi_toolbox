/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.table;

import java.util.Hashtable;

import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFColor;

public class ExcelUtils {
	public static final String CELL_ID_DIVIDER_REGEX = "\\/\\/";
	public static final String FOOTNOTE_DIVIDER_REGEX = "\\^\\^";
	
	private static class ColorRepresentation {
		public String xssfArgb;
		public String hssfHexString;
		
		public ColorRepresentation(String xssfArgb, String hssfHexString) {
			this.xssfArgb = xssfArgb;
			this.hssfHexString = hssfHexString;
		}
	}
	
	public final static ColorRepresentation COLUMN_DEF_COLOR = 
		new ColorRepresentation("FF0000FF", "0:0:D4D4");
	public final static ColorRepresentation ROW_DEF_COLOR = 
		new ColorRepresentation("FFFF0000", "DDDD:0808:0606");
	public final static ColorRepresentation TABLE_DESCRIPTION_COLOR = 
		new ColorRepresentation("FFFFFF00", "FCFC:F3F3:0505");
	public final static ColorRepresentation ROW_HEADER_COLOR = 
		new ColorRepresentation("FFFF6600", "FFFF:6666:0");
	public final static ColorRepresentation FOOTNOTE_DEF_COLOR = 
		new ColorRepresentation("FF00FF00", "0:6464:1111");

	public ExcelUtils() {
		super();
	}

	// table of known cell styles, mapped to restyled cell styles
	public static Hashtable<Integer, CellStyle> cellStyleFactory()
	{ 
		return new Hashtable<Integer, CellStyle>();
	}

	private static boolean cellIsColor(Cell cell, ColorRepresentation color) 
	{
		if (cell == null)
			return false;
		
		CellStyle style = cell.getCellStyle();
		
		Color bgColor = style.getFillForegroundColorColor();
		
		if (bgColor instanceof XSSFColor) {
			XSSFColor xssfBgColor = (XSSFColor) bgColor;
//			System.out.printf("Cell color is %s index is %d %d\n", xssfBgColor.getARGBHex(), xssfBgColor.getRgb()[0], xssfBgColor.getIndexed());
			return xssfBgColor.getARGBHex().equalsIgnoreCase(color.xssfArgb);
		} else if (bgColor instanceof HSSFColor) {
			HSSFColor hssfBgColor = (HSSFColor) bgColor;
//			System.out.printf("Cell color is %s\n", hssfBgColor.getHexString());
			return hssfBgColor.getHexString().equalsIgnoreCase(color.hssfHexString);
		} else
			return false;
	}
	
	public static boolean cellIsBold(Cell cell) 
	{
		if (cell == null)
			return false;
		
		Row row = cell.getRow();
		Sheet sheet = row.getSheet();
		Workbook workbook = sheet.getWorkbook();
		Font font = workbook.getFontAt(cell.getCellStyle().getFontIndex());

		if (font.getBoldweight() == Font.BOLDWEIGHT_BOLD)
			return true;
		
		return false;
	}
	
	public static boolean cellIsRowDefinition(Cell cell)
	{
		return ((cellIsStringOrBlank(cell)) && (cellIsColor(cell, ROW_DEF_COLOR)));
	}
	
	public static boolean cellIsColumnDefinition(Cell cell)
	{
		return ((cellIsStringOrBlank(cell)) && (cellIsColor(cell, COLUMN_DEF_COLOR)));
	}
	
	public static boolean cellIsTableDescription(Cell cell)
	{
		return ((cellIsStringOrBlank(cell)) && (cellIsColor(cell, TABLE_DESCRIPTION_COLOR)));
	}

	public static boolean cellIsRowHeader(Cell cell)
	{
		return ((cellIsStringOrBlank(cell)) && (cellIsColor(cell, ROW_HEADER_COLOR)));
	}

	public static boolean cellIsFootnoteDefinition(Cell cell)
	{
		return ((cellIsStringOrBlank(cell)) && (cellIsColor(cell, FOOTNOTE_DEF_COLOR)));
	}
	
	public static void restyleCell(CellStyleLookup csl, Cell cell)
	{
		CellStyle origStyle = cell.getCellStyle();
		
		CellStyle newStyle =  csl.getExistingStyle(origStyle); 
		if (newStyle == null) {
			newStyle = cell.getRow().getSheet().getWorkbook().createCellStyle();
			newStyle.cloneStyleFrom(origStyle);
			newStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
			csl.putNewStyle(origStyle, newStyle);
		}

		cell.setCellStyle(newStyle);
	}
	
	private static final String[] decimalFormats = {
			"0",			// 0
			"0.0", 			// 1
			"0.00", 		// 2
			"0.000", 		// 3
			"0.0000", 		// 4
			"0.00000", 		// 5
			"0.000000", 	// 6
			"0.0000000", 	// 7
			"0.00000000" 	// 8
	};
	
	private static final String[] percentFormats = {
		"0%",			// 0
		"0.0%", 		// 1
		"0.00%", 		// 2
		"0.000%", 		// 3
		"0.0000%", 		// 4
		"0.00000%", 	// 5
		"0.000000%", 	// 6
		"0.0000000%", 	// 7
		"0.00000000%"	// 8
	};
	
	public static void formatNumericCell(Cell cell, String s, String[] excelFormats) 
	{
		CellStyle origStyle = cell.getCellStyle();
		CellStyle newStyle = cell.getRow().getSheet().getWorkbook().createCellStyle();
		DataFormat newFormat = cell.getRow().getSheet().getWorkbook().createDataFormat();
		newStyle.cloneStyleFrom(origStyle);
		
		newStyle.setAlignment(CellStyle.ALIGN_LEFT);
		
		int numDecimals = -1;
		if (s != null) {
			int decimalIndex = s.indexOf(".");
			if (decimalIndex >= 0)
				numDecimals = s.length() - decimalIndex - 1;
		}
		
		if (numDecimals < 0) 
			numDecimals = 0;
		
		if ((numDecimals >= 0) && (numDecimals <= 8))
			newStyle.setDataFormat(newFormat.getFormat(excelFormats[numDecimals]));
		
		cell.setCellStyle(newStyle);
	}
	
	public static void formatDecimalCell(Cell cell, String s) 
	{
		formatNumericCell(cell, s, decimalFormats);
	}
	
	public static void formatPercentageCell(Cell cell, String s) 
	{
		formatNumericCell(cell, s, percentFormats);
	}
	
	public static Cell getColumnParentCell(Sheet sheet, Row row, Cell cell)
	throws Exception
	{
		if (row.getRowNum() == 0)
			return null;
		
		if (cell == null)
			return null;
		
		Row prevRow = sheet.getRow(row.getRowNum() - 1);
		if (prevRow == null)
			return null;
		
		Cell parentCell = null;
		int lookupIndex = cell.getColumnIndex();
		while (lookupIndex >= 0) {
			parentCell = prevRow.getCell(lookupIndex);
			
			if (parentCell == null)
				break;
			
			if ((cellIsColumnDefinition(parentCell))  && (! cellIsEmpty(parentCell)))
				return parentCell;
			
			lookupIndex--;
		} 
		
		return null;
	}
	
	public static Cell getRowParentCell(Sheet sheet, Row row, Cell cell)
	throws Exception
	{
		if (row.getRowNum() == 0)
			return null;
		
		if (cell == null)
			return null;

		int prevCol = cell.getColumnIndex() - 1;
		if (prevCol < 0)
			return null;
		
		Cell parentCell = null;
		for (int i = row.getRowNum() - 1; i >= 0; i--) {
			Row searchRow = sheet.getRow(i);
			if (searchRow != null) {
				parentCell = searchRow.getCell(prevCol);
				
				if ((cellIsRowDefinition(parentCell)) && (! cellIsEmpty(parentCell)))
					return parentCell;
			}
		}
		
		return null;
	}
	
	public static Cell getCell(Sheet sheet, int rowNum, int colNum)
	throws Exception 
	{
		Row row = sheet.getRow(rowNum);
		if (row == null)
			return null;
		
		Cell cell = row.getCell(colNum);
		
		return cell;
	}
	
	public static Cell getContainerColumnCell(Sheet sheet, int rowIndex, int colIndex)
	throws Exception
	{
		for (int i = rowIndex; i >= 0; i--) {
			Cell containerCell = getCell(sheet, i, colIndex);
			if ((containerCell != null) && 
				(cellIsColumnDefinition(containerCell)) &&
				(! cellIsEmpty(containerCell)))
				return containerCell;
		}
		
		return null;
	}
	
	public static Cell getContainerRowCell(Sheet sheet, int rowIndex, int colIndex)
	throws Exception
	{
		for (int i = colIndex; i >= 0; i--) {
			Cell containerCell = getCell(sheet, rowIndex, i);
			if ((containerCell != null) && 
				(cellIsRowDefinition(containerCell)) &&
				(! cellIsEmpty(containerCell)))
				return containerCell;
		}
		
		return null;
	}
	
	private static String stripFootnoteReference(Cell cell)
	throws Exception
	{
		if (cellIsEmpty(cell))
			return null;
		
		if (! cellIsStringOrBlank(cell))
			throw new Exception("Cannot operate on a non-string cell.");
		
		String[] tokens = cell.getStringCellValue().split(FOOTNOTE_DIVIDER_REGEX);
		if (tokens.length == 2)
			return tokens[0].trim();
		
		return cell.getStringCellValue().trim();
	}
	
	public static String getCellContents(Cell cell) 
	throws Exception
	{
		if (cellIsEmpty(cell))
			return null;
		
		if (! cellIsStringOrBlank(cell))
			throw new Exception("Cannot operate on a non-string cell.");
		
		String strippedCell = stripFootnoteReference(cell).trim();
		
		String[] tokens = strippedCell.split(CELL_ID_DIVIDER_REGEX);
		if (tokens.length >= 2)
			return tokens[0].trim();
		
		return strippedCell;
	}
	
	public static String getCellId(Cell cell) 
	throws Exception
	{
		if (cellIsEmpty(cell))
			return null;
		
		if (! cellIsStringOrBlank(cell))
			throw new Exception("Cannot operate on a non-string cell.");
		
		String strippedCell = stripFootnoteReference(cell);
		if (strippedCell != null) 
			strippedCell = strippedCell.trim();
		
		String id = String.format("R%dC%d", cell.getRowIndex(), cell.getColumnIndex());
		if (cellIsEmpty(cell))
			return id;
		
		String[] tokens = strippedCell.split(CELL_ID_DIVIDER_REGEX);
		if (tokens.length == 2)
			return tokens[1].trim();
		
		return id;
	}
	
	public static String getCellFootnoteReference(Cell cell) 
	throws Exception
	{
		if (cellIsEmpty(cell))
			return null;
		
		if (! cellIsStringOrBlank(cell))
			throw new Exception("Cannot operate on a non-string cell.");
		
		String[] tokens = cell.getStringCellValue().split(FOOTNOTE_DIVIDER_REGEX);
		if (tokens.length == 2)
			return tokens[1].trim();
		
		return null;
	}
	
	public static String getCellFootnoteDefinitionReference(Cell cell) 
	throws Exception
	{
		if (cellIsEmpty(cell))
			return null;

		if (! cellIsStringOrBlank(cell))
			throw new Exception("Cannot operate on a non-string cell.");
		
		String[] tokens = cell.getStringCellValue().split(FOOTNOTE_DIVIDER_REGEX);
		if (tokens.length == 2)
			return tokens[0].trim();
		
		return null;
	}

	public static String getCellFootnoteDefinitionText(Cell cell) 
	throws Exception
	{
		if (cellIsEmpty(cell))
			return null;

		if (! cellIsStringOrBlank(cell))
			throw new Exception("Cannot operate on a non-string cell.");
		
		String[] tokens = cell.getStringCellValue().split(FOOTNOTE_DIVIDER_REGEX);
		if (tokens.length == 2)
			return tokens[1].trim();
		
		return null;
	}
	
	public static boolean allCellsBlank(Row row, int maxColIndex)
	throws Exception
	{
		if (row == null)
			return true;
		
		for (int i = 0; i <= maxColIndex; i++) {
			Cell cell = row.getCell(i);
			
			if (! cellIsEmpty(cell))
				return false;
		}
		
		return true;
	}
	
	public static String getRowId(Row row) 
	throws Exception
	{
		if (row == null)
			return null;
		
		String id = String.format("R%d", row.getRowNum());
		return id;
	}
	
	public static boolean cellIsStringOrBlank(Cell cell)
	{
		return ((cell.getCellType() == Cell.CELL_TYPE_STRING) ||
				(cell.getCellType() == Cell.CELL_TYPE_BLANK));
	}
	
	public static boolean cellIsEmpty(Cell cell)
	{
		if (cell == null)
			return true;
		
		if (cell.getCellType() == Cell.CELL_TYPE_BLANK)
			return true;
		
		if (cell.getCellType() != Cell.CELL_TYPE_STRING)
			return false;
		
		return (cell.getStringCellValue().length() == 0);
	}
	
	public static void setCellValue(Cell cell, String s)
	{
		if (s == null)
			return;
		
		boolean isPercent = false;
		
		if (s.endsWith("%")) {
			isPercent = true;
			s = s.substring(0, s.length() - 1);
		}
		
		try {
			double d = Double.parseDouble(s);
			
			if (isPercent) {
				ExcelUtils.formatPercentageCell(cell, s);
				cell.setCellValue(d / 100d);
			} else {
				ExcelUtils.formatDecimalCell(cell, s);
				cell.setCellValue(d);
			}
		} catch (Exception e) {
			cell.setCellValue(s);
		}
	}
}
