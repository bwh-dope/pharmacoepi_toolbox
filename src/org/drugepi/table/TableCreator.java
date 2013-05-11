/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

package org.drugepi.table;

import java.io.*;
import java.util.*;

import org.apache.poi.ss.usermodel.*;
import org.drugepi.PharmacoepiTool;
import org.drugepi.table.TableRowCol.RowColTypes;

/**
 * Creation of basic tables for epidemiology.
 * 
 * Tables are added using the {@link #addTable(String, String)} method.  Rows and columns are then defined,
 * and values added to cells.  Tables are then rendered to HTML using {@link #writeHtmlToFile(String, String)}.
 * 
 * @author Jeremy A. Rassen
 * @version 1.0.0
 *
 */
/**
 * @author jeremy
 *
 */
public class TableCreator extends PharmacoepiTool {
	Map<String, Table> tables;
	
	/**
	 * Constructor for TableCreator.
	 */
	public TableCreator()
	{
		super();
		this.tables = new HashMap<String, Table>();
	}
	
	/**
	 * Add a table definition.
	 * 
	 * @param tableId		Unique ID of this table.
	 * @param description	Description of this table, to print as the table header.
	 */
	public void addTable(String tableId, String description)
	{
		tableId = TableElement.makeId(tableId);

		Table t = new Table(tableId, description);
		tables.put(tableId, t);
	}

	/**
	 * Add a header-style row to a table.
	 * 
	 * @param tableId	ID of the table.
	 * @param parentId	ID of the row's parent.
	 * @param id		Unique ID of the row.
	 * @param description	Description of the row, to print as the row's text in the table output.
	 */
	public void addHeaderRowToTable(String tableId, String parentId, String id, String description) {
		this.addRowToTable(tableId, parentId, id, description, RowColTypes.HEADER);
	}
	
	/**
	 * Add a normal-style row to a table.
	 * 
	 * @param tableId	ID of the table.
	 * @param parentId	ID of the row's parent.
	 * @param id		Unique ID of the row.
	 * @param description	Description of the row, to print as the row's text in the table output.
	 */
	public void addRowToTable(String tableId, String parentId, String id, String description) {
		this.addRowToTable(tableId, parentId, id, description, RowColTypes.NORMAL);
	}

	/**
	 * Sets the table description.
	 * 
	 * @param tableId		ID of the table.
	 * @param description	The rows title, to print above the first table row.
	 */
	public void setTableDescription(String tableId, String description) {
		tableId = TableElement.makeId(tableId);
		Table t = this.tables.get(tableId);
		if (t == null) {
			System.out.printf("Table %s not found.\n", tableId);
			return;
		}
		
		t.description = description;
	}
	
	/**
	 * Sets the text that appears above the first row.
	 * 
	 * @param tableId		ID of the table.
	 * @param description	The rows title, to print above the first table row.
	 */
	public void setRowsTitle(String tableId, String description) {
		tableId = TableElement.makeId(tableId);
		Table t = this.tables.get(tableId);
		if (t == null) {
			System.out.printf("Table %s not found.\n", tableId);
			return;
		}
		
		t.rowsTitle = description;
	}
	
	private void addRowToTable(String tableId, String parentId, String id, String description, RowColTypes rcType) {
		tableId = TableElement.makeId(tableId);
		parentId = TableElement.makeId(parentId);
		id = TableElement.makeId(id);
		
		Table t = this.tables.get(tableId);
		if (t == null) {
			System.out.printf("Table %s not found.\n", tableId);
			return;
		}
		
		TableRowCol r = new TableRowCol(id, description, rcType);
		if ((parentId != null) && (! parentId.trim().contentEquals("."))) { 
			TableRowCol parent = t.rowsMap.get(parentId);
			
			if (parent != null) {
				r.parent = parent;
				parent.addChild(r);
			} else 
				System.out.printf("Attempt to add to non-existent parent %s\n", parentId);
		}
		t.addRow(r);
	}
	
	/**
	 * Add a normal-style column to a table.
	 * 
	 * @param tableId	ID of the table.
	 * @param parentId	ID of the column's parent.
	 * @param id		Unique ID of the column.
	 * @param description	Description of the column, to print as the column's text in the table output.
	 */
	public void addColToTable(String tableId, String parentId, String id, String description) {
		this.addColToTable(tableId, parentId, id, description, RowColTypes.NORMAL);
	}

	/**
	 * Add a header-style column to a table.
	 * 
	 * @param tableId	ID of the table.
	 * @param parentId	ID of the column's parent.
	 * @param id		Unique ID of the column.
	 * @param description	Description of the column, to print as the column's text in the table output.
	 */
	public void addHeaderColToTable(String tableId, String parentId, String id, String description) {
		this.addColToTable(tableId, parentId, id, description, RowColTypes.HEADER);
	}
	
	private void addColToTable(String tableId, String parentId, String id, String description, RowColTypes rcType) {
		tableId = TableElement.makeId(tableId);
		parentId = TableElement.makeId(parentId);
		id = TableElement.makeId(id);
		
		Table t = this.tables.get(tableId);
		if (t == null) {
			System.out.printf("Table %s not found.\n", tableId);
			return;
		}
		
		TableRowCol c = new TableRowCol(id, description, rcType);
		if ((parentId != null) && (! parentId.trim().contentEquals("."))) {
			TableRowCol parent = t.colsMap.get(parentId);
			c.parent = parent;
			parent.addChild(c);
		}
		
		t.addCol(c);
	}
	
	/**
	 * Add a cell to the table.
	 * 
	 * @param tableId	ID of the table.
	 * @param rowId		ID of the row.
	 * @param colId		ID of the column.
	 * @param description 	Description of the column, to print as the cell's text in the table output.
	 */
	public void addCellToTable(String tableId, String rowId, String colId, String description) {
		tableId = TableElement.makeId(tableId);
		rowId = TableElement.makeId(rowId);
		colId = TableElement.makeId(colId);

		Table t = this.tables.get(tableId);
		if (t == null) {
			System.out.printf("Table %s not found.\n", tableId);
			return;
		}
		
		TableCell c = new TableCell(rowId, colId, description);
		c.row = t.rowsMap.get(rowId);
		c.col = t.colsMap.get(colId);
		t.addCell(c);
	}
	
	/**
	 * Add a footnote to the table.
	 * 
	 * @param tableId	ID of the table.
	 * @param rowId		ID of the row (leave blank for a footnote on a column label).
	 * @param colId		ID of the column (leave blank for a footnote on a row label).
	 * @param footnoteSymbol 	The footnote symbol.
	 * @param description 	The footnote text.
	 */
	public void addFootnoteToTable(String tableId, String rowId, String colId, String footnoteSymbol, String description) {
		tableId = TableElement.makeId(tableId);
		rowId = TableElement.makeId(rowId);
		colId = TableElement.makeId(colId);

		Table t = this.tables.get(tableId);
		if (t == null) {
			System.out.printf("Table %s not found.\n", tableId);
			return;
		}
		
		TableElement e = null;

		if ((rowId == null) || (rowId.length() == 0))
			e = t.colsMap.get(colId);
		else if ((colId == null) || (colId.length() == 0))
			e = t.rowsMap.get(rowId);
		else 
			e = t.cells.get(TableCell.getCellId(rowId, colId));
		
		if (e == null)
			return;
		
		TableFootnote f = new TableFootnote(footnoteSymbol, description);
		e.addFootnote(f);
	}
	
	/**
	 * Get the number of cells in a table.
	 * 
	 * @param tableId	ID of the table.	
	 * @return			Number of cells in the table.
	 */
	public int getNumCells(String tableId)
	{
		tableId = TableElement.makeId(tableId);

		Table t = this.tables.get(tableId);
		if (t==null) 
			System.out.printf("Table %s not found.\n", tableId);
		
		return t.cells.size();
	}
	
	private String tableToHtml(String tableId)
	throws Exception
	{
		tableId = TableElement.makeId(tableId);

		Table t = this.tables.get(tableId);
		if (t==null) 
			System.out.printf("Table %s not found.\n", tableId);
		return t.toHtml();
	}

	/**
	 * Render a table as HTML and then write the HTML to a file.
	 * 
	 * @param tableId		ID of the table.
	 * @param outputDirectory	Directory where the file should be placed.  The file will be named
	 * 							with the table's ID.
	 * @throws Exception
	 */
	public void writeHtmlToFile(String tableId, String outputDirectory)
	throws Exception
	{
		tableId = TableElement.makeId(tableId);

		String html = this.tableToHtml(tableId);
		
		String outputPath = (new File(outputDirectory)).getAbsolutePath() + 
							File.separator +
							tableId +
							".html";
	
	    BufferedWriter out = new BufferedWriter(new FileWriter(outputPath));
	    out.write(html);
	    out.close();
	}
	
	/**
	 * Render all tables as HTML and then write the HTML to files.
	 * 
	 * @param tableId		ID of the table.
	 * @param outputDirectory	Directory where the file should be placed.  The file will be named
	 * 							with the table's ID.
	 * @throws Exception
	 */
	public void writeAllHtmlToFile(String outputDirectory)
	throws Exception
	{
		for (Table table: tables.values()) {
			this.writeHtmlToFile(table.id, outputDirectory);
		}
	}
	
	
	private void createTableFromSheet(Sheet sheet)
	throws Exception
	{
		String tableId = sheet.getSheetName();
		String description = sheet.getSheetName();
		
		Row firstRow = sheet.getRow(0);
		
		if (firstRow == null) {
			System.out.println("Sheet is empty.");
			return;
		}

		this.addTable(tableId, description);
		System.out.printf("Table Creator added table: %s\n", tableId);
		
		LinkedHashMap<String, String> footnoteDefs = new LinkedHashMap<String, String>();
		LinkedHashMap<String, Cell> footnoteLinks = new LinkedHashMap<String, Cell>();
		
		for (Row row: sheet) {
			if (row != null) {
				boolean rowIsEmpty = true;
				
				for (Cell cell : row) {
					if (! ExcelUtils.cellIsEmpty(cell)) {
						rowIsEmpty = false;
						
						if (ExcelUtils.cellIsTableDescription(cell)) {
							this.setTableDescription(tableId, cell.getStringCellValue());
						}

						if (ExcelUtils.cellIsRowHeader(cell)) {
							this.setRowsTitle(tableId, cell.getStringCellValue());
						}
					
						if (ExcelUtils.cellIsColumnDefinition(cell)) {
							Cell parentCell = ExcelUtils.getColumnParentCell(sheet, row, cell);
							RowColTypes rcType = (ExcelUtils.cellIsBold(cell) ? 
									RowColTypes.HEADER : RowColTypes.NORMAL);
							
							this.addColToTable(tableId, 
									ExcelUtils.getCellId(parentCell), 
									ExcelUtils.getCellId(cell), 
									ExcelUtils.getCellContents(cell),
									rcType);
							
//							System.out.printf("Added %s column %s, ID = %s, parent = %s\n",
//									(rcType == RowColTypes.HEADER ? "header" : "normal"),
//									ExcelUtils.getCellContents(cell),
//									ExcelUtils.getCellId(cell), 
//									ExcelUtils.getCellId(ExcelUtils.getColumnParentCell(sheet, row, parentCell)));
							
							String footnoteRef = ExcelUtils.getCellFootnoteReference(cell);
							if (footnoteRef != null)
								footnoteLinks.put(footnoteRef, cell);
						}
						
						if (ExcelUtils.cellIsRowDefinition(cell)) {
							Cell parentCell = 
								ExcelUtils.getRowParentCell(sheet, row, cell);

							RowColTypes rcType = (ExcelUtils.cellIsBold(cell) ? 
									RowColTypes.HEADER : RowColTypes.NORMAL);

							this.addRowToTable(tableId, 
									ExcelUtils.getCellId(parentCell),
									ExcelUtils.getCellId(cell), 
									ExcelUtils.getCellContents(cell),
									rcType);
							
//							System.out.printf("Added %s row %s, ID = %s, parent = %s\n",
//									(rcType == RowColTypes.HEADER ? "header" : "normal"),
//									ExcelUtils.getCellContents(cell),
//									ExcelUtils.getCellId(cell), 
//									ExcelUtils.getCellId(parentCell));

							String footnoteRef = ExcelUtils.getCellFootnoteReference(cell);
							if (footnoteRef != null)
								footnoteLinks.put(footnoteRef, cell);
						}
						
						if (ExcelUtils.cellIsFootnoteDefinition(cell)) {
							String footnoteRef = ExcelUtils.getCellFootnoteDefinitionReference(cell);
							String footnoteText = ExcelUtils.getCellFootnoteDefinitionText(cell);
							
							if ((footnoteRef != null) && (footnoteText != null)) {
								footnoteDefs.put(footnoteRef, footnoteText);
								
								System.out.printf("Noted footnote definition %s : %s\n", footnoteRef, footnoteText);
							}
						}

					}
				}
				
				if (rowIsEmpty) {
					this.addRowToTable(tableId, 
							null,
							ExcelUtils.getRowId(row),
							"");
//					System.out.println("Added blank row");
				}
			}
		}
		
		if (footnoteDefs.size() > 0) {
			for (String footnoteRef: footnoteDefs.keySet()) {
				Cell referredCell = footnoteLinks.get(footnoteRef);
				if (referredCell != null) {
					
					String rowId = null;
					String colId = null;
					String id = ExcelUtils.getCellId(referredCell);;
					if (ExcelUtils.cellIsRowDefinition(referredCell))
						rowId = id;
					else if (ExcelUtils.cellIsColumnDefinition(referredCell))
						colId = id;
					else
						break;
					
					this.addFootnoteToTable(tableId, 
							rowId, 
							colId, 
							footnoteRef, 
							footnoteDefs.get(footnoteRef));
					
//					System.out.printf("Added footnote %s to R[%s], C[%s] -- %s\n", 
//							footnoteRef, rowId, colId, footnoteDefs.get(footnoteRef));
				}
			}
		}
	}
	
	
	private void fillTableInSheet(Sheet sheet, Table t)
	throws Exception
	{
		Row firstRow = sheet.getRow(0);
		
		if (firstRow == null) {
			System.out.println("Sheet is empty.");
			return;
		}

		// Find four cells:
		Cell columnDefBottomLeft = null;
		Cell columnDefBottomRight = null;
		Cell rowDefTopRight = null;
		Cell rowDefBottomRight = null;

		for (Row row: sheet) {
			if (row != null) {
				for (Cell cell : row) {
					// We are iterating top to bottom, left to right
					if (ExcelUtils.cellIsColumnDefinition(cell)) {
						// get the last cell that is on the left side
						if ((columnDefBottomLeft == null) || 
							(cell.getColumnIndex() <= columnDefBottomLeft.getColumnIndex()))
							columnDefBottomLeft = cell;
						
						// get the last cell that is on the right side
						if ((columnDefBottomRight == null) || 
							(cell.getColumnIndex() >= columnDefBottomRight.getColumnIndex()))
							columnDefBottomRight = cell;
						
					}
					
					if (ExcelUtils.cellIsRowDefinition(cell)) {
						// get the first cell that is on the right side
						if ((rowDefTopRight == null) || 
							(cell.getColumnIndex() > rowDefTopRight.getColumnIndex()))
							rowDefTopRight = cell;
						
						// get the last cell that is on the right side
						if ((rowDefBottomRight == null) || 
							(cell.getColumnIndex() >= rowDefBottomRight.getColumnIndex()))
							rowDefBottomRight = cell;
					}
				}
			}
		}
		
		if ((columnDefBottomLeft == null) ||
			(columnDefBottomRight == null) ||
			(rowDefTopRight == null) ||
			(rowDefBottomRight == null))
			return;

		int rowFillStart = rowDefTopRight.getRowIndex();
		int rowFillEnd = rowDefBottomRight.getRowIndex();
		int colFillStart = columnDefBottomLeft.getColumnIndex();
		int colFillEnd = columnDefBottomRight.getColumnIndex();
		
		for (int rowIndex = rowFillStart; rowIndex <= rowFillEnd; rowIndex++) {
			Row row = sheet.getRow(rowIndex);
			for (int colIndex = colFillStart; colIndex <= colFillEnd; colIndex++) {
				Cell columnParent = ExcelUtils.getContainerColumnCell(sheet, rowIndex, colIndex);
				Cell rowParent = ExcelUtils.getContainerRowCell(sheet, rowIndex, colIndex);
				
				if ((columnParent != null) && (rowParent != null)) {
					Cell cell = row.getCell(colIndex);
					if (cell == null) 
						cell = row.createCell(colIndex);
					
					String colId = TableElement.makeId(ExcelUtils.getCellId(columnParent));
					String rowId = TableElement.makeId(ExcelUtils.getCellId(rowParent));
					String cellId = TableCell.getCellId(rowId, colId);

					TableCell c = t.cells.get(cellId);
					if ((c != null) && (c.description.length() > 0)) 
						ExcelUtils.setCellValue(cell, c.description);
				}
			}
		}
		
		CellStyleLookup csl = new CellStyleLookup();
		for (Row row: sheet) {
			if (row != null) {
				for (Cell cell : row) {
					if (ExcelUtils.cellIsStringOrBlank(cell)) {
						String id = ExcelUtils.getCellId(cell);
						if (id != null)
							cell.setCellValue(ExcelUtils.getCellContents(cell));
					}
					
					ExcelUtils.restyleCell(csl, cell);
				}
			}
		}
	}
	
	/**
	 * Generate table formats by reading an Excel file.  Each sheet in the Excel file will 
	 * serve as a table template.
	 * 
	 * @param workbookName  Name of the Excel workbook (.xls or .xlsx) with the defined tables.
	 * @throws Exception
	 */
	public void createTablesFromWorkbook(String workbookName) 
	throws Exception {
		InputStream s = new FileInputStream(workbookName);

		Workbook workbook = WorkbookFactory.create(s);
		for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
			Sheet sheet = workbook.getSheetAt(i);
			this.createTableFromSheet(sheet);
		}
	}
	
	/**
	 * Replace the shell table in an Excel file with data from tables.
	 * 
	 * @param inWorkbookName  Name of the Excel workbook (.xls or .xlsx) with the defined tables.
	 * @param outWorkbookName  Name of the Excel workbook (.xls or .xlsx) to use for output.  Any existing file will be replaced.
	 * @throws Exception
	 */
	public void writeTablesToWorkbook(String inWorkbookName, String outWorkbookName) 
	throws Exception {
		InputStream fileIn = new FileInputStream(inWorkbookName);

		Workbook workbook = WorkbookFactory.create(fileIn);
		for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
			Sheet sheet = workbook.getSheetAt(i);
			String tableId = TableElement.makeId(sheet.getSheetName());
			Table t = this.tables.get(tableId);
			if (t != null)
				this.fillTableInSheet(sheet, t);
		}
		
		FileOutputStream fileOut = new FileOutputStream(outWorkbookName);
		workbook.write(fileOut);
		fileOut.close();
	}
	
	public String toString() {
		StringBuffer output = new StringBuffer();
		
		for (Table table: this.tables.values()) {
			output.append(table.toString());
		}
		
		return output.toString();
	}
}
