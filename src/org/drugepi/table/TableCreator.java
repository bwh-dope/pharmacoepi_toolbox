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
	
	 public static void main(String[] args) {
		 try {
			 System.out.println("Starting");
			 TableCreator tm = new TableCreator();
			 
			 tm.createTablesFromWorkbook("/Users/jeremy/Dropbox/JAR73/Projects/plasmode/Plasmode Tables.xlsx");
//			 tm.createTablesFromWorkbook("/Users/jeremy/Dropbox/JAR73/Projects/matching/Three Way Matching Tables.xls");
			 tm.addCellToTable("Table E1", "ace_rx", "FULL1", "0.264465");
			 tm.addCellToTable("Table E1", "ace_rx", "FULL2", "0.272683");
			 tm.addCellToTable("Table E1", "ace_rx", "FULL3", "0.294818");
			 tm.addCellToTable("Table E1", "ace_rx", "MERGED1", "0.266914");
			 tm.addCellToTable("Table E1", "ace_rx", "MERGED2", "0.270406");
			 tm.addCellToTable("Table E1", "ace_rx", "MERGED3", "0.271715");
			 tm.addCellToTable("Table E1", "ace_rx", "PAIRWISE211", "0.265698");
			 tm.addCellToTable("Table E1", "ace_rx", "PAIRWISE212", "0.269757");
			 tm.addCellToTable("Table E1", "ace_rx", "PAIRWISE311", "0.265599");
			 tm.addCellToTable("Table E1", "ace_rx", "PAIRWISE313", "0.270025");
			 tm.addCellToTable("Table E1", "ace_rx", "PAIRWISE322", "0.273919");
			 tm.addCellToTable("Table E1", "ace_rx", "PAIRWISE323", "0.268185");
			 tm.addCellToTable("Table E1", "ace_rx", "THREE_WAY1", "0.265978");
			 tm.addCellToTable("Table E1", "ace_rx", "THREE_WAY2", "0.274586");
			 tm.addCellToTable("Table E1", "ace_rx", "THREE_WAY3", "0.261889");
			 tm.addCellToTable("Table E1", "age", "FULL1", "79.669060");
			 tm.addCellToTable("Table E1", "age", "FULL2", "80.872489");
			 tm.addCellToTable("Table E1", "age", "FULL3", "81.150702");
			 tm.addCellToTable("Table E1", "age", "MERGED1", "80.017460");
			 tm.addCellToTable("Table E1", "age", "MERGED2", "79.966390");
			 tm.addCellToTable("Table E1", "age", "MERGED3", "79.775426");
			 tm.addCellToTable("Table E1", "age", "PAIRWISE211", "79.913926");
			 tm.addCellToTable("Table E1", "age", "PAIRWISE212", "79.847928");
			 tm.addCellToTable("Table E1", "age", "PAIRWISE311", "79.819351");
			 tm.addCellToTable("Table E1", "age", "PAIRWISE313", "79.686973");
			 tm.addCellToTable("Table E1", "age", "PAIRWISE322", "80.892529");
			 tm.addCellToTable("Table E1", "age", "PAIRWISE323", "80.810452");
			 tm.addCellToTable("Table E1", "age", "THREE_WAY1", "79.938455");
			 tm.addCellToTable("Table E1", "age", "THREE_WAY2", "79.967721");
			 tm.addCellToTable("Table E1", "age", "THREE_WAY3", "79.967076");
			 tm.addCellToTable("Table E1", "alzheimer", "FULL1", "0.096840");
			 tm.addCellToTable("Table E1", "alzheimer", "FULL2", "0.109689");
			 tm.addCellToTable("Table E1", "alzheimer", "FULL3", "0.112451");
			 tm.addCellToTable("Table E1", "alzheimer", "MERGED1", "0.100393");
			 tm.addCellToTable("Table E1", "alzheimer", "MERGED2", "0.100611");
			 tm.addCellToTable("Table E1", "alzheimer", "MERGED3", "0.098647");
			 tm.addCellToTable("Table E1", "alzheimer", "PAIRWISE211", "0.099317");
			 tm.addCellToTable("Table E1", "alzheimer", "PAIRWISE212", "0.100171");
			 tm.addCellToTable("Table E1", "alzheimer", "PAIRWISE311", "0.098229");
			 tm.addCellToTable("Table E1", "alzheimer", "PAIRWISE313", "0.098440");
			 tm.addCellToTable("Table E1", "alzheimer", "PAIRWISE322", "0.108126");
			 tm.addCellToTable("Table E1", "alzheimer", "PAIRWISE323", "0.109764");
			 tm.addCellToTable("Table E1", "alzheimer", "THREE_WAY1", "0.099849");
			 tm.addCellToTable("Table E1", "alzheimer", "THREE_WAY2", "0.103508");
			 tm.addCellToTable("Table E1", "alzheimer", "THREE_WAY3", "0.096621");
			 tm.addCellToTable("Table E1", "angina_dx", "FULL1", "0.063398");
			 tm.addCellToTable("Table E1", "angina_dx", "FULL2", "0.084413");
			 tm.addCellToTable("Table E1", "angina_dx", "FULL3", "0.091977");
			 tm.addCellToTable("Table E1", "angina_dx", "MERGED1", "0.066128");
			 tm.addCellToTable("Table E1", "angina_dx", "MERGED2", "0.069838");
			 tm.addCellToTable("Table E1", "angina_dx", "MERGED3", "0.065692");
			 tm.addCellToTable("Table E1", "angina_dx", "PAIRWISE211", "0.065570");
			 tm.addCellToTable("Table E1", "angina_dx", "PAIRWISE212", "0.068560");
			 tm.addCellToTable("Table E1", "angina_dx", "PAIRWISE311", "0.064292");
			 tm.addCellToTable("Table E1", "angina_dx", "PAIRWISE313", "0.064924");
			 tm.addCellToTable("Table E1", "angina_dx", "PAIRWISE322", "0.084043");
			 tm.addCellToTable("Table E1", "angina_dx", "PAIRWISE323", "0.084371");
			 tm.addCellToTable("Table E1", "angina_dx", "THREE_WAY1", "0.064773");
			 tm.addCellToTable("Table E1", "angina_dx", "THREE_WAY2", "0.065203");
			 tm.addCellToTable("Table E1", "angina_dx", "THREE_WAY3", "0.062621");
			 tm.addCellToTable("Table E1", "antithromb_rx", "FULL1", "0.144235");
			 tm.addCellToTable("Table E1", "antithromb_rx", "FULL2", "0.176280");
			 tm.addCellToTable("Table E1", "antithromb_rx", "FULL3", "0.276883");
			 tm.addCellToTable("Table E1", "antithromb_rx", "MERGED1", "0.151462");
			 tm.addCellToTable("Table E1", "antithromb_rx", "MERGED2", "0.151026");
			 tm.addCellToTable("Table E1", "antithromb_rx", "MERGED3", "0.159756");
			 tm.addCellToTable("Table E1", "antithromb_rx", "PAIRWISE211", "0.148227");
			 tm.addCellToTable("Table E1", "antithromb_rx", "PAIRWISE212", "0.149509");
			 tm.addCellToTable("Table E1", "antithromb_rx", "PAIRWISE311", "0.148187");
			 tm.addCellToTable("Table E1", "antithromb_rx", "PAIRWISE313", "0.154933");
			 tm.addCellToTable("Table E1", "antithromb_rx", "PAIRWISE322", "0.178244");
			 tm.addCellToTable("Table E1", "antithromb_rx", "PAIRWISE323", "0.175786");
			 tm.addCellToTable("Table E1", "antithromb_rx", "THREE_WAY1", "0.150204");
			 tm.addCellToTable("Table E1", "antithromb_rx", "THREE_WAY2", "0.159888");
			 tm.addCellToTable("Table E1", "antithromb_rx", "THREE_WAY3", "0.149128");
			 tm.addCellToTable("Table E1", "arb_rx", "FULL1", "0.132130");
			 tm.addCellToTable("Table E1", "arb_rx", "FULL2", "0.126053");
			 tm.addCellToTable("Table E1", "arb_rx", "FULL3", "0.142290");
			 tm.addCellToTable("Table E1", "arb_rx", "MERGED1", "0.129419");
			 tm.addCellToTable("Table E1", "arb_rx", "MERGED2", "0.129419");
			 tm.addCellToTable("Table E1", "arb_rx", "MERGED3", "0.129638");
			 tm.addCellToTable("Table E1", "arb_rx", "PAIRWISE211", "0.130073");
			 tm.addCellToTable("Table E1", "arb_rx", "PAIRWISE212", "0.131995");
			 tm.addCellToTable("Table E1", "arb_rx", "PAIRWISE311", "0.131745");
			 tm.addCellToTable("Table E1", "arb_rx", "PAIRWISE313", "0.129637");
			 tm.addCellToTable("Table E1", "arb_rx", "PAIRWISE322", "0.125819");
			 tm.addCellToTable("Table E1", "arb_rx", "PAIRWISE323", "0.130898");
			 tm.addCellToTable("Table E1", "arb_rx", "THREE_WAY1", "0.131052");
			 tm.addCellToTable("Table E1", "arb_rx", "THREE_WAY2", "0.130192");
			 tm.addCellToTable("Table E1", "arb_rx", "THREE_WAY3", "0.126748");
			 tm.addCellToTable("Table E1", "backpain_dx", "FULL1", "0.285802");
			 tm.addCellToTable("Table E1", "backpain_dx", "FULL2", "0.302171");
			 tm.addCellToTable("Table E1", "backpain_dx", "FULL3", "0.329656");
			 tm.addCellToTable("Table E1", "backpain_dx", "MERGED1", "0.290485");
			 tm.addCellToTable("Table E1", "backpain_dx", "MERGED2", "0.292449");
			 tm.addCellToTable("Table E1", "backpain_dx", "MERGED3", "0.292667");
			 tm.addCellToTable("Table E1", "backpain_dx", "PAIRWISE211", "0.287484");
			 tm.addCellToTable("Table E1", "backpain_dx", "PAIRWISE212", "0.291328");
			 tm.addCellToTable("Table E1", "backpain_dx", "PAIRWISE311", "0.290051");
			 tm.addCellToTable("Table E1", "backpain_dx", "PAIRWISE313", "0.290261");
			 tm.addCellToTable("Table E1", "backpain_dx", "PAIRWISE322", "0.303735");
			 tm.addCellToTable("Table E1", "backpain_dx", "PAIRWISE323", "0.300786");
			 tm.addCellToTable("Table E1", "backpain_dx", "THREE_WAY1", "0.290725");
			 tm.addCellToTable("Table E1", "backpain_dx", "THREE_WAY2", "0.291371");
			 tm.addCellToTable("Table E1", "backpain_dx", "THREE_WAY3", "0.295459");
			 tm.addCellToTable("Table E1", "bblock_rx", "FULL1", "0.374025");
			 tm.addCellToTable("Table E1", "bblock_rx", "FULL2", "0.374595");
			 tm.addCellToTable("Table E1", "bblock_rx", "FULL3", "0.420046");
			 tm.addCellToTable("Table E1", "bblock_rx", "MERGED1", "0.377346");
			 tm.addCellToTable("Table E1", "bblock_rx", "MERGED2", "0.372763");
			 tm.addCellToTable("Table E1", "bblock_rx", "MERGED3", "0.371017");
			 tm.addCellToTable("Table E1", "bblock_rx", "PAIRWISE211", "0.375694");
			 tm.addCellToTable("Table E1", "bblock_rx", "PAIRWISE212", "0.373558");
			 tm.addCellToTable("Table E1", "bblock_rx", "PAIRWISE311", "0.376265");
			 tm.addCellToTable("Table E1", "bblock_rx", "PAIRWISE313", "0.371206");
			 tm.addCellToTable("Table E1", "bblock_rx", "PAIRWISE322", "0.376966");
			 tm.addCellToTable("Table E1", "bblock_rx", "PAIRWISE323", "0.375000");
			 tm.addCellToTable("Table E1", "bblock_rx", "THREE_WAY1", "0.377017");
			 tm.addCellToTable("Table E1", "bblock_rx", "THREE_WAY2", "0.376587");
			 tm.addCellToTable("Table E1", "bblock_rx", "THREE_WAY3", "0.370346");
			 tm.addCellToTable("Table E1", "benzo_rx", "FULL1", "0.205786");
			 tm.addCellToTable("Table E1", "benzo_rx", "FULL2", "0.217920");
			 tm.addCellToTable("Table E1", "benzo_rx", "FULL3", "0.245060");
			 tm.addCellToTable("Table E1", "benzo_rx", "MERGED1", "0.211043");
			 tm.addCellToTable("Table E1", "benzo_rx", "MERGED2", "0.216936");
			 tm.addCellToTable("Table E1", "benzo_rx", "MERGED3", "0.207770");
			 tm.addCellToTable("Table E1", "benzo_rx", "PAIRWISE211", "0.208672");
			 tm.addCellToTable("Table E1", "benzo_rx", "PAIRWISE212", "0.215293");
			 tm.addCellToTable("Table E1", "benzo_rx", "PAIRWISE311", "0.208474");
			 tm.addCellToTable("Table E1", "benzo_rx", "PAIRWISE313", "0.204680");
			 tm.addCellToTable("Table E1", "benzo_rx", "PAIRWISE322", "0.219692");
			 tm.addCellToTable("Table E1", "benzo_rx", "PAIRWISE323", "0.220511");
			 tm.addCellToTable("Table E1", "benzo_rx", "THREE_WAY1", "0.209167");
			 tm.addCellToTable("Table E1", "benzo_rx", "THREE_WAY2", "0.209813");
			 tm.addCellToTable("Table E1", "benzo_rx", "THREE_WAY3", "0.206800");
			 tm.addCellToTable("Table E1", "bmd_test", "FULL1", "0.102380");
			 tm.addCellToTable("Table E1", "bmd_test", "FULL2", "0.083117");
			 tm.addCellToTable("Table E1", "bmd_test", "FULL3", "0.085231");
			 tm.addCellToTable("Table E1", "bmd_test", "MERGED1", "0.093191");
			 tm.addCellToTable("Table E1", "bmd_test", "MERGED2", "0.095373");
			 tm.addCellToTable("Table E1", "bmd_test", "MERGED3", "0.098865");
			 tm.addCellToTable("Table E1", "bmd_test", "PAIRWISE211", "0.094618");
			 tm.addCellToTable("Table E1", "bmd_test", "PAIRWISE212", "0.097394");
			 tm.addCellToTable("Table E1", "bmd_test", "PAIRWISE311", "0.100126");
			 tm.addCellToTable("Table E1", "bmd_test", "PAIRWISE313", "0.099073");
			 tm.addCellToTable("Table E1", "bmd_test", "PAIRWISE322", "0.083060");
			 tm.addCellToTable("Table E1", "bmd_test", "PAIRWISE323", "0.085026");
			 tm.addCellToTable("Table E1", "bmd_test", "THREE_WAY1", "0.094254");
			 tm.addCellToTable("Table E1", "bmd_test", "THREE_WAY2", "0.098773");
			 tm.addCellToTable("Table E1", "bmd_test", "THREE_WAY3", "0.093178");
			 tm.addCellToTable("Table E1", "coron_proc", "FULL1", "0.010464");
			 tm.addCellToTable("Table E1", "coron_proc", "FULL2", "0.010207");
			 tm.addCellToTable("Table E1", "coron_proc", "FULL3", "0.024204");
			 tm.addCellToTable("Table E1", "coron_proc", "MERGED1", "0.010912");
			 tm.addCellToTable("Table E1", "coron_proc", "MERGED2", "0.010039");
			 tm.addCellToTable("Table E1", "coron_proc", "MERGED3", "0.010258");
			 tm.addCellToTable("Table E1", "coron_proc", "PAIRWISE211", "0.010679");
			 tm.addCellToTable("Table E1", "coron_proc", "PAIRWISE212", "0.010252");
			 tm.addCellToTable("Table E1", "coron_proc", "PAIRWISE311", "0.010750");
			 tm.addCellToTable("Table E1", "coron_proc", "PAIRWISE313", "0.009907");
			 tm.addCellToTable("Table E1", "coron_proc", "PAIRWISE322", "0.010321");
			 tm.addCellToTable("Table E1", "coron_proc", "PAIRWISE323", "0.011468");
			 tm.addCellToTable("Table E1", "coron_proc", "THREE_WAY1", "0.010760");
			 tm.addCellToTable("Table E1", "coron_proc", "THREE_WAY2", "0.010544");
			 tm.addCellToTable("Table E1", "coron_proc", "THREE_WAY3", "0.012266");
			 tm.addCellToTable("Table E1", "cortic_rx", "FULL1", "0.077760");
			 tm.addCellToTable("Table E1", "cortic_rx", "FULL2", "0.089274");
			 tm.addCellToTable("Table E1", "cortic_rx", "FULL3", "0.112769");
			 tm.addCellToTable("Table E1", "cortic_rx", "MERGED1", "0.079660");
			 tm.addCellToTable("Table E1", "cortic_rx", "MERGED2", "0.080314");
			 tm.addCellToTable("Table E1", "cortic_rx", "MERGED3", "0.077914");
			 tm.addCellToTable("Table E1", "cortic_rx", "PAIRWISE211", "0.079240");
			 tm.addCellToTable("Table E1", "cortic_rx", "PAIRWISE212", "0.078812");
			 tm.addCellToTable("Table E1", "cortic_rx", "PAIRWISE311", "0.078415");
			 tm.addCellToTable("Table E1", "cortic_rx", "PAIRWISE313", "0.075885");
			 tm.addCellToTable("Table E1", "cortic_rx", "PAIRWISE322", "0.089941");
			 tm.addCellToTable("Table E1", "cortic_rx", "PAIRWISE323", "0.091252");
			 tm.addCellToTable("Table E1", "cortic_rx", "THREE_WAY1", "0.079406");
			 tm.addCellToTable("Table E1", "cortic_rx", "THREE_WAY2", "0.080052");
			 tm.addCellToTable("Table E1", "cortic_rx", "THREE_WAY3", "0.078545");
			 tm.addCellToTable("Table E1", "diabetes", "FULL1", "0.330735");
			 tm.addCellToTable("Table E1", "diabetes", "FULL2", "0.324530");
			 tm.addCellToTable("Table E1", "diabetes", "FULL3", "0.356876");
			 tm.addCellToTable("Table E1", "diabetes", "MERGED1", "0.327368");
			 tm.addCellToTable("Table E1", "diabetes", "MERGED2", "0.332606");
			 tm.addCellToTable("Table E1", "diabetes", "MERGED3", "0.338717");
			 tm.addCellToTable("Table E1", "diabetes", "PAIRWISE211", "0.328492");
			 tm.addCellToTable("Table E1", "diabetes", "PAIRWISE212", "0.336608");
			 tm.addCellToTable("Table E1", "diabetes", "PAIRWISE311", "0.329469");
			 tm.addCellToTable("Table E1", "diabetes", "PAIRWISE313", "0.338111");
			 tm.addCellToTable("Table E1", "diabetes", "PAIRWISE322", "0.324377");
			 tm.addCellToTable("Table E1", "diabetes", "PAIRWISE323", "0.321429");
			 tm.addCellToTable("Table E1", "diabetes", "THREE_WAY1", "0.330536");
			 tm.addCellToTable("Table E1", "diabetes", "THREE_WAY2", "0.331181");
			 tm.addCellToTable("Table E1", "diabetes", "THREE_WAY3", "0.329460");
			 tm.addCellToTable("Table E1", "epilet_rx", "FULL1", "0.053755");
			 tm.addCellToTable("Table E1", "epilet_rx", "FULL2", "0.050875");
			 tm.addCellToTable("Table E1", "epilet_rx", "FULL3", "0.066899");
			 tm.addCellToTable("Table E1", "epilet_rx", "MERGED1", "0.054125");
			 tm.addCellToTable("Table E1", "epilet_rx", "MERGED2", "0.054343");
			 tm.addCellToTable("Table E1", "epilet_rx", "MERGED3", "0.056307");
			 tm.addCellToTable("Table E1", "epilet_rx", "PAIRWISE211", "0.053610");
			 tm.addCellToTable("Table E1", "epilet_rx", "PAIRWISE212", "0.054891");
			 tm.addCellToTable("Table E1", "epilet_rx", "PAIRWISE311", "0.054595");
			 tm.addCellToTable("Table E1", "epilet_rx", "PAIRWISE313", "0.055649");
			 tm.addCellToTable("Table E1", "epilet_rx", "PAIRWISE322", "0.051278");
			 tm.addCellToTable("Table E1", "epilet_rx", "PAIRWISE323", "0.047182");
			 tm.addCellToTable("Table E1", "epilet_rx", "THREE_WAY1", "0.053798");
			 tm.addCellToTable("Table E1", "epilet_rx", "THREE_WAY2", "0.049925");
			 tm.addCellToTable("Table E1", "epilet_rx", "THREE_WAY3", "0.050355");
			 tm.addCellToTable("Table E1", "falls_dx", "FULL1", "0.022569");
			 tm.addCellToTable("Table E1", "falls_dx", "FULL2", "0.023331");
			 tm.addCellToTable("Table E1", "falls_dx", "FULL3", "0.047774");
			 tm.addCellToTable("Table E1", "falls_dx", "MERGED1", "0.023352");
			 tm.addCellToTable("Table E1", "falls_dx", "MERGED2", "0.023352");
			 tm.addCellToTable("Table E1", "falls_dx", "MERGED3", "0.023789");
			 tm.addCellToTable("Table E1", "falls_dx", "PAIRWISE211", "0.022853");
			 tm.addCellToTable("Table E1", "falls_dx", "PAIRWISE212", "0.022853");
			 tm.addCellToTable("Table E1", "falls_dx", "PAIRWISE311", "0.023187");
			 tm.addCellToTable("Table E1", "falls_dx", "PAIRWISE313", "0.022976");
			 tm.addCellToTable("Table E1", "falls_dx", "PAIRWISE322", "0.023591");
			 tm.addCellToTable("Table E1", "falls_dx", "PAIRWISE323", "0.025393");
			 tm.addCellToTable("Table E1", "falls_dx", "THREE_WAY1", "0.023241");
			 tm.addCellToTable("Table E1", "falls_dx", "THREE_WAY2", "0.023241");
			 tm.addCellToTable("Table E1", "falls_dx", "THREE_WAY3", "0.023456");
			 tm.addCellToTable("Table E1", "fracture_dx", "FULL1", "0.065039");
			 tm.addCellToTable("Table E1", "fracture_dx", "FULL2", "0.071938");
			 tm.addCellToTable("Table E1", "fracture_dx", "FULL3", "0.137211");
			 tm.addCellToTable("Table E1", "fracture_dx", "MERGED1", "0.067656");
			 tm.addCellToTable("Table E1", "fracture_dx", "MERGED2", "0.069184");
			 tm.addCellToTable("Table E1", "fracture_dx", "MERGED3", "0.071803");
			 tm.addCellToTable("Table E1", "fracture_dx", "PAIRWISE211", "0.066211");
			 tm.addCellToTable("Table E1", "fracture_dx", "PAIRWISE212", "0.068133");
			 tm.addCellToTable("Table E1", "fracture_dx", "PAIRWISE311", "0.066821");
			 tm.addCellToTable("Table E1", "fracture_dx", "PAIRWISE313", "0.069562");
			 tm.addCellToTable("Table E1", "fracture_dx", "PAIRWISE322", "0.072739");
			 tm.addCellToTable("Table E1", "fracture_dx", "PAIRWISE323", "0.076835");
			 tm.addCellToTable("Table E1", "fracture_dx", "THREE_WAY1", "0.067355");
			 tm.addCellToTable("Table E1", "fracture_dx", "THREE_WAY2", "0.065419");
			 tm.addCellToTable("Table E1", "fracture_dx", "THREE_WAY3", "0.068001");
			 tm.addCellToTable("Table E1", "generics", "FULL1", "8.278006");
			 tm.addCellToTable("Table E1", "generics", "FULL2", "8.546176");
			 tm.addCellToTable("Table E1", "generics", "FULL3", "9.763590");
			 tm.addCellToTable("Table E1", "generics", "MERGED1", "8.338498");
			 tm.addCellToTable("Table E1", "generics", "MERGED2", "8.278918");
			 tm.addCellToTable("Table E1", "generics", "MERGED3", "8.408992");
			 tm.addCellToTable("Table E1", "generics", "PAIRWISE211", "8.297095");
			 tm.addCellToTable("Table E1", "generics", "PAIRWISE212", "8.280863");
			 tm.addCellToTable("Table E1", "generics", "PAIRWISE311", "8.328415");
			 tm.addCellToTable("Table E1", "generics", "PAIRWISE313", "8.347175");
			 tm.addCellToTable("Table E1", "generics", "PAIRWISE322", "8.581258");
			 tm.addCellToTable("Table E1", "generics", "PAIRWISE323", "8.567333");
			 tm.addCellToTable("Table E1", "generics", "THREE_WAY1", "8.328384");
			 tm.addCellToTable("Table E1", "generics", "THREE_WAY2", "8.329245");
			 tm.addCellToTable("Table E1", "generics", "THREE_WAY3", "8.276953");
			 tm.addCellToTable("Table E1", "gout", "FULL1", "0.065449");
			 tm.addCellToTable("Table E1", "gout", "FULL2", "0.045852");
			 tm.addCellToTable("Table E1", "gout", "FULL3", "0.052139");
			 tm.addCellToTable("Table E1", "gout", "MERGED1", "0.054561");
			 tm.addCellToTable("Table E1", "gout", "MERGED2", "0.049760");
			 tm.addCellToTable("Table E1", "gout", "MERGED3", "0.062636");
			 tm.addCellToTable("Table E1", "gout", "PAIRWISE211", "0.056813");
			 tm.addCellToTable("Table E1", "gout", "PAIRWISE212", "0.055959");
			 tm.addCellToTable("Table E1", "gout", "PAIRWISE311", "0.061130");
			 tm.addCellToTable("Table E1", "gout", "PAIRWISE313", "0.064713");
			 tm.addCellToTable("Table E1", "gout", "PAIRWISE322", "0.046035");
			 tm.addCellToTable("Table E1", "gout", "PAIRWISE323", "0.044561");
			 tm.addCellToTable("Table E1", "gout", "THREE_WAY1", "0.056596");
			 tm.addCellToTable("Table E1", "gout", "THREE_WAY2", "0.054229");
			 tm.addCellToTable("Table E1", "gout", "THREE_WAY3", "0.058748");
			 tm.addCellToTable("Table E1", "h2block_rx", "FULL1", "0.084325");
			 tm.addCellToTable("Table E1", "h2block_rx", "FULL2", "0.118600");
			 tm.addCellToTable("Table E1", "h2block_rx", "FULL3", "0.116340");
			 tm.addCellToTable("Table E1", "h2block_rx", "MERGED1", "0.088826");
			 tm.addCellToTable("Table E1", "h2block_rx", "MERGED2", "0.089699");
			 tm.addCellToTable("Table E1", "h2block_rx", "MERGED3", "0.087080");
			 tm.addCellToTable("Table E1", "h2block_rx", "PAIRWISE211", "0.087142");
			 tm.addCellToTable("Table E1", "h2block_rx", "PAIRWISE212", "0.087783");
			 tm.addCellToTable("Table E1", "h2block_rx", "PAIRWISE311", "0.086214");
			 tm.addCellToTable("Table E1", "h2block_rx", "PAIRWISE313", "0.085371");
			 tm.addCellToTable("Table E1", "h2block_rx", "PAIRWISE322", "0.118447");
			 tm.addCellToTable("Table E1", "h2block_rx", "PAIRWISE323", "0.119266");
			 tm.addCellToTable("Table E1", "h2block_rx", "THREE_WAY1", "0.088229");
			 tm.addCellToTable("Table E1", "h2block_rx", "THREE_WAY2", "0.085001");
			 tm.addCellToTable("Table E1", "h2block_rx", "THREE_WAY3", "0.089090");
			 tm.addCellToTable("Table E1", "hep_liv_alcohol", "FULL1", "0.037341");
			 tm.addCellToTable("Table E1", "hep_liv_alcohol", "FULL2", "0.038075");
			 tm.addCellToTable("Table E1", "hep_liv_alcohol", "FULL3", "0.043806");
			 tm.addCellToTable("Table E1", "hep_liv_alcohol", "MERGED1", "0.038193");
			 tm.addCellToTable("Table E1", "hep_liv_alcohol", "MERGED2", "0.037538");
			 tm.addCellToTable("Table E1", "hep_liv_alcohol", "MERGED3", "0.039721");
			 tm.addCellToTable("Table E1", "hep_liv_alcohol", "PAIRWISE211", "0.038018");
			 tm.addCellToTable("Table E1", "hep_liv_alcohol", "PAIRWISE212", "0.037164");
			 tm.addCellToTable("Table E1", "hep_liv_alcohol", "PAIRWISE311", "0.037732");
			 tm.addCellToTable("Table E1", "hep_liv_alcohol", "PAIRWISE313", "0.039629");
			 tm.addCellToTable("Table E1", "hep_liv_alcohol", "PAIRWISE322", "0.038172");
			 tm.addCellToTable("Table E1", "hep_liv_alcohol", "PAIRWISE323", "0.039482");
			 tm.addCellToTable("Table E1", "hep_liv_alcohol", "THREE_WAY1", "0.037228");
			 tm.addCellToTable("Table E1", "hep_liv_alcohol", "THREE_WAY2", "0.037659");
			 tm.addCellToTable("Table E1", "hep_liv_alcohol", "THREE_WAY3", "0.040887");
			 tm.addCellToTable("Table E1", "hosp_days", "FULL1", "1.853098");
			 tm.addCellToTable("Table E1", "hosp_days", "FULL2", "2.190700");
			 tm.addCellToTable("Table E1", "hosp_days", "FULL3", "4.176335");
			 tm.addCellToTable("Table E1", "hosp_days", "MERGED1", "1.913575");
			 tm.addCellToTable("Table E1", "hosp_days", "MERGED2", "1.854212");
			 tm.addCellToTable("Table E1", "hosp_days", "MERGED3", "2.191183");
			 tm.addCellToTable("Table E1", "hosp_days", "PAIRWISE211", "1.884451");
			 tm.addCellToTable("Table E1", "hosp_days", "PAIRWISE212", "1.839385");
			 tm.addCellToTable("Table E1", "hosp_days", "PAIRWISE311", "1.891020");
			 tm.addCellToTable("Table E1", "hosp_days", "PAIRWISE313", "2.133221");
			 tm.addCellToTable("Table E1", "hosp_days", "PAIRWISE322", "2.214122");
			 tm.addCellToTable("Table E1", "hosp_days", "PAIRWISE323", "2.306684");
			 tm.addCellToTable("Table E1", "hosp_days", "THREE_WAY1", "1.902087");
			 tm.addCellToTable("Table E1", "hosp_days", "THREE_WAY2", "1.896708");
			 tm.addCellToTable("Table E1", "hosp_days", "THREE_WAY3", "2.111255");
			 tm.addCellToTable("Table E1", "hyp_dx", "FULL1", "0.706812");
			 tm.addCellToTable("Table E1", "hyp_dx", "FULL2", "0.695237");
			 tm.addCellToTable("Table E1", "hyp_dx", "FULL3", "0.720578");
			 tm.addCellToTable("Table E1", "hyp_dx", "MERGED1", "0.702313");
			 tm.addCellToTable("Table E1", "hyp_dx", "MERGED2", "0.699476");
			 tm.addCellToTable("Table E1", "hyp_dx", "MERGED3", "0.703405");
			 tm.addCellToTable("Table E1", "hyp_dx", "PAIRWISE211", "0.703118");
			 tm.addCellToTable("Table E1", "hyp_dx", "PAIRWISE212", "0.701410");
			 tm.addCellToTable("Table E1", "hyp_dx", "PAIRWISE311", "0.705312");
			 tm.addCellToTable("Table E1", "hyp_dx", "PAIRWISE313", "0.704469");
			 tm.addCellToTable("Table E1", "hyp_dx", "PAIRWISE322", "0.696429");
			 tm.addCellToTable("Table E1", "hyp_dx", "PAIRWISE323", "0.696429");
			 tm.addCellToTable("Table E1", "hyp_dx", "THREE_WAY1", "0.702819");
			 tm.addCellToTable("Table E1", "hyp_dx", "THREE_WAY2", "0.709705");
			 tm.addCellToTable("Table E1", "hyp_dx", "THREE_WAY3", "0.704110");
			 tm.addCellToTable("Table E1", "hyperlipid", "FULL1", "0.659622");
			 tm.addCellToTable("Table E1", "hyperlipid", "FULL2", "0.620220");
			 tm.addCellToTable("Table E1", "hyperlipid", "FULL3", "0.631775");
			 tm.addCellToTable("Table E1", "hyperlipid", "MERGED1", "0.650808");
			 tm.addCellToTable("Table E1", "hyperlipid", "MERGED2", "0.650808");
			 tm.addCellToTable("Table E1", "hyperlipid", "MERGED3", "0.662375");
			 tm.addCellToTable("Table E1", "hyperlipid", "PAIRWISE211", "0.652285");
			 tm.addCellToTable("Table E1", "hyperlipid", "PAIRWISE212", "0.655062");
			 tm.addCellToTable("Table E1", "hyperlipid", "PAIRWISE311", "0.657673");
			 tm.addCellToTable("Table E1", "hyperlipid", "PAIRWISE313", "0.662310");
			 tm.addCellToTable("Table E1", "hyperlipid", "PAIRWISE322", "0.621560");
			 tm.addCellToTable("Table E1", "hyperlipid", "PAIRWISE323", "0.623198");
			 tm.addCellToTable("Table E1", "hyperlipid", "THREE_WAY1", "0.653325");
			 tm.addCellToTable("Table E1", "hyperlipid", "THREE_WAY2", "0.656983");
			 tm.addCellToTable("Table E1", "hyperlipid", "THREE_WAY3", "0.656768");
			 tm.addCellToTable("Table E1", "loop_rx", "FULL1", "0.213172");
			 tm.addCellToTable("Table E1", "loop_rx", "FULL2", "0.257777");
			 tm.addCellToTable("Table E1", "loop_rx", "FULL3", "0.312674");
			 tm.addCellToTable("Table E1", "loop_rx", "MERGED1", "0.222174");
			 tm.addCellToTable("Table E1", "loop_rx", "MERGED2", "0.220864");
			 tm.addCellToTable("Table E1", "loop_rx", "MERGED3", "0.223701");
			 tm.addCellToTable("Table E1", "loop_rx", "PAIRWISE211", "0.218710");
			 tm.addCellToTable("Table E1", "loop_rx", "PAIRWISE212", "0.217856");
			 tm.addCellToTable("Table E1", "loop_rx", "PAIRWISE311", "0.217538");
			 tm.addCellToTable("Table E1", "loop_rx", "PAIRWISE313", "0.219435");
			 tm.addCellToTable("Table E1", "loop_rx", "PAIRWISE322", "0.259010");
			 tm.addCellToTable("Table E1", "loop_rx", "PAIRWISE323", "0.257372");
			 tm.addCellToTable("Table E1", "loop_rx", "THREE_WAY1", "0.219927");
			 tm.addCellToTable("Table E1", "loop_rx", "THREE_WAY2", "0.217129");
			 tm.addCellToTable("Table E1", "loop_rx", "THREE_WAY3", "0.227243");
			 tm.addCellToTable("Table E1", "male", "FULL1", "0.160033");
			 tm.addCellToTable("Table E1", "male", "FULL2", "0.143065");
			 tm.addCellToTable("Table E1", "male", "FULL3", "0.159908");
			 tm.addCellToTable("Table E1", "male", "MERGED1", "0.154954");
			 tm.addCellToTable("Table E1", "male", "MERGED2", "0.147970");
			 tm.addCellToTable("Table E1", "male", "MERGED3", "0.171323");
			 tm.addCellToTable("Table E1", "male", "PAIRWISE211", "0.154848");
			 tm.addCellToTable("Table E1", "male", "PAIRWISE212", "0.149722");
			 tm.addCellToTable("Table E1", "male", "PAIRWISE311", "0.159359");
			 tm.addCellToTable("Table E1", "male", "PAIRWISE313", "0.170531");
			 tm.addCellToTable("Table E1", "male", "PAIRWISE322", "0.144168");
			 tm.addCellToTable("Table E1", "male", "PAIRWISE323", "0.141710");
			 tm.addCellToTable("Table E1", "male", "THREE_WAY1", "0.158167");
			 tm.addCellToTable("Table E1", "male", "THREE_WAY2", "0.153648");
			 tm.addCellToTable("Table E1", "male", "THREE_WAY3", "0.156660");
			 tm.addCellToTable("Table E1", "mi_dx", "FULL1", "0.052113");
			 tm.addCellToTable("Table E1", "mi_dx", "FULL2", "0.057356");
			 tm.addCellToTable("Table E1", "mi_dx", "FULL3", "0.095707");
			 tm.addCellToTable("Table E1", "mi_dx", "MERGED1", "0.054561");
			 tm.addCellToTable("Table E1", "mi_dx", "MERGED2", "0.054561");
			 tm.addCellToTable("Table E1", "mi_dx", "MERGED3", "0.053907");
			 tm.addCellToTable("Table E1", "mi_dx", "PAIRWISE211", "0.053610");
			 tm.addCellToTable("Table E1", "mi_dx", "PAIRWISE212", "0.054037");
			 tm.addCellToTable("Table E1", "mi_dx", "PAIRWISE311", "0.053331");
			 tm.addCellToTable("Table E1", "mi_dx", "PAIRWISE313", "0.052698");
			 tm.addCellToTable("Table E1", "mi_dx", "PAIRWISE322", "0.057995");
			 tm.addCellToTable("Table E1", "mi_dx", "PAIRWISE323", "0.060616");
			 tm.addCellToTable("Table E1", "mi_dx", "THREE_WAY1", "0.053798");
			 tm.addCellToTable("Table E1", "mi_dx", "THREE_WAY2", "0.052077");
			 tm.addCellToTable("Table E1", "mi_dx", "THREE_WAY3", "0.053798");
			 tm.addCellToTable("Table E1", "osteoporosis", "FULL1", "0.293394");
			 tm.addCellToTable("Table E1", "osteoporosis", "FULL2", "0.289047");
			 tm.addCellToTable("Table E1", "osteoporosis", "FULL3", "0.313308");
			 tm.addCellToTable("Table E1", "osteoporosis", "MERGED1", "0.292230");
			 tm.addCellToTable("Table E1", "osteoporosis", "MERGED2", "0.289612");
			 tm.addCellToTable("Table E1", "osteoporosis", "MERGED3", "0.296159");
			 tm.addCellToTable("Table E1", "osteoporosis", "PAIRWISE211", "0.291328");
			 tm.addCellToTable("Table E1", "osteoporosis", "PAIRWISE212", "0.289193");
			 tm.addCellToTable("Table E1", "osteoporosis", "PAIRWISE311", "0.294266");
			 tm.addCellToTable("Table E1", "osteoporosis", "PAIRWISE313", "0.294688");
			 tm.addCellToTable("Table E1", "osteoporosis", "PAIRWISE322", "0.290465");
			 tm.addCellToTable("Table E1", "osteoporosis", "PAIRWISE323", "0.299476");
			 tm.addCellToTable("Table E1", "osteoporosis", "THREE_WAY1", "0.291371");
			 tm.addCellToTable("Table E1", "osteoporosis", "THREE_WAY2", "0.295890");
			 tm.addCellToTable("Table E1", "osteoporosis", "THREE_WAY3", "0.292877");
			 tm.addCellToTable("Table E1", "parkinson", "FULL1", "0.025441");
			 tm.addCellToTable("Table E1", "parkinson", "FULL2", "0.030946");
			 tm.addCellToTable("Table E1", "parkinson", "FULL3", "0.035870");
			 tm.addCellToTable("Table E1", "parkinson", "MERGED1", "0.027062");
			 tm.addCellToTable("Table E1", "parkinson", "MERGED2", "0.027062");
			 tm.addCellToTable("Table E1", "parkinson", "MERGED3", "0.029245");
			 tm.addCellToTable("Table E1", "parkinson", "PAIRWISE211", "0.026484");
			 tm.addCellToTable("Table E1", "parkinson", "PAIRWISE212", "0.026698");
			 tm.addCellToTable("Table E1", "parkinson", "PAIRWISE311", "0.026138");
			 tm.addCellToTable("Table E1", "parkinson", "PAIRWISE313", "0.028457");
			 tm.addCellToTable("Table E1", "parkinson", "PAIRWISE322", "0.031291");
			 tm.addCellToTable("Table E1", "parkinson", "PAIRWISE323", "0.031127");
			 tm.addCellToTable("Table E1", "parkinson", "THREE_WAY1", "0.026253");
			 tm.addCellToTable("Table E1", "parkinson", "THREE_WAY2", "0.027975");
			 tm.addCellToTable("Table E1", "parkinson", "THREE_WAY3", "0.022380");
			 tm.addCellToTable("Table E1", "ppi_rx", "FULL1", "0.226713");
			 tm.addCellToTable("Table E1", "ppi_rx", "FULL2", "0.238820");
			 tm.addCellToTable("Table E1", "ppi_rx", "FULL3", "0.291882");
			 tm.addCellToTable("Table E1", "ppi_rx", "MERGED1", "0.231995");
			 tm.addCellToTable("Table E1", "ppi_rx", "MERGED2", "0.231558");
			 tm.addCellToTable("Table E1", "ppi_rx", "MERGED3", "0.226539");
			 tm.addCellToTable("Table E1", "ppi_rx", "PAIRWISE211", "0.229176");
			 tm.addCellToTable("Table E1", "ppi_rx", "PAIRWISE212", "0.230457");
			 tm.addCellToTable("Table E1", "ppi_rx", "PAIRWISE311", "0.230607");
			 tm.addCellToTable("Table E1", "ppi_rx", "PAIRWISE313", "0.222175");
			 tm.addCellToTable("Table E1", "ppi_rx", "PAIRWISE322", "0.240826");
			 tm.addCellToTable("Table E1", "ppi_rx", "PAIRWISE323", "0.235092");
			 tm.addCellToTable("Table E1", "ppi_rx", "THREE_WAY1", "0.231117");
			 tm.addCellToTable("Table E1", "ppi_rx", "THREE_WAY2", "0.229395");
			 tm.addCellToTable("Table E1", "ppi_rx", "THREE_WAY3", "0.225952");
			 tm.addCellToTable("Table E1", "race_w", "FULL1", "0.846122");
			 tm.addCellToTable("Table E1", "race_w", "FULL2", "0.880104");
			 tm.addCellToTable("Table E1", "race_w", "FULL3", "0.923657");
			 tm.addCellToTable("Table E1", "race_w", "MERGED1", "0.874727");
			 tm.addCellToTable("Table E1", "race_w", "MERGED2", "0.860978");
			 tm.addCellToTable("Table E1", "race_w", "MERGED3", "0.865343");
			 tm.addCellToTable("Table E1", "race_w", "PAIRWISE211", "0.858180");
			 tm.addCellToTable("Table E1", "race_w", "PAIRWISE212", "0.856899");
			 tm.addCellToTable("Table E1", "race_w", "PAIRWISE311", "0.866568");
			 tm.addCellToTable("Table E1", "race_w", "PAIRWISE313", "0.855818");
			 tm.addCellToTable("Table E1", "race_w", "PAIRWISE322", "0.889744");
			 tm.addCellToTable("Table E1", "race_w", "PAIRWISE323", "0.887615");
			 tm.addCellToTable("Table E1", "race_w", "THREE_WAY1", "0.867441");
			 tm.addCellToTable("Table E1", "race_w", "THREE_WAY2", "0.863998");
			 tm.addCellToTable("Table E1", "race_w", "THREE_WAY3", "0.861846");
			 tm.addCellToTable("Table E1", "score", "FULL1", "1.591096");
			 tm.addCellToTable("Table E1", "score", "FULL2", "1.720512");
			 tm.addCellToTable("Table E1", "score", "FULL3", "2.166019");
			 tm.addCellToTable("Table E1", "score", "MERGED1", "1.622654");
			 tm.addCellToTable("Table E1", "score", "MERGED2", "1.632038");
			 tm.addCellToTable("Table E1", "score", "MERGED3", "1.664339");
			 tm.addCellToTable("Table E1", "score", "PAIRWISE211", "1.607646");
			 tm.addCellToTable("Table E1", "score", "PAIRWISE212", "1.627723");
			 tm.addCellToTable("Table E1", "score", "PAIRWISE311", "1.611298");
			 tm.addCellToTable("Table E1", "score", "PAIRWISE313", "1.642285");
			 tm.addCellToTable("Table E1", "score", "PAIRWISE322", "1.728866");
			 tm.addCellToTable("Table E1", "score", "PAIRWISE323", "1.721986");
			 tm.addCellToTable("Table E1", "score", "THREE_WAY1", "1.623843");
			 tm.addCellToTable("Table E1", "score", "THREE_WAY2", "1.630514");
			 tm.addCellToTable("Table E1", "score", "THREE_WAY3", "1.633527");
			 tm.addCellToTable("Table E1", "ssri_rx", "FULL1", "0.120845");
			 tm.addCellToTable("Table E1", "ssri_rx", "FULL2", "0.136423");
			 tm.addCellToTable("Table E1", "ssri_rx", "FULL3", "0.156099");
			 tm.addCellToTable("Table E1", "ssri_rx", "MERGED1", "0.126801");
			 tm.addCellToTable("Table E1", "ssri_rx", "MERGED2", "0.120253");
			 tm.addCellToTable("Table E1", "ssri_rx", "MERGED3", "0.124182");
			 tm.addCellToTable("Table E1", "ssri_rx", "PAIRWISE211", "0.124092");
			 tm.addCellToTable("Table E1", "ssri_rx", "PAIRWISE212", "0.119180");
			 tm.addCellToTable("Table E1", "ssri_rx", "PAIRWISE311", "0.123946");
			 tm.addCellToTable("Table E1", "ssri_rx", "PAIRWISE313", "0.120995");
			 tm.addCellToTable("Table E1", "ssri_rx", "PAIRWISE322", "0.137451");
			 tm.addCellToTable("Table E1", "ssri_rx", "PAIRWISE323", "0.135649");
			 tm.addCellToTable("Table E1", "ssri_rx", "THREE_WAY1", "0.125027");
			 tm.addCellToTable("Table E1", "ssri_rx", "THREE_WAY2", "0.122660");
			 tm.addCellToTable("Table E1", "ssri_rx", "THREE_WAY3", "0.120723");
			 tm.addCellToTable("Table E1", "stroke_dx", "FULL1", "0.151826");
			 tm.addCellToTable("Table E1", "stroke_dx", "FULL2", "0.160564");
			 tm.addCellToTable("Table E1", "stroke_dx", "FULL3", "0.215300");
			 tm.addCellToTable("Table E1", "stroke_dx", "MERGED1", "0.154518");
			 tm.addCellToTable("Table E1", "stroke_dx", "MERGED2", "0.156264");
			 tm.addCellToTable("Table E1", "stroke_dx", "MERGED3", "0.156700");
			 tm.addCellToTable("Table E1", "stroke_dx", "PAIRWISE211", "0.153140");
			 tm.addCellToTable("Table E1", "stroke_dx", "PAIRWISE212", "0.155916");
			 tm.addCellToTable("Table E1", "stroke_dx", "PAIRWISE311", "0.153668");
			 tm.addCellToTable("Table E1", "stroke_dx", "PAIRWISE313", "0.154511");
			 tm.addCellToTable("Table E1", "stroke_dx", "PAIRWISE322", "0.162189");
			 tm.addCellToTable("Table E1", "stroke_dx", "PAIRWISE323", "0.161533");
			 tm.addCellToTable("Table E1", "stroke_dx", "THREE_WAY1", "0.153863");
			 tm.addCellToTable("Table E1", "stroke_dx", "THREE_WAY2", "0.158167");
			 tm.addCellToTable("Table E1", "stroke_dx", "THREE_WAY3", "0.151065");
			 tm.addCellToTable("Table E1", "thiazide_rx", "FULL1", "0.147107");
			 tm.addCellToTable("Table E1", "thiazide_rx", "FULL2", "0.126215");
			 tm.addCellToTable("Table E1", "thiazide_rx", "FULL3", "0.147528");
			 tm.addCellToTable("Table E1", "thiazide_rx", "MERGED1", "0.138586");
			 tm.addCellToTable("Table E1", "thiazide_rx", "MERGED2", "0.138586");
			 tm.addCellToTable("Table E1", "thiazide_rx", "MERGED3", "0.140550");
			 tm.addCellToTable("Table E1", "thiazide_rx", "PAIRWISE211", "0.139257");
			 tm.addCellToTable("Table E1", "thiazide_rx", "PAIRWISE212", "0.140752");
			 tm.addCellToTable("Table E1", "thiazide_rx", "PAIRWISE311", "0.145025");
			 tm.addCellToTable("Table E1", "thiazide_rx", "PAIRWISE313", "0.141020");
			 tm.addCellToTable("Table E1", "thiazide_rx", "PAIRWISE322", "0.126802");
			 tm.addCellToTable("Table E1", "thiazide_rx", "PAIRWISE323", "0.122051");
			 tm.addCellToTable("Table E1", "thiazide_rx", "THREE_WAY1", "0.141166");
			 tm.addCellToTable("Table E1", "thiazide_rx", "THREE_WAY2", "0.139445");
			 tm.addCellToTable("Table E1", "thiazide_rx", "THREE_WAY3", "0.137723");
			 tm.addCellToTable("Table E1", "ugi_esophag", "FULL1", "0.025236");
			 tm.addCellToTable("Table E1", "ugi_esophag", "FULL2", "0.029488");
			 tm.addCellToTable("Table E1", "ugi_esophag", "FULL3", "0.037695");
			 tm.addCellToTable("Table E1", "ugi_esophag", "MERGED1", "0.025535");
			 tm.addCellToTable("Table E1", "ugi_esophag", "MERGED2", "0.025316");
			 tm.addCellToTable("Table E1", "ugi_esophag", "MERGED3", "0.024880");
			 tm.addCellToTable("Table E1", "ugi_esophag", "PAIRWISE211", "0.025630");
			 tm.addCellToTable("Table E1", "ugi_esophag", "PAIRWISE212", "0.024776");
			 tm.addCellToTable("Table E1", "ugi_esophag", "PAIRWISE311", "0.025295");
			 tm.addCellToTable("Table E1", "ugi_esophag", "PAIRWISE313", "0.024452");
			 tm.addCellToTable("Table E1", "ugi_esophag", "PAIRWISE322", "0.029489");
			 tm.addCellToTable("Table E1", "ugi_esophag", "PAIRWISE323", "0.030308");
			 tm.addCellToTable("Table E1", "ugi_esophag", "THREE_WAY1", "0.025608");
			 tm.addCellToTable("Table E1", "ugi_esophag", "THREE_WAY2", "0.025393");
			 tm.addCellToTable("Table E1", "ugi_esophag", "THREE_WAY3", "0.024102");
			 tm.addCellToTable("Table E1", "visits", "FULL1", "8.719943");
			 tm.addCellToTable("Table E1", "visits", "FULL2", "8.799417");
			 tm.addCellToTable("Table E1", "visits", "FULL3", "10.084596");
			 tm.addCellToTable("Table E1", "visits", "MERGED1", "8.766696");
			 tm.addCellToTable("Table E1", "visits", "MERGED2", "8.695330");
			 tm.addCellToTable("Table E1", "visits", "MERGED3", "8.928416");
			 tm.addCellToTable("Table E1", "visits", "PAIRWISE211", "8.716788");
			 tm.addCellToTable("Table E1", "visits", "PAIRWISE212", "8.699060");
			 tm.addCellToTable("Table E1", "visits", "PAIRWISE311", "8.777403");
			 tm.addCellToTable("Table E1", "visits", "PAIRWISE313", "8.857504");
			 tm.addCellToTable("Table E1", "visits", "PAIRWISE322", "8.842562");
			 tm.addCellToTable("Table E1", "visits", "PAIRWISE323", "8.835026");
			 tm.addCellToTable("Table E1", "visits", "THREE_WAY1", "8.769744");
			 tm.addCellToTable("Table E1", "visits", "THREE_WAY2", "8.775124");
			 tm.addCellToTable("Table E1", "visits", "THREE_WAY3", "8.800732");
			 tm.addCellToTable("Table E1", "dist2", "FULL1", "2.115986");
			 tm.addCellToTable("Table E1", "dist2", "MERGED1", "-0.270741");
			 tm.addCellToTable("Table E1", "dist2", "PAIRWISE211", "0.131853");
			 tm.addCellToTable("Table E1", "dist2", "THREE_WAY1", "0.116554");
			 tm.addCellToTable("Table E1", "dist3", "FULL1", "11.334650");
			 tm.addCellToTable("Table E1", "dist3", "FULL2", "9.218663");
			 tm.addCellToTable("Table E1", "dist3", "MERGED1", "0.660941");
			 tm.addCellToTable("Table E1", "dist3", "MERGED2", "0.931682");
			 tm.addCellToTable("Table E1", "dist3", "PAIRWISE311", "0.210422");
			 tm.addCellToTable("Table E1", "dist3", "PAIRWISE322", "0.001628");
			 tm.addCellToTable("Table E1", "dist3", "THREE_WAY1", "-0.161234");
			 tm.addCellToTable("Table E1", "dist3", "THREE_WAY2", "-0.277788");
			 tm.addCellToTable("Table E1", "mdist2", "FULL1", "8.694892");
			 tm.addCellToTable("Table E1", "mdist2", "MERGED1", "0.488921");
			 tm.addCellToTable("Table E1", "mdist2", "PAIRWISE211", "0.263854");
			 tm.addCellToTable("Table E1", "mdist2", "THREE_WAY1", "0.328244");
			 tm.addCellToTable("Table E1", "mdist3", "FULL1", "34.035399");
			 tm.addCellToTable("Table E1", "mdist3", "FULL2", "18.972794");
			 tm.addCellToTable("Table E1", "mdist3", "MERGED1", "0.967300");
			 tm.addCellToTable("Table E1", "mdist3", "MERGED2", "1.562176");
			 tm.addCellToTable("Table E1", "mdist3", "PAIRWISE311", "0.607628");
			 tm.addCellToTable("Table E1", "mdist3", "PAIRWISE322", "0.290499");
			 tm.addCellToTable("Table E1", "mdist3", "THREE_WAY1", "0.547729");
			 tm.addCellToTable("Table E1", "mdist3", "THREE_WAY2", "0.963699");
			 
			 tm.writeTablesToWorkbook(
					 "/Users/jeremy/Dropbox/JAR73/Projects/matching/Three Way Matching Tables.xls", 
			 		"/Users/jeremy/Dropbox/JAR73/Projects/matching/Three Way Matching Tables Output.xlsx");
			 
			 System.out.println(tm.toString());
			 
//			 tm.createTablesFromWorkbook("documentation/Table Creator Example.xls");
			 
			 
			 
			 
			 
//			 System.out.println("Starting");
//			 TableCreator tm = new TableCreator();
//			 tm.addTable("Table1", "Table 1");
//			 tm.addRowToTable("Table1", null, "RA", "RA");
//			 tm.addRowToTable("Table1", "RA", "Statin", "Statin");
//			 tm.addRowToTable("Table1", "RA", "Statin1", "Statin1");
//			 tm.addRowToTable("Table1", "Statin1", "A", "A");
//			 
//			 tm.addRowToTable("Table1", "RA", "Other", "Other");
//			 tm.addRowToTable("Table1", "Other", "C", "C");
//			 tm.addRowToTable("Table1", "Other", "D", "D");
//			 tm.addRowToTable("Table1", "Statin1", "B", "B");
//			 tm.addRowToTable("Table1", "B", "BG", "BG");
//			 
//			 tm.addColToTable("Table1", null, "Estimate", "Estimate");
//		//	 tm.addColToTable("Table1", "Estimate", "OR", "OR");
//		// 	 tm.addColToTable("Table1", "Estimate", "CI", "CI");
//			 
//			 tm.addColToTable("Table1", null, "Est2", "Est2");
//			 tm.addColToTable("Table1", null, "Est3", "Est3");
//			 tm.addColToTable("Table1", null, "Est5", "Est5");
//			 tm.addColToTable("Table1", null, "Est4", "Est4");
//			 tm.addColToTable("Table1", null, "Est7", "Est7");			 
//			 tm.addColToTable("Table1", null, "Est6", "Est6");
//			 tm.addColToTable("Table1", "Est4", "est4dnnd", "est4ddn");
//			 tm.addColToTable("Table1", "Est4", "qnnq", "est4qnq");
//			 tm.addColToTable("Table1", "Est4", "q7nq", "est4qdnq");
//			 tm.addColToTable("Table1", "Est2", "dd", "est2dd");
//			 tm.addColToTable("Table1", "Est2", "qq", "est2qq");
//			 tm.addColToTable("Table1", "Est2", "rr", "est2rr");
//			 tm.addColToTable("Table1", "Est2", "rrp", "est2rrr");
//
//
//			 tm.addColToTable("Table1", "Est4", "q6nq", "est4qddnq");
//			 tm.addColToTable("Table1", "Est4", "q6nq6", "est4qddnq");
//			 tm.addColToTable("Table1", "Est4", "q6nq7", "est4qddnq");
//			 
//			 tm.addColToTable("Table1", "Est6", "q6dnq", "est6qddnq");
//			 tm.addColToTable("Table1", "Est6", "q6ndq6", "est6qddnq");
//			 tm.addColToTable("Table1", "Est6", "q6ndq7", "est6qddnq");
//			 tm.addColToTable("Table1", "Est6", "q6ndq78", "est6qddnqd");
//			 
//			 tm.addColToTable("Table1", "rr", "rr3", "est2rr3");
//			 tm.addColToTable("Table1", "rr", "rr33", "est2rr33");
//			 tm.addColToTable("Table1", "rr3", "rr34", "est2rr34");
///*			 tm.addColToTable("Table1", "q6dnq", "est6qddnqrr34", "est6qddnqest2rr34");
//			 tm.addColToTable("Table1", "est6qddnqrr34", "est6qddnqrr4", "est6qddnqest2rr4");
//			 tm.addColToTable("Table1", "q6ndq78", "rrdd34", "est2ddrr34");
//			 tm.addColToTable("Table1", "rrp", "rrp4", "est2rrp4");
//*/		 
//			 tm.addCellToTable("Table1", "A", "Est5", "Bad");
//			 tm.addCellToTable("Table1", "A", "Estimate", "Hello!");
//			 
//			 tm.addCellToTable("Table1", "A", "dd", "Hello_hoof");
//		 	 
////		 	tm.addCellToTable("Table1", "BG", "rr3", "goodtoo");
//			 	tm.addCellToTable("Table1", "BG", "rr34", "good");
//			 	tm.addCellToTable("Table1", "C", "q6ndq7", "Hello_you");
//			 	tm.addCellToTable("Table1", "Statin", "est4dnnd", "Hello_you_too");
//
//			 	
//			 	tm.addFootnoteToTable("Table1", null, "rr34", "A", "This is footnote.");
//			 	tm.addFootnoteToTable("Table1", "BG", "rr34", "B", "This is also a footnote.");
//			 	tm.addFootnoteToTable("Table1", "BG", "rr34", "C", "1");
//
//
//			 tm.setRowsTitle("Table1", "ABCDEFGHIJKLMNOP!");
//			 tm.writeHtmlToFile("Table1", "/Users/jeremy/Desktop");
//			 
//		//	 tm.tables.get(TableElement.makeId("Table1")).toHtml();
		 } catch (Exception e) {
			 e.printStackTrace();
		 }
		 
		 System.out.println("Done");
	 }
}
