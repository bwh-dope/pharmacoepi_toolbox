/*
 * TABLE_CREATOR.SAS
 *
 * Copyright 2010 Brigham and Women's Hospital.  
 */
 
/*
	The contents of this file are subject to the Mozilla Public License Version 
	1.1 (the "License"); you may not use this file except in compliance with
	the License.  You may obtain a copy of the License at http://www.mozilla.org/MPL.
	
	The Original Code is the DoPE Pharmacoepidemiology Toolbox.
	
	The Initial Developer of the Original Code is the Brigham and Women's Hospital 
	Division of Pharmacoepidemiology.
	
	Contributor(s):
	   Jeremy A. Rassen <jrassen@post.harvard.edu>
 */

 

%GLOBAL table_LibName;
%GLOBAL table_EmptyCounter;

/*
Initialize the table creation functions.  Call this macro before beginning any 
other table operations.
*/
%macro table_Init(table_library);
	%LET table_LibName = &table_library;
	%LET table_EmptyCounter = 0;
	
	OPTION NONOTES;
	
	%IF NOT %SYSFUNC(EXIST(&table_LibName..table_def)) %THEN %DO;
		PROC SQL NOPRINT;
			CREATE TABLE &table_LibName..table_def (
				table_id	varchar(255),
				description	varchar(1000),
				rows_title	varchar(255),
				sequence	int,
				ts			int
			);
		QUIT;
	%END;

	%IF NOT %SYSFUNC(EXIST(&table_LibName..excel_table_def)) %THEN %DO;
		PROC SQL NOPRINT;
			CREATE TABLE &table_LibName..excel_table_def (
				workbook_path	varchar(1000),
				sequence	int,
				ts			int
			);
		QUIT;
	%END;


	%IF NOT %SYSFUNC(EXIST(&table_LibName..table_row_def)) %THEN %DO;
		PROC SQL NOPRINT;
			CREATE TABLE &table_LibName..table_row_def (
				row_id		varchar(255),
				table_id	varchar(255),
				parent_id	varchar(255),
				description	varchar(1000),
				row_type	varchar(1),
				sequence	int,
				ts			int
			);
		QUIT;
	%END;

	%IF NOT %SYSFUNC(EXIST(&table_LibName..table_col_def)) %THEN %DO;
		PROC SQL NOPRINT;
			CREATE TABLE &table_LibName..table_col_def (
				col_id		varchar(255),
				table_id	varchar(255),
				parent_id	varchar(255),
				description	varchar(1000),
				col_type	varchar(1),
				sequence	int,
				ts			int
			);
		QUIT;
	%END;

	%IF NOT %SYSFUNC(EXIST(&table_LibName..table_cells)) %THEN %DO;
		PROC SQL NOPRINT;
			CREATE TABLE &table_LibName..table_cells (
				table_id	varchar(255),
				row_id		varchar(255),
				col_id		varchar(255),
				contents	varchar(1000),
				sequence	int,
				ts			int
			);
		QUIT;
	%END;


	%IF NOT %SYSFUNC(EXIST(&table_LibName..table_footnotes)) %THEN %DO;
		PROC SQL NOPRINT;
			CREATE TABLE &table_LibName..table_footnotes (
				table_id	varchar(255),
				row_id		varchar(255),
				col_id		varchar(255),
				symbol		varchar(10),
				contents	varchar(1000),
				sequence	int,
				ts			int
			);
		QUIT;
	%END;
	OPTION NOTES;
%mend;

/*
Internal macro.  Do not call.
*/
%macro table_FinishInsertSQL(table_name);
	%LOCAL max_seq;

	SELECT MAX(sequence)
	INTO :max_seq
	FROM &table_LibName..&table_name;

	%IF &max_seq =  %THEN %LET max_seq = 1;
	%ELSE %IF &max_seq = . %THEN %LET max_seq = 1;
	%ELSE %LET max_seq = %EVAL(&max_seq + 1);

	UPDATE &table_LibName..&table_name
	SET sequence = &max_seq
	WHERE sequence IS NULL;

	UPDATE &table_LibName..&table_name
	SET ts = DATETIME()
	WHERE ts IS NULL;
%mend;

/*
Call to add table(s) defined in Excel to the output.
*/
%macro table_AddTablesFromExcel(workbook_path);
	OPTION NONOTES;
	
	PROC SQL NOPRINT;
		INSERT INTO &table_LibName..excel_table_def(workbook_path) 
		VALUES("&workbook_path");

		%table_FinishInsertSQL(table_def);
	QUIT;

	OPTION NOTES;
%mend;

/*
Call to add a table to the output.
*/
%macro table_AddTable(table_id, description);
	OPTION NONOTES;
	
	%LET table_id = %UPCASE(&table_id);

	PROC SQL NOPRINT;
		DELETE FROM &table_LibName..table_def
		WHERE table_id = "&table_id";

		DELETE FROM &table_LibName..table_row_def
		WHERE table_id = "&table_id";

		DELETE FROM &table_LibName..table_col_def
		WHERE table_id = "&table_id";

		DELETE FROM &table_LibName..table_cells
		WHERE table_id = "&table_id";

		DELETE FROM &table_LibName..table_footnotes
		WHERE table_id = "&table_id";

		INSERT INTO &table_LibName..table_def(table_id, description) 
		VALUES("&table_id", "&description");

		%table_FinishInsertSQL(table_def);
	QUIT;

	OPTION NOTES;
%mend;

/*
After creating a table, call to copy the structure of a source table to the new table.
*/
%macro table_Copy(source, dest);
	OPTION NONOTES;
	
	%LET source = %UPCASE(&source);
	%LET dest = %UPCASE(&dest);

	PROC SQL NOPRINT;
		CREATE TABLE t_table_copy AS
		SELECT rows_title
		FROM &table_LibName..table_def
		WHERE table_id = "&source";
		
		UPDATE &table_LibName..table_def
		SET rows_title = (SELECT MIN(rows_title) FROM t_table_copy)
		WHERE table_id = "&dest";

		CREATE TABLE t_table_copy AS
		SELECT row_id, "&dest", parent_id, description, row_type, sequence
		FROM &table_LibName..table_row_def
		WHERE table_id = "&source";

		INSERT INTO &table_LibName..table_row_def(row_id, table_id, parent_id, description, row_type, sequence)
		SELECT *
		FROM t_table_copy;
		%table_FinishInsertSQL(table_row_def);

		CREATE TABLE t_table_copy AS
		SELECT col_id, "&dest", parent_id, description, col_type, sequence
		FROM &table_LibName..table_col_def
		WHERE table_id = "&source";

		INSERT INTO &table_LibName..table_col_def(col_id, table_id, parent_id, description, col_type, sequence)
		SELECT *
		FROM t_table_copy;
		%table_FinishInsertSQL(table_col_def);

		CREATE TABLE t_table_copy AS
		SELECT "&dest", row_id, col_id, contents, sequence
		FROM &table_LibName..table_cells
		WHERE table_id = "&source";

		INSERT INTO &table_LibName..table_cells(table_id, row_id, col_id, contents, sequence)
		SELECT *
		FROM t_table_copy;
		%table_FinishInsertSQL(table_cells);
		
		CREATE TABLE t_table_copy AS
		SELECT "&dest", row_id, col_id, symbol, contents, sequence
		FROM &table_LibName..table_footnotes
		WHERE table_id = "&source";

		INSERT INTO &table_LibName..table_footnotes(table_id, row_id, col_id, symbol, contents, sequence)
		SELECT *
		FROM t_table_copy;
		%table_FinishInsertSQL(table_footnotes);
	QUIT;

	OPTION NOTES;
%mend;

/*
Add a row to the table.  parent_id indicates the row that this new row is nested in.
*/
%macro table_AddRow(table_id, row_id, description, parent_id=.);
	OPTION NONOTES;

	%LET table_id = %UPCASE(&table_id);
	%LET row_id = %UPCASE(&row_id);
	%LET parent_id = %UPCASE(&parent_id);

	PROC SQL NOPRINT;
		INSERT INTO &table_LibName..table_row_def(row_id, table_id, parent_id, description, row_type) 
		VALUES("&row_id", "&table_id", "&parent_id", "&description", "N");

		%table_FinishInsertSQL(table_row_def);
	QUIT;

	OPTION NOTES;
%mend;

%macro table_AddRowX(table_id, parent_id, row_id, description);
	%table_AddRow(&table_id, &row_id, &description, parent_id=&parent_id);
%mend;

/*
Add a header (bolded) row to the table.
*/
%macro table_AddHeaderRow(table_id, row_id, description, parent_id=.);
	OPTION NONOTES;

	%LET table_id = %UPCASE(&table_id);
	%LET row_id = %UPCASE(&row_id);
	%LET parent_id = %UPCASE(&parent_id);

	PROC SQL NOPRINT;
		INSERT INTO &table_LibName..table_row_def(row_id, table_id, parent_id, description, row_type) 
		VALUES("&row_id", "&table_id", "&parent_id", "&description", "H");

		%table_FinishInsertSQL(table_row_def);
	QUIT;

	OPTION NOTES;
%mend;

%macro table_AddHeaderRowX(table_id, parent_id, row_id, description);
	%table_AddHeaderRow(&table_id, &row_id, &description, parent_id=&parent_id);
%mend;

/*
Add a row to the table.  parent_id indicates the row that this new row is nested in.
*/
%macro table_AddEmptyRow(table_id, parent_id=.);
	%LET table_id = %UPCASE(&table_id);
	%LET parent_id = %UPCASE(&parent_id);

	PROC SQL NOPRINT;
		INSERT INTO &table_LibName..table_row_def(row_id, table_id, parent_id, description, row_type) 
		VALUES("EMPTY&table_EmptyCounter", "&table_id", "&parent_id", "", "N");
		
		%LET table_EmptyCounter = %EVAL(&table_EmptyCounter + 1);

		%table_FinishInsertSQL(table_row_def);
	QUIT;
%mend;

%macro table_AddEmptyRowX(table_id, parent_id);
	%table_AddEmptyRow(&table_id, parent_id=&parent_id);
%mend;

/*
Adds text to the row description.
*/
%macro table_AddToRowDescription(table_id, row_id, description);
	OPTION NONOTES;
	
	%LET table_id = %UPCASE(&table_id);
	%LET row_id = %UPCASE(&row_id);

	PROC SQL NOPRINT;
		UPDATE &table_LibName..table_row_def
		SET description = CATX(" ", description, "&description")
		WHERE table_id = "&table_id" AND row_id = "&row_id";
	QUIT;

	OPTION NOTES;
%mend;

/*
Adds text above the first table row.
*/
%macro table_SetRowsTitle(table_id, description);
	%LET table_id = %UPCASE(&table_id);

	OPTION NONOTES;

	PROC SQL NOPRINT;
		UPDATE &table_LibName..table_def
		SET rows_title = "&description"
		WHERE table_id = "&table_id";
	QUIT;
	
	OPTION NOTES;
%mend;

/*
Add a column to the table.  parent_id indicates the row that this new column is nested in.
*/
%macro table_AddCol(table_id, col_id, description, parent_id=.);
	%LET table_id = %UPCASE(&table_id);
	%LET col_id = %UPCASE(&col_id);
	%LET parent_id = %UPCASE(&parent_id);

	OPTION NONOTES;

	PROC SQL NOPRINT;
		INSERT INTO &table_LibName..table_col_def(col_id, table_id, parent_id, description, col_type) 
		VALUES("&col_id", "&table_id", "&parent_id", "&description", "N");

		%table_FinishInsertSQL(table_col_def);
	QUIT;

	OPTION NOTES;
%mend;

%macro table_AddColX(table_id, parent_id, col_id, description);
	%table_AddCol(&table_id, &col_id, &description, parent_id=&parent_id);
%mend;

/*
Add a header (bolded) column to this table.
*/
%macro table_AddHeaderCol(table_id, col_id, description, parent_id=.);
	%LET table_id = %UPCASE(&table_id);
	%LET col_id = %UPCASE(&col_id);
	%LET parent_id = %UPCASE(&parent_id);

	OPTION NONOTES;

	PROC SQL NOPRINT;
		INSERT INTO &table_LibName..table_col_def(col_id, table_id, parent_id, description, col_type) 
		VALUES("&col_id", "&table_id", "&parent_id", "&description", "H");

		%table_FinishInsertSQL(table_col_def);
	QUIT;

	OPTION NOTES;
%mend;

%macro table_AddHeaderColX(table_id, parent_id, col_id, description);
	%table_AddHeaderCol(&table_id, &col_id, &description, parent_id=&parent_id);
%mend;

/*
Adds text to the column description.
*/
%macro table_AddToColDescription(table_id, col_id, description);
	%LET table_id = %UPCASE(&table_id);
	%LET col_id = %UPCASE(&col_id);

	OPTION NONOTES;

	PROC SQL NOPRINT;
		UPDATE &table_LibName..table_col_def
		SET description = CATX(" ", description, "&description")
		WHERE table_id = "&table_id" AND col_id = "&col_id";
	QUIT;

	OPTION NOTES;
%mend;

/* 
Generic macro to fill a table cell with text.
*/
%macro table_FillCell(table_id, row_id, col_id, contents);
	%LET table_id = %UPCASE(&table_id);
	%LET col_id = %UPCASE(&col_id);
	%LET row_id = %UPCASE(&row_id);

	OPTION NONOTES;

	PROC SQL NOPRINT;
		INSERT INTO &table_LibName..table_cells(table_id, row_id, col_id, contents) 
		VALUES("&table_id", "&row_id", "&col_id", "&contents");

		%table_FinishInsertSQL(table_cells);
	QUIT;

	OPTION NOTES;
%mend;

/* 
Add a footnote to a cell.
*/
%macro table_AddFootnote(table_id, row_id, col_id, symbol, contents);
	%LET table_id = %UPCASE(&table_id);
	%LET col_id = %UPCASE(&col_id);
	%LET row_id = %UPCASE(&row_id);

	OPTION NONOTES;

	PROC SQL NOPRINT;
		INSERT INTO &table_LibName..table_footnotes(table_id, row_id, col_id, symbol, contents) 
		VALUES("&table_id", "&row_id", "&col_id", "&symbol", "&contents");

		%table_FinishInsertSQL(table_footnotes);
	QUIT;

	OPTION NOTES;
%mend;


/*
Fill a cell with numeric information.
*/
%macro table_FillCellNum(table_id, row_id, col_id, num, num_decimals = 0);
	%LET table_id = %UPCASE(&table_id);
	%LET col_id = %UPCASE(&col_id);
	%LET row_id = %UPCASE(&row_id);

	OPTION NONOTES;

	DATA _NULL_;
		CALL SYMPUT("num", STRIP(PUT(&num, 12.&num_decimals)));
	RUN;
	
	PROC SQL NOPRINT;
		INSERT INTO &table_LibName..table_cells(table_id, row_id, col_id, contents) 
		VALUES("&table_id", "&row_id", "&col_id", "&num");

		%table_FinishInsertSQL(table_cells);
	QUIT;

	OPTION NOTES;
%mend;

/*
Fill a cell with numeric information, adding commas.
*/
%macro table_FillCellCommaNum(table_id, row_id, col_id, num, num_decimals = 0);
	%LET table_id = %UPCASE(&table_id);
	%LET col_id = %UPCASE(&col_id);
	%LET row_id = %UPCASE(&row_id);

	OPTION NONOTES;

	DATA _NULL_;
		CALL SYMPUT("num", STRIP(PUT(&num, COMMA20.&num_decimals)));
	RUN;
	
	PROC SQL NOPRINT;
		INSERT INTO &table_LibName..table_cells(table_id, row_id, col_id, contents) 
		VALUES("&table_id", "&row_id", "&col_id", "&num");

		%table_FinishInsertSQL(table_cells);
	QUIT;

	OPTION NOTES;
%mend;

/* 
Fill a cell with percentage information.
*/
%macro table_FillCellPct(table_id, row_id, col_id, pct, num_decimals = 1, multiply=0);
	%LET table_id = %UPCASE(&table_id);
	%LET col_id = %UPCASE(&col_id);
	%LET row_id = %UPCASE(&row_id);

	OPTION NONOTES;

	DATA _NULL_;
		IF &multiply = 1 THEN
			CALL SYMPUT("pct", STRIP(PUT(&pct * 100, 8.&num_decimals)));
		ELSE
			CALL SYMPUT("pct", STRIP(PUT(&pct, 8.&num_decimals)));
	RUN;
	
	PROC SQL NOPRINT;
		INSERT INTO &table_LibName..table_cells(table_id, row_id, col_id, contents) 
		VALUES("&table_id", "&row_id", "&col_id", "&pct.%");

		%table_FinishInsertSQL(table_cells);
	QUIT;

	OPTION NOTES;
%mend;


/* 
Fill a cell with N and percentage information.
*/
%macro table_FillCellNPct(table_id, row_id, col_id, n, pct, num_decimals = 1, multiply=0);

	%LET table_id = %UPCASE(&table_id);
	%LET col_id = %UPCASE(&col_id);
	%LET row_id = %UPCASE(&row_id);

	OPTION NONOTES;

	DATA _NULL_;
		IF &multiply = 1 THEN
			CALL SYMPUT("pct", STRIP(PUT(&pct * 100, 8.&num_decimals)));
		ELSE
			CALL SYMPUT("pct", STRIP(PUT(&pct, 8.&num_decimals)));
		CALL SYMPUT("n", STRIP(PUT(&n, comma12.)));
	RUN;
	
	PROC SQL NOPRINT;
		INSERT INTO &table_LibName..table_cells(table_id, row_id, col_id, contents) 
		VALUES("&table_id", "&row_id", "&col_id", "&n (&pct.%)");

		%table_FinishInsertSQL(table_cells);
	QUIT;

	OPTION NOTES;
%mend;


/* 
Fill a cell with mean and standard deviation.
*/
%macro table_FillCellMeanSD(table_id, row_id, col_id, mean, sd, num_decimals = 1, percentage = 0);

	%LET table_id = %UPCASE(&table_id);
	%LET col_id = %UPCASE(&col_id);
	%LET row_id = %UPCASE(&row_id);

	OPTION NONOTES;

	DATA _NULL_;
		IF &percentage = 1 THEN DO;
			mean = CATS(PUT(&mean * 100, 8.&num_decimals), '%');
			sd = CATS(PUT(&sd * 100, 8.&num_decimals), '%');
		END;
		ELSE DO;
			mean = PUT(&mean, 8.&num_decimals);
			sd = PUT(&sd, 8.&num_decimals);
		END;

		CALL SYMPUT("mean", STRIP(mean));
		CALL SYMPUT("sd", STRIP(sd));
	RUN;
	
	PROC SQL NOPRINT;
		INSERT INTO &table_LibName..table_cells(table_id, row_id, col_id, contents) 
		VALUES("&table_id", "&row_id", "&col_id", "&mean (&sd)");

		%table_FinishInsertSQL(table_cells);
	QUIT;

	OPTION NOTES;
%mend;

/* 
Fill a cell with median and interquartile range.
*/
%macro table_FillCellMedianIQR(table_id, row_id, col_id, median, q1, q3, num_decimals = 1, percentage = 0);

	%LET table_id = %UPCASE(&table_id);
	%LET col_id = %UPCASE(&col_id);
	%LET row_id = %UPCASE(&row_id);

	OPTION NONOTES;

	DATA _NULL_;
		IF &percentage = 1 THEN DO;
			median = CATS(PUT(&median * 100, 8.&num_decimals), '%');
			q1 = CATS(PUT(&q1 * 100, 8.&num_decimals), '%');
			q3 = CATS(PUT(&q3 * 100, 8.&num_decimals), '%');
		END;
		ELSE DO;
			median = PUT(&median, 8.&num_decimals);
			q1 = PUT(&q1, 8.&num_decimals);
			q3 = PUT(&q3, 8.&num_decimals);
		END;

		CALL SYMPUT("median", STRIP(median));
		CALL SYMPUT("q1", STRIP(q1));
		CALL SYMPUT("q3", STRIP(q3));
	RUN;

	PROC SQL NOPRINT;
		INSERT INTO &table_LibName..table_cells(table_id, row_id, col_id, contents) 
		VALUES("&table_id", "&row_id", "&col_id", "&median [&q1-&q3]");

		%table_FinishInsertSQL(table_cells);
	QUIT;

	OPTION NOTES;
%mend;

/* 
Fill a cell with an estimate and a confidence interval.
*/
%macro table_FillCellEstCI(table_id, row_id, col_id, est, ci_low, ci_high, num_decimals = 2);

	%LET table_id = %UPCASE(&table_id);
	%LET col_id = %UPCASE(&col_id);
	%LET row_id = %UPCASE(&row_id);

	OPTION NONOTES;

	DATA _NULL_;
		CALL SYMPUT("est", STRIP(PUT(&est, 8.&num_decimals)));
		CALL SYMPUT("ci_low", STRIP(PUT(&ci_low, 8.&num_decimals)));
		CALL SYMPUT("ci_high", STRIP(PUT(&ci_high, 8.&num_decimals)));
	RUN;
	
	PROC SQL NOPRINT;
		INSERT INTO &table_LibName..table_cells(table_id, row_id, col_id, contents) 
		VALUES("&table_id", "&row_id", "&col_id", "&est [&ci_low, &ci_high]");

		%table_FinishInsertSQL(table_cells);
	QUIT;

	OPTION NOTES;
%mend;

/*
Fill a cell with an odds (hazard, rate) ratio and a confidence interval.
*/
%macro table_FillCellOR(table_id, row_id, col_id, est, ci_low, ci_high, num_decimals = 2);

	%LET table_id = %UPCASE(&table_id);
	%LET col_id = %UPCASE(&col_id);
	%LET row_id = %UPCASE(&row_id);

	%table_FillCellEstCI(&table_id, &row_id, &col_id, &est, &ci_low, &ci_high, num_decimals=&num_decimals);
%mend;

/*
Fill a cell with a risk (rate) difference and a confidence interval.
*/
%macro table_FillCellRD(table_id, row_id, col_id, est, ci_low, ci_high, mult_100 = 1, num_decimals = 2);

	%LET table_id = %UPCASE(&table_id);
	%LET col_id = %UPCASE(&col_id);
	%LET row_id = %UPCASE(&row_id);

	OPTION NONOTES;

	DATA _NULL_;

		%IF &mult_100 = 1 %THEN %DO;
			est = &est * 100;
			ci_low = &ci_low * 100;
			ci_high = &ci_high * 100;
		%END;
		%ELSE %DO;
			est = &est;
			ci_low = &ci_low;
			ci_high = &ci_high;
		%END;

		CALL SYMPUT("est", STRIP(PUT(est, 8.&num_decimals)));
		CALL SYMPUT("ci_low", STRIP(PUT(ci_low, 8.&num_decimals)));
		CALL SYMPUT("ci_high", STRIP(PUT(ci_high, 8.&num_decimals)));
	RUN;

	%table_FillCellEstCI(&table_id, &row_id, &col_id, &est, &ci_low, &ci_high, num_decimals=&num_decimals);

	OPTION NOTES;
%mend;

/*
Fill a cell with a hazard ratio using an ODS OUTPUT table from PHREG.
Pass ODS OUTPUT xxx = phreg_out_ds.
*/
%macro table_FillCellFromPHREG(table_id, row_id, col_id, phreg_out_ds, variable, num_decimals = 2);
	%LET table_id = %UPCASE(&table_id);
	%LET col_id = %UPCASE(&col_id);
	%LET row_id = %UPCASE(&row_id);

	OPTION NONOTES;

	%LET point_est = %QUOTE(-);
	%LET ci_lower = %QUOTE(-);
	%LET ci_upper = %QUOTE(-);

	PROC SQL NOPRINT;
		SELECT HazardRatio, HRLowerCL, HRUpperCL
		INTO :point_est, :ci_lower, :ci_upper
		FROM &phreg_out_ds
		WHERE UPPER(parameter) = UPPER("&variable");
	QUIT;

	%table_FillCellOR(&table_id, &row_id, &col_id, &point_est, &ci_lower, &ci_upper, num_decimals = &num_decimals);

	OPTION NOTES;
%mend;

/*
Fill a cell with an odds ratio using an ODS OUTPUT table from LOGISTIC.
Pass ODS OUTPUT OddsRatios = logistic_out_ds.
*/
%macro table_FillCellFromLOGISTIC(table_id, row_id, col_id, logistic_out_ds, variable, num_decimals = 2);
	%LET table_id = %UPCASE(&table_id);
	%LET col_id = %UPCASE(&col_id);
	%LET row_id = %UPCASE(&row_id);

	OPTION NONOTES;

	%LET point_est = %QUOTE(-);
	%LET ci_lower = %QUOTE(-);
	%LET ci_upper = %QUOTE(-);

	PROC SQL;
		SELECT OddsRatioEst, LowerCL, UpperCL
		INTO :point_est, :ci_lower, :ci_upper
		FROM &logistic_out_ds
		WHERE UPPER(effect) = UPPER("&variable");
	QUIT;

	%table_FillCellOR(&table_id, &row_id, &col_id, &point_est, &ci_lower, &ci_upper, num_decimals = &num_decimals);

	OPTION NOTES;
%mend;

/*
Fill a cell with an estimate using an ODS OUTPUT table from GENMOD.
Pass ODS OUTPUT ParameterEstimates = genmod_out_ds.
Set exponentiate = 1 if the estimate and the CI should be expoentiated before printing.
*/
%macro table_FillCellFromGENMOD(table_id, row_id, col_id, genmod_out_ds, variable, exponentiate = 0, mult_100 = 0, num_decimals = 2);
	%LET table_id = %UPCASE(&table_id);
	%LET col_id = %UPCASE(&col_id);
	%LET row_id = %UPCASE(&row_id);

	OPTION NONOTES;

	%LET point_est = %QUOTE(-);
	%LET ci_lower = %QUOTE(-);
	%LET ci_upper = %QUOTE(-);

	PROC SQL;
		%IF &exponentiate = 1 %THEN %DO;
			SELECT EXP(Estimate), EXP(LowerWaldCL), EXP(UpperWaldCL)
		%END;
		%ELSE %IF &mult_100 = 1 %THEN %DO;
			SELECT Estimate * 100, LowerWaldCL * 100, UpperWaldCL * 100
		%END;
		%ELSE %DO;
			SELECT Estimate, LowerWaldCL, UpperWaldCL
		%END;
		INTO :point_est, :ci_lower, :ci_upper
		FROM &genmod_out_ds
		WHERE UPPER(parameter) = UPPER("&variable");
	QUIT;

	%table_FillCellOR(&table_id, &row_id, &col_id, &point_est, &ci_lower, &ci_upper, num_decimals = &num_decimals);

	OPTION NOTES;
%mend;

/*
Fill a cell with an estimate using an ODS OUTPUT table from SYSLIN.
Pass ODS OUTPUT ParameterEstimates = syslin_out_ds.
*/
%macro table_FillCellFromSYSLIN(table_id, row_id, col_id, syslin_out_ds, variable, 
			mult_100 = 1, num_decimals = 2);
	%LET table_id = %UPCASE(&table_id);
	%LET col_id = %UPCASE(&col_id);
	%LET row_id = %UPCASE(&row_id);

	OPTION NONOTES;

	%LET point_est = %QUOTE(-);
	%LET ci_lower = %QUOTE(-);
	%LET ci_upper = %QUOTE(-);

	PROC SQL;
		SELECT Estimate, Estimate - 1.96*StdErr, Estimate + 1.96*StdErr
		INTO :point_est, :ci_lower, :ci_upper
		FROM &syslin_out_ds
		WHERE UPPER(Variable) = UPPER("&variable");
	QUIT;

	%table_FillCellRD(&table_id, &row_id, &col_id, &point_est, &ci_lower, &ci_upper, mult_100 = &mult_100, num_decimals = &num_decimals);

	OPTION NOTES;
%mend;


/*
Fill a cell with an estimate using an ODS OUTPUT table from MIXED.
Pass ODS OUTPUT SolutionF = mixed_out_ds;.
*/
%macro table_FillCellFromMIXED(table_id, row_id, col_id, mixed_out_ds, variable, 
			mult_100 = 1, num_decimals = 2);
	%LET table_id = %UPCASE(&table_id);
	%LET col_id = %UPCASE(&col_id);
	%LET row_id = %UPCASE(&row_id);

	OPTION NONOTES;

	%LET point_est = %QUOTE(-);
	%LET ci_lower = %QUOTE(-);
	%LET ci_upper = %QUOTE(-);

	PROC SQL;
		SELECT Estimate, Estimate - 1.96*StdErr, Estimate + 1.96*StdErr
		INTO :point_est, :ci_lower, :ci_upper
		FROM &mixed_out_ds
		WHERE UPPER(Effect) = UPPER("&variable");
	QUIT;

	%table_FillCellRD(&table_id, &row_id, &col_id, &point_est, &ci_lower, &ci_upper, mult_100 = &mult_100, num_decimals = &num_decimals);

	OPTION NOTES;
%mend;

/*
Fill a cell with a median and IQR using ODS output from UNIVARIATE.
Pass ODS OUTPUT Quantiles = univariate_out_ds.
*/
%macro table_FillCellMedianFromUNIVAR(table_id, row_id, col_id, univariate_out_ds, variable, num_decimals = 0, percentage = 0);
	%LET table_id = %UPCASE(&table_id);
	%LET col_id = %UPCASE(&col_id);
	%LET row_id = %UPCASE(&row_id);

	OPTION NONOTES;

	%LET median = %QUOTE(-);
	%LET q1 = %QUOTE(-);
	%LET q3 = %QUOTE(-);

	PROC SQL NOPRINT;
		SELECT estimate
		INTO :median
		FROM &univariate_out_ds
		WHERE UPPER(varname) = UPPER("&variable") and quantile = "50% Median";

		SELECT estimate
		INTO :q1
		FROM &univariate_out_ds
		WHERE UPPER(varname) = UPPER("&variable") and quantile = "25% Q1";

		SELECT estimate
		INTO :q3
		FROM &univariate_out_ds
		WHERE UPPER(varname) = UPPER("&variable") and quantile = "75% Q3";
	QUIT;

	%table_FillCellMedianIQR(&table_id, &row_id, &col_id, &median, &q1, &q3, num_decimals = &num_decimals, percentage = &percentage);

	OPTION NOTES;
%mend;

/*
Fill a cell with a mean and SD using ODS output from UNIVARIATE.
Pass ODS OUTPUT Moments = univariate_out_ds.
*/
%macro table_FillCellMeanFromUNIVAR(table_id, row_id, col_id, univariate_out_ds, variable, num_decimals = 2, percentage = 0);
	%LET table_id = %UPCASE(&table_id);
	%LET col_id = %UPCASE(&col_id);
	%LET row_id = %UPCASE(&row_id);

	OPTION NONOTES;

	%LET mean = %QUOTE(-);
	%LET sd = %QUOTE(-);

	PROC SQL NOPRINT;
		SELECT nValue1
		INTO :mean
		FROM &univariate_out_ds
		WHERE UPPER(varname) = UPPER("&variable") and Label1 = "Mean";

		SELECT nValue1
		INTO :sd
		FROM &univariate_out_ds
		WHERE UPPER(varname) = UPPER("&variable") and Label1 = "Std Deviation";
	QUIT;

	%table_FillCellMeanSD(&table_id, &row_id, &col_id, &mean, &sd, num_decimals = &num_decimals, percentage = &percentage);

	OPTION NOTES;
%mend;



/* fill all cells specified by the row and column ids with numbers from FREQ */
%macro table_FillMultiMeansFromUNIVAR(table_id, row_id, col_id, ds=,  
			percentage = 0, num_decimals = 1, DEBUG = 0);
* Table ID should match Table ID from excel file;
* col_id is list of columns (indicator variables) in Table ID,;
*        variable names are separated by spaces;
* row_id is list of rows (indicator variables) in Table ID,;
*        variable names are separated by spaces;
* ds is the dataset with both col_id and row_id indicator variables;

  %LET table_id = %UPCASE(&table_id);
  %LET col_id = %UPCASE(&col_id);
  %LET row_id = %UPCASE(&row_id);

  * Count up the number of column and row variables by looking for the spaces in between;
  %let totcvar = %sysfunc(countw(&col_id," "));
  %let totrvar = %sysfunc(countw(&row_id," "));

  * Loop through each of the column variables;
  %do numcvar = 1 %to &totcvar;
    * Set cvar as the current column variable;
    %let cvar = %scan(&col_id, &numcvar);
    
      ODS OUTPUT Moments = univariate_out_ds;
	  PROC UNIVARIATE DATA=&ds;
		 VAR &row_id;
		 WHERE &cvar = 1;
	  RUN; 
    
    * Loop through the row variables;
    %do numrvar = 1 %to &totrvar;
        * Set rvar as the current row variable;
        %let rvar = %scan(&row_id, &numrvar);
	     %table_FillCellMeanFromUNIVAR(&table_id, &rvar, &cvar, univariate_out_ds, 
	     			&rvar, num_decimals = &num_decimals, percentage = &percentage);
	%end;
  %end;
%mend;




/* fill all cells specified by the row and column ids with numbers from FREQ */
%macro table_FillMultiMediansFromUNIVAR(table_id, row_id, col_id, ds=,  
			DEBUG = 0);
* Table ID should match Table ID from excel file;
* col_id is list of columns (indicator variables) in Table ID,;
*        variable names are separated by spaces;
* row_id is list of rows (indicator variables) in Table ID,;
*        variable names are separated by spaces;
* ds is the dataset with both col_id and row_id indicator variables;

  %LET table_id = %UPCASE(&table_id);
  %LET col_id = %UPCASE(&col_id);
  %LET row_id = %UPCASE(&row_id);

  * Count up the number of column and row variables by looking for the spaces in between;
  %let totcvar = %sysfunc(countw(&col_id," "));
  %let totrvar = %sysfunc(countw(&row_id," "));

  * Loop through each of the column variables;
  %do numcvar = 1 %to &totcvar;
    * Set cvar as the current column variable;
    %let cvar = %scan(&col_id, &numcvar);
    
      ODS OUTPUT Quantiles = univariate_out_ds;
	  PROC UNIVARIATE DATA=&ds;
		 VAR &row_id;
		 WHERE &cvar = 1;
	  RUN; 
    
    * Loop through the row variables;
    %do numrvar = 1 %to &totrvar;
        * Set rvar as the current row variable;
        %let rvar = %scan(&row_id, &numrvar);
	     %table_FillCellMedianFromUNIVAR(&table_id, &rvar, &cvar, univariate_out_ds, 
	     			&rvar);
	%end;
  %end;
%mend;

/*
Fill a cell with a median and IQR using ODS output from MEANS.
Run PROC MEANS with the MEDIAN, Q1, and Q3 options.  Pass ODS OUTPUT Summary = means_out_ds.
*/
%macro table_FillCellMedianFromMEANS(table_id, row_id, col_id, means_out_ds, variable, num_decimals = 2, percentage = 0);

	%LET table_id = %UPCASE(&table_id);
	%LET col_id = %UPCASE(&col_id);
	%LET row_id = %UPCASE(&row_id);

	OPTION NONOTES;

	%LET median = %QUOTE(-);
	%LET q1 = %QUOTE(-);
	%LET q3 = %QUOTE(-);

	PROC SQL NOPRINT;
		SELECT &variable._median, &variable._q1, &variable._q3 
		INTO :median, :q1, :q3
		FROM &means_out_ds;
	QUIT;

	%table_FillCellMedianIQR(&table_id, &row_id, &col_id, &median, &q1, &q3, num_decimals = &num_decimals, percentage = &percentage);

	OPTION NOTES;
%mend;

/*
Fill a cell with a mean and SD using ODS output from MEANS.
Pass ODS OUTPUT Summary = means_out_ds.
*/
%macro table_FillCellMeanFromMEANS(table_id, row_id, col_id, means_out_ds, variable, num_decimals = 2, percentage = 0);

	%LET table_id = %UPCASE(&table_id);
	%LET col_id = %UPCASE(&col_id);
	%LET row_id = %UPCASE(&row_id);

	OPTION NONOTES;

	%LET mean = %QUOTE(-);
	%LET sd = %QUOTE(-);

	PROC SQL NOPRINT;
		SELECT &variable._mean, &variable._stddev 
		INTO :mean, :sd
		FROM &means_out_ds;
	QUIT;

	%table_FillCellMeanSD(&table_id, &row_id, &col_id, &mean, &sd, num_decimals = &num_decimals, percentage = &percentage);

	OPTION NOTES;
%mend;


/*
Fill a cell with a number using ODS output from FREQ.
Pass ODS OUTPUT CrossTabFreqs = freq_out_ds.
*/
%macro table_FillCellNumFromFREQ(table_id, row_id, col_id, freq_out_ds, where=(1=1), comma=1);
	%LET table_id = %UPCASE(&table_id);
	%LET col_id = %UPCASE(&col_id);
	%LET row_id = %UPCASE(&row_id);

	OPTION NONOTES;

	%LET n = %QUOTE(-);

	PROC SQL NOPRINT;
		SELECT frequency
		INTO :n
		FROM &freq_out_ds
		WHERE &where;
	QUIT;

	%IF &comma = 1 %THEN %DO;
		%table_FillCellCommaNum(&table_id, &row_id, &col_id, &n, num_decimals = 0);
	%END;
	%ELSE %DO;
		%table_FillCellNum(&table_id, &row_id, &col_id, &n, num_decimals = 0);
	%END;
	
	OPTION NOTES;
%mend;

/*
Fill a cell with a row percentage using ODS output from FREQ.
Pass ODS OUTPUT CrossTabFreqs = freq_out_ds.
*/
%macro table_FillCellRowPctFromFREQ(table_id, row_id, col_id, freq_out_ds, where=(1=1), num_decimals = 2);
	%LET table_id = %UPCASE(&table_id);
	%LET col_id = %UPCASE(&col_id);
	%LET row_id = %UPCASE(&row_id);

	OPTION NONOTES;

	%LET pct = %QUOTE(-);

	PROC SQL NOPRINT;
		SELECT RowPercent
		INTO :pct
		FROM &freq_out_ds
		WHERE &where;
	QUIT;

	%table_FillCellPct(&table_id, &row_id, &col_id, &pct, num_decimals = &num_decimals, multiply = 0);

	OPTION NOTES;
%mend;

/*
Fill a cell with a column percentage using ODS output from FREQ.
Pass ODS OUTPUT CrossTabFreqs = freq_out_ds.
*/
%macro table_FillCellColPctFromFREQ(table_id, row_id, col_id, freq_out_ds, where=(1=1), num_decimals = 2);

	%LET table_id = %UPCASE(&table_id);
	%LET col_id = %UPCASE(&col_id);
	%LET row_id = %UPCASE(&row_id);

	OPTION NONOTES;

	%LET pct = %QUOTE(-);

	PROC SQL NOPRINT;
		SELECT ColPercent
		INTO :pct
		FROM &freq_out_ds
		WHERE &where;
	QUIT;

	%table_FillCellPct(&table_id, &row_id, &col_id, &pct, num_decimals = &num_decimals, multiply = 0);

	OPTION NOTES;
%mend;

/* fill all cells specified by the row and column ids with numbers from FREQ */
%macro table_FillMultiCellsFromFREQ(table_id, row_id, col_id, ds=,  
			include_percent = 0, num_decimals = 1, DEBUG = 0);
* Table ID should match Table ID from excel file;
* col_id is list of columns (indicator variables) in Table ID,;
*        variable names are separated by spaces;
* row_id is list of rows (indicator variables) in Table ID,;
*        variable names are separated by spaces;
* ds is the dataset with both col_id and row_id indicator variables;

  %LET table_id = %UPCASE(&table_id);
  %LET col_id = %UPCASE(&col_id);
  %LET row_id = %UPCASE(&row_id);

  * Step 1: Run cross tabulation of row by col variables;
  proc freq data = &ds;
    table (&row_id)*(&col_id);
    ods output CrossTabFreqs = freq_out_ds;
  run;
  %if &DEBUG %then %do;
    title "DEBUG RUNNING";
    title2 'freq_out_ds';
    proc print data = freq_out_ds; run;
  %end;
  * Count up the number of column and row variables by looking for the spaces in between;
  %let totcvar = %sysfunc(countw(&col_id," "));
  %let totrvar = %sysfunc(countw(&row_id," "));

  * Loop through each of the column variables;
  %do numcvar = 1 %to &totcvar;
    * Set cvar as the current column variable;
    %let cvar = %scan(&col_id, &numcvar);
    * Loop through the row variables;
    %do numrvar = 1 %to &totrvar;
      * Set n as -;
      %LET n = %QUOTE(-);
      * Set rvar as the current row variable;
      %let rvar = %scan(&row_id, &numrvar);
      * Look in the output of the proc freq above;
      * for the N and percent where &rvar and &cvar = 1;
      * set the name as rvar;
      * If the cross tab was 0, the dataset will be empty;
      data freq1;
        length name  $32;
        set freq_out_ds;
          where &cvar = 1 and &rvar = 1;
            name = "&rvar";
          if name ne '';
          keep name frequency colpercent &rvar &cvar;
      run;
    /* Determine the number of observations in the dataset */
    /* if Nobs = 0 then dataset is empty */
    /* If the dataset is empty, the N and percent should be 0 */
    data _null_;
      if NObs=0 then do;
        call symput("ObsCount","0");
      end;
      else call symput("ObsCount",put(NObs,best12.));
      stop;
      set freq1 nobs=NObs;
    run;
    * If the dataset was empty (obscount = 0) then fill in freq1 with zeroes;
    %if &ObsCount = 0 %then %do;
      data freq1;
        name = "&rvar";
        &rvar = 1;
        &cvar = 1;
        frequency = 0;
        colpercent = 0;
        output;
      run;
    %end;
    %if &DEBUG %then %do;
      title "DEBUG RUNNING";
      title2 'Freq1';
      proc print data = freq1; run;
    %end;
    * Fill in N macro with count from freq1;
      PROC SQL NOPRINT;
        SELECT frequency, colpercent
        INTO :n, :pcnt
        FROM freq1
        WHERE (&cvar = 1 and &rvar = 1);
      QUIT;
    %if &DEBUG %then %do;
      title "DEBUG RUNNING";
      data testing;
        TABLE_ID = "&table_id";
        ROW_ID = "&rvar";
        COL_ID = "&cvar";
        ds = "&ds";
        N = "&n";
        PERCENT = "&pcnt";
        output;
      run;
      proc print data = testing; 
       title2 'Testing Output'; 
      run;
      title;
    %end;
    %if &DEBUG = 0 %then %do;
  * Fill in the cell with N;
  	%if &include_percent = 0 %then %do;
      %table_FillCellNum(&table_id, &rvar, &cvar, &n, num_decimals = 0);
    %end;
    %else %do;
      %table_FillCellNPct(&table_id, &rvar, &cvar, &n, &pcnt, num_decimals = &num_decimals);
    %end;
   %end;
    proc datasets nolist;
      delete freq1 testing;
      quit;
    run;

    %end;
  %end;
  proc datasets nolist;
    delete freq_out_ds;
    quit;
  run;
%mend;

/*
Get the value in a cell.  Return the value in the &table_CellValue variable.
*/
%GLOBAL table_CellValue;
%macro table_GetCell(table_id, row_id, col_id);

	%LET table_id = %UPCASE(&table_id);
	%LET col_id = %UPCASE(&col_id);
	%LET row_id = %UPCASE(&row_id);

	OPTION NONOTES;

	PROC SQL NOPRINT;
		SELECT contents
		INTO :table_CellValue
		FROM &table_LibName..table_cells(
		WHERE table_id = "&table_id" AND row_id = "&row_id" AND col_id = "&col_id";
	QUIT;

	OPTION NOTES;
%mend;


%GLOBAL table_Error;

/*
Output all tables to the filepath.
*/
%macro table_Output(filepath);
	%table_OutputAll(&filepath);
%mend;

%macro table_OutputAll(filepath);
	%LET table_Error = 0;

	PROC SORT DATA=&table_LibName..table_def;
		BY table_id sequence DESCENDING ts;
	RUN;

   	PROC SORT DATA=&table_LibName..table_row_def;
		BY table_id sequence DESCENDING ts;
	RUN;

	PROC SORT DATA=&table_LibName..table_col_def;
		BY table_id sequence DESCENDING ts;
	RUN;

	PROC SORT DATA=&table_LibName..table_cells;
		BY table_id sequence DESCENDING ts;
	RUN;

	%LET table_JavaError = 1;

	DATA _NULL_;
		LENGTH workbook_path $ 1024;
		RETAIN workbook_path;
	
		SET &table_LibName..table_def
			&table_LibName..excel_table_def
			&table_LibName..table_row_def
			&table_LibName..table_col_def
			&table_LibName..table_cells
			&table_LibName..table_footnotes
			END=eof INDSNAME=ds;

		IF _n_ = 1 THEN DO;
			DECLARE JavaObj tablemaker("org/drugepi/table/TableCreator");
			tablemaker.exceptionDescribe(1);
		END;

		IF ds EQ UPCASE("&table_LibName..table_def") THEN DO;
			tablemaker.callVoidMethod("addTable", table_id, description);
			tablemaker.callVoidMethod("setRowsTitle", table_id, rows_title);
		END;

		IF ds EQ UPCASE("&table_LibName..excel_table_def") THEN DO;
			workbook_path = STRIP(workbook_path);
			PUT workbook_path=;
			tablemaker.callVoidMethod("createTablesFromWorkbook", workbook_path);
		END;

		IF ds EQ UPCASE("&table_LibName..table_row_def") THEN DO;
			IF (row_type = "N") THEN                                                                 
				tablemaker.callVoidMethod("addRowToTable", table_id, parent_id, row_id, description);
			IF (row_type = "H") THEN
				tablemaker.callVoidMethod("addHeaderRowToTable", table_id, parent_id, row_id, description);
		END;

		IF ds EQ UPCASE("&table_LibName..table_col_def") THEN DO;
			IF (col_type = "N") THEN
				tablemaker.callVoidMethod("addColToTable", table_id, parent_id, col_id, description);
			IF (col_type = "H") THEN
				tablemaker.callVoidMethod("addHeaderColToTable", table_id, parent_id, col_id, description);
		END;

		IF ds EQ UPCASE("&table_LibName..table_cells") THEN DO;
			tablemaker.callVoidMethod("addCellToTable", table_id, row_id, col_id, contents);
		END;
		
		IF ds EQ UPCASE("&table_LibName..table_footnotes") THEN DO;
			PUT table_id=;
			PUT row_id=;
			PUT col_id=;
			PUT symbol=;
			PUT contents=;
			
			tablemaker.callVoidMethod("addFootnoteToTable", table_id, row_id, col_id, symbol, contents);
		END;

		IF eof THEN DO;
			%PUT NOTE: Outputting all tables to &filepath ;

			tablemaker.callVoidMethod("writeAllHtmlToFile", "&filepath");
		END;
		
		rc = tablemaker.ExceptionCheck(e); 
		IF NOT e AND _ERROR_ = 0 THEN DO;
			CALL SYMPUT("table_JavaError", 0);
		END;
	RUN;
	%IF &table_JavaError > 0 %THEN %GOTO EXIT_WITH_ERROR;
		
	%GOTO EXIT_MACRO;
		
	%EXIT_WITH_ERROR: ;
		%LET table_Error = 1;
		%PUT ERROR: Table creation failed.  See the log above for more information.;
	
	%EXIT_MACRO: ; 	
%mend;


%macro table_CreateTablesFromWorkbook(in_filepath, out_filepath);
	%LET table_Error = 0;
	%LET table_JavaError = 1;

	DATA _NULL_;
	SET &table_LibName..table_cells
		END=eof INDSNAME=ds;

		IF _n_ = 1 THEN DO;
			DECLARE JavaObj tablemaker("org/drugepi/table/TableCreator");
			tablemaker.exceptionDescribe(1);
			
			tablemaker.callVoidMethod("createTablesFromWorkbook", "&in_filepath");
		END;

		IF ds EQ UPCASE("&table_LibName..table_cells") THEN DO;
			tablemaker.callVoidMethod("addCellToTable", table_id, row_id, col_id, contents);
		END;
		
		IF eof THEN DO;
			%PUT NOTE: Outputting all tables to &out_filepath ;

			tablemaker.callVoidMethod("writeTablesToWorkbook", "&in_filepath", "&out_filepath");
		END;
		
		rc = tablemaker.ExceptionCheck(e); 
		IF NOT e AND _ERROR_ = 0 THEN DO;
			CALL SYMPUT("table_JavaError", 0);
		END;
	RUN;
	%IF &table_JavaError > 0 %THEN %GOTO EXIT_WITH_ERROR;
		
	%GOTO EXIT_MACRO;
		
	%EXIT_WITH_ERROR: ;
		%LET table_Error = 1;
		%PUT ERROR: Table creation failed.  See the log above for more information.;
	
	%EXIT_MACRO: ; 	
%mend;


