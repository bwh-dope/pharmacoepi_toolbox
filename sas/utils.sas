/*
 * UTILS.SAS
 *
 * Copyright 2008-2010 Brigham and Women's Hospital. 
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



%macro CountWords(s);
	%local count; 
	%let count = 0;
	
	%do %while(%qscan(&list,&count + 1, %str( )) ne %str());
		%let count = %eval(&count + 1); 
	%end;
	
	&count  
%mend CountWords;

%macro transfer_data(source, dest);
	DATA &dest;
		SET &source;
	RUN;
%mend;

%macro do_sort(dataset, vars);
	PROC SORT DATA=&dataset;
		BY &vars;
	RUN;
%mend;

%macro prepare_merge(table1, table2, vars);
	%do_sort(&table1, &vars);
	%do_sort(&table2, &vars);
%mend;

%macro write_quantiles(dataset, vars);
	proc univariate data=&dataset;
		var &vars;
		ods output Quantiles=temp1;
	run;

	data temp2;
		set temp1;

		* chop off the % sign and format the number;
		quantile = trim(left(substr(quantile, 1, index(quantile, "%") - 1)));
		var_name = trim(left(VarName)) || "_qtile_" || quantile;

		call symputx(var_name, estimate, 'global');
	run;

	%put _user_;

	proc datasets;
		delete temp1;
		delete temp2;
	quit;
%mend;


%macro delete_dataset(dsname);
	PROC DATASETS NOLIST NOWARN;
		DELETE &dsname;
	RUN;
%mend;


%macro export_data(dsname, outfile, dbms);
	PROC EXPORT DATA=&dsname
	    OUTFILE="&outfile" 
	    DBMS=&dbms REPLACE;
	RUN;
%mend;

%macro export_tab_delimited(dsname, outfile);
	%export_data(&dsname, &outfile, TAB);
%mend;

%macro export_dbase(dsname, outfile);
	%export_data(&dsname, &outfile, DBF);
%mend;

%macro export_excel(dsname, outfile);
	%export_data(&dsname, &outfile, EXCEL);
%mend;


%macro make_quantiles(dsname, num_groups, var, quantvar, indicator);
	PROC RANK DATA=&dsname GROUPS=&num_groups OUT=&dsname;
		VAR &var;
		RANKS &quantvar;
	RUN;

	/* adjust for numbering the groups from 1 rather than from 0 */
	DATA &dsname;
		SET &dsname;

		&quantvar = &quantvar + 1;

		%IF &indicator = 1 %THEN %DO;
			ARRAY a(1:&num_groups) &quantvar._1 - &quantvar._&num_groups;

			DO array_i = 1 TO &num_groups ;
				a(array_i) = (&quantvar = array_i);
			END;
		%END;
	RUN;
%mend;


%macro make_quantiles_jar(dsname, num_groups, var, quantvar, indicator);
	PROC RANK DATA=&dsname GROUPS=&num_groups OUT=&dsname;
		VAR &var;
		RANKS &quantvar;
	RUN;

	/* adjust for numbering the groups from 1 rather than from 0 */
	DATA &dsname;
		SET &dsname;

		&quantvar = &quantvar + 1;

		%IF &indicator = 1 %THEN %DO;
			ARRAY a(1:&num_groups) &quantvar._1 - &quantvar._&num_groups;

			DO array_i = 1 TO &num_groups ;
				a(array_i) = (&quantvar = array_i);
			END;
		%END;
	RUN;
%mend;


%macro make_indicator_variabes(dataset, var);
	PROC SQL;
		SELECT DISTINCT(&var)
		INTO :cat1 - :cat100
		FROM &dataset
		ORDER BY &var;
	QUIT;

	DATA &dataset;
		SET &dataset;

		LENGTH category_text $100;
		LENGTH category_i 8.;

		ARRAY indicators (&sqlobs) &var._1 - &var._&sqlobs;

		DO i = 1 TO &sqlobs;
			indicators(i) = 0;
		END;

		IF _n_ = 1 THEN DO;
			DECLARE hash lookups;
			lookups = _new_ hash();
		    lookups.defineKey('category_text');
		    lookups.defineData('category_i');
		    lookups.defineDone();
			call missing(category_text, category_i);

			%DO hashi = 1 %TO &sqlobs;
				lookups.add(key: "&&cat&hashi", data: &hashi);
			%END;
		END;

		rc = lookups.find(key: &var);
   		if (rc = 0) then do;
			indicators(category_i) = 1;
		END;

		DROP i category_text category_i;
	RUN;
%mend;


%macro compute_dataset_n(dsname);
	OPTION NONOTES;
	ODS LISTING CLOSE;

	%LOCAL local_dataset_n;
	PROC SQL;
		SELECT COUNT(*) AS dataset_n
		INTO :local_dataset_n
		FROM &dsname;
	QUIT;
	
	DATA _null_;
		LENGTH nstr $ 20;
		
		nstr = "(n=" || STRIP(PUT(&local_dataset_n, COMMA15.)) || ")";
		CALL SYMPUTX("dataset_n", &local_dataset_n, "GLOBAL");
		CALL SYMPUTX("dataset_n_str", nstr, "GLOBAL");
	RUN;


	/*

	PROC CONTENTS data=&dsname;
		ODS OUTPUT Attributes=attrib;
	RUN;

	DATA _null_;
		SET attrib(WHERE=(label2="Observations"));

		CALL SYMPUTX("dataset_n", nvalue2, "GLOBAL");
	RUN;
	*/

	ODS LISTING;
	OPTION NOTES;
%mend;


%macro put_timestamp;
	%put *** %sysfunc(today(), date8.) %sysfunc(time(), time.);
%mend;
