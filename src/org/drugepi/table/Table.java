/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.table;

import java.io.InputStream;
import java.util.*;

import org.drugepi.table.TableRowCol.RowColTypes;

public class Table extends TableElement {
	String						rowsTitle;
	
	Map<String, TableRowCol>	rowsMap;
	Map<String, TableRowCol>	colsMap;
	List<TableRowCol>	rowsList;
	List<TableRowCol>	colsList;

	Map<String, TableCell>	cells;

	List <TableFootnote> footnotes;
	
	public Table(String id, String description) {
		super(id, description);
		
		this.rowsMap = new HashMap<String, TableRowCol>();
		this.colsMap = new HashMap<String, TableRowCol>();
		
		this.rowsList = new ArrayList<TableRowCol>();
		this.colsList = new ArrayList<TableRowCol>();
		
		this.cells = new HashMap<String, TableCell>();
		
		this.footnotes = new ArrayList<TableFootnote>();
		
		this.rowsTitle = "";
	}

	public void addRow(TableRowCol row) {
		rowsMap.put(row.id, row);
		if (rowsList.contains(row))
			rowsList.remove(row);
		rowsList.add(row);

	}

	public void addCol(TableRowCol col) {
		colsMap.put(col.id, col);
		if (colsList.contains(col))
			colsList.remove(col);
		colsList.add(col);

	}
	
	public void addCell(TableCell cell) {
		cells.put(cell.id, cell);
	}
	
	public List<TableRowCol> getRowsColsAtLevel(List<TableRowCol> rcs, int level) {
		ArrayList<TableRowCol> rowsColsAtLevel = new ArrayList<TableRowCol>();
		
		for (TableRowCol rc: rcs) {
			if (TableRowCol.getDepth(rc) == level)
				rowsColsAtLevel.add(rc);
		}
		
		return rowsColsAtLevel;
	}
	
	public String getRowHtml(List<TableRowCol> rows, List<TableRowCol> leafCols, int level, int maxRowDepth) {
		StringBuffer html = new StringBuffer();
		ArrayList<TableRowCol> final_col_position = new ArrayList<TableRowCol>();
		
		for (TableRowCol row: rows) {
			boolean printTr = false;
			
			if ((row.parent == null) ||
				(row.parent.children.get(0) != row))
				printTr = true;
			
			if (printTr) 
				html.append("<tr>\n");
			
			String rowClass = (row.rcType == RowColTypes.NORMAL ? "row_normal" : "row_header");
			html.append("<td ID=\"" + row.id + "\" class='" + rowClass + "' rowspan=" + TableRowCol.getNumLeaves(row) + ">" + 
					row.description);
			if (row.footnotes.size() > 0) {
				html.append("<sup><em>");
				html.append(row.getFootnotesSymbolList());
				html.append("</em></sup>");
				this.footnotes.addAll(row.footnotes);
			}
			html.append("</td>\n");

			if (row.children.size() > 0)
				html.append(getRowHtml(row.children, leafCols, level + 1, maxRowDepth));

			if (row.children.size() == 0) {
				
				final_col_position.clear();
				for (int j = 0; j < maxRowDepth-level; j++) {
					final_col_position.add(new TableRowCol("",""));
				}
				for (int i = 0; i < leafCols.size(); i++) {
					final_col_position.add(leafCols.get(i));
				}
				
				for (int i = 0; i < final_col_position.size(); i++) {
					TableRowCol col = final_col_position.get(i);
				
					TableCell cell = this.cells.get(TableCell.getCellId(row.id, col.id));
					if (cell == null)
						html.append("<td>&nbsp;</td>\n");
					else {
						html.append("<td ID=\"" + cell.id + "\" class='cell'>" + cell.description);
						if (cell.footnotes.size() > 0) {
							html.append("<sup><em>");
							html.append(cell.getFootnotesSymbolList());
							html.append("</em></sup>");
							this.footnotes.addAll(cell.footnotes);
						}
						html.append("</td>\n");
					}
				}
				html.append("</tr>\n");
			}
		}
		
		return html.toString();
	}
	
	public String toHtml() 
	throws Exception
	{ //1
		StringBuffer html = new StringBuffer();
		
		int headerRowDepth = TableRowCol.getMaxDepth(this.rowsMap.values());
		int headerColDepth = TableRowCol.getMaxDepth(this.colsMap.values());
		
		List<TableRowCol> leafCols = new ArrayList<TableRowCol>();
		List<TableRowCol> parentCols = new ArrayList<TableRowCol>();
		List<TableRowCol> temp_parentCols = new ArrayList<TableRowCol>();
		ArrayList<Integer> parentCols_index_object = new ArrayList<Integer>();
		
		InputStream in = Table.class.getResourceAsStream("TableHeader.html");
		byte[] buf = new byte[4096];
		while (in.read(buf) > 0)
			html.append(new String(buf));
		
		html.append("<p class='table_title'>" + this.description + "</p>\n");
		html.append("<table class='generated_table'>\n");
		
		// make the column headers
		for (int level = 1; level <= headerColDepth; level++)
		  { //2
			// leave headerRowDepth x headerRowDepth columns open
			if (level == 1)
			  { //3
				html.append("<td ID='ROWS_TITLE' class='rows_title' colspan=" + headerRowDepth + "" +
						" rowspan=" + headerColDepth + ">" +
						this.rowsTitle + "</td>");
				List<TableRowCol> pcols = this.getRowsColsAtLevel(this.colsList, level);
				parentCols.addAll(pcols);
				if (!parentCols.isEmpty())
				{ //4
				for (TableRowCol col: parentCols) 
				  { //5
					String colClass = (col.rcType == RowColTypes.NORMAL ? "col_normal" : "col_header");
					html.append("<td ID=\"" + col.id + "\" class='" + colClass + "' colspan=" + TableRowCol.getNumLeaves(col) + ">"+col.description);
					if (col.footnotes.size() > 0) {
						html.append("<sup><em>");
						html.append(col.getFootnotesSymbolList());
						html.append("</em></sup>");
						this.footnotes.addAll(col.footnotes);
					}
					html.append("</td>\n");
				  }	//5*
				} //4*
			} //3*
			
			List<TableRowCol> cols = this.getRowsColsAtLevel(this.colsList, level);

			if (level!=1)
			{ //6

			    leafCols.clear();
				leafCols.addAll(cols);

	          
				if (!parentCols.isEmpty())
			     { //7
				   
		          parentCols_index_object.clear();	
                  int parentCols_index=0; 
		          for (TableRowCol col: parentCols)  
		             { //8
		        	     String colClass = (col.rcType == RowColTypes.NORMAL ? "col_normal" : "col_header");
				         if (col.children.size()==0)
					        { //9
				        	 html.append("<td></td>\n");
					        } //9*
				         else 
				            { //10
				        	 for (int i=0; i<leafCols.size(); i++ )
					         for (int j=0; j<col.children.size(); j++ )
				                {  //11
						           if(col.children.get(j)==leafCols.get(i))
						            { //12
   					                  html.append("<td ID=\"" + leafCols.get(i).id + "\" class='" + colClass + 
   					                		  "' colspan=" + TableRowCol.getNumLeaves(leafCols.get(i)) + ">"+
   					                		  leafCols.get(i).description);
	   					               if (leafCols.get(i).footnotes.size() > 0) {
	   									html.append("<sup><em>");
	   									html.append(leafCols.get(i).getFootnotesSymbolList());
	   									html.append("</em></sup>");
	   									this.footnotes.addAll(leafCols.get(i).footnotes);
	   					               }
	   					               html.append("</td>\n");
				       	            } //12*
				                } //11*
						      parentCols_index=parentCols.indexOf(col);
				              parentCols_index_object.add(new Integer(parentCols_index));
				             } //10*	
		             } //8*
				int p_index=0;
				int not_empty_child=0;
				for (int k=0; k<parentCols.size(); k++)
				 { not_empty_child=0;
				   for (int l=0;l<parentCols_index_object.size();l++)
				   { //13
					   p_index=parentCols_index_object.get(l);

					   if (k==p_index)
					     {
						   not_empty_child=1;
					     }

				    } //13*
				   
				   if (not_empty_child==0)
				     {
					   temp_parentCols.add(parentCols.get(k));
				     }
				   else
				     {
				       for (int m=0; m<parentCols.get(k).children.size(); m++)
				       {
				    	   temp_parentCols.add(parentCols.get(k).children.get(m));
	                    }
				     }
				}
				
				parentCols.clear();
				parentCols.addAll(temp_parentCols);
				temp_parentCols.clear();
				
			     } //7
			   
			   
			} //6*
		html.append("</tr>\n");
		} //2*
		html.append(this.getRowHtml(this.getRowsColsAtLevel(this.rowsList, 1), parentCols, 1, headerRowDepth));
		
		html.append("</table>\n");
		
		if (this.footnotes.size() > 0) {
			html.append("<br />");
			for (TableFootnote f: this.footnotes) 
				html.append("<p class='footnote'><sup><em>" + f.id + "</em></sup>&nbsp;&nbsp;" + f.description + "</p>");
			html.append("<br />");
		}
			
//		html.append("<p class='table_footer'>" + this.id + " generated at " + 
//				DateFormat.getInstance().format(new Date()) +
//				"</p>\n");

		in = Table.class.getResourceAsStream("TableFooter.html");
		buf = new byte[4096];
		while (in.read(buf) > 0)
			html.append(new String(buf));

		return html.toString();
	} //1*
	
	public String toString() {
		StringBuffer output = new StringBuffer();

		output.append(String.format(
		    "TABLE ID=%s, Description = %s\n",
		    this.id, this.description)
		);
		for (TableRowCol row: this.rowsList) {
			output.append(row.toString());
		}
		for (TableRowCol col: this.colsList) {
			output.append(col.toString());
		}
		for (TableCell cell: this.cells.values()) {
			output.append(cell.toString());
		}
		output.append("\n\n\n");
		
		return output.toString();
	}
}
