/*
 * HDMACROS.SAS
 *
 * Copyright 2008-2012 Brigham and Women's Hospital. 
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

%GLOBAL hd_Error;

%GLOBAL hd_NumModels;
%LET hd_NumModels = 5;

%GLOBAL hd_NumDims;

%macro hd_PrintBanner;
  %PUT %STR(NOTE: hd Macros Version 2);
  %PUT %STR(NOTE: See http://www.hdpharmacoepi.org for full details);
  %PUT %STR(NOTE: Report bugs to bugs@hdpharmacoepi.org);
  %PUT %STR(NOTE: Copyright 2008-11 Division of Pharmacoepidemiology, Brigham and Womens Hospital);
%mend;

%GLOBAL hd_LibraryExists;
%macro hd_SetLibraryExists(dsname);
  %LET hd_LibraryExists = 1;
  %let T = .;

  %let POS = %index(&dsname, &T);
  %if &pos > 0 %then %let Dir = %substr(&dsname, 1, %eval(&pos-1));
  %else %let Dir = work;

  %if %sysfunc(libref(&Dir)) ^= 0 %then %LET hd_LibraryExists = 0;
%mend;

%macro hd_CheckGeneralConditions;
  %let hd_Error = 0;

  %if %quote(&path_temp_dir) =  %then %do;
    %put ERROR: TEMP DIRECTORY IS MISSING;
    %goto EXIT_WITH_ERROR;
  %end;

  %if %sysfunc(fileexist(&path_temp_dir)) = 0 %then %do;
    %put ERROR: HDPS TEMP DIRECTORY DOES NOT EXIST AT &path_temp_dir ;
    %goto EXIT_WITH_ERROR;
  %end;

  %if %sysfunc(fileexist(&path_jar_file)) = 0 %then %do;
    %put ERROR: HDPS JAR FILE DOES NOT EXIST AT &path_jar_file ;
    %goto EXIT_WITH_ERROR;
  %end;

  /* canoncialize the path of the temp directory */
  %LET hd_JavaError = 1;
  DATA _NULL_;
      LENGTH  new_td $ 500
          path $ 500
          separator $ 1;

      DECLARE JavaObj f("java.io.File", "&path_temp_dir");
    f.exceptionDescribe(1);
      f.callStringMethod("getAbsolutePath", path);
      f.getStaticStringField("separator", separator);
    
    new_td = STRIP(path) || STRIP(separator);
    CALL SYMPUTX("new_td", new_td, "GLOBAL");

    rc = f.ExceptionCheck(e);
    IF NOT e AND _ERROR_ = 0 THEN DO;
      CALL SYMPUT("hd_JavaError", 0);
      f.delete();
    END;
  RUN;
  %IF &hd_JavaError > 0 %THEN %DO;
    %PUT "ERROR: Could not canonicalize the file name &path_temp_dir";
    %GOTO EXIT_WITH_ERROR;
  %END;

  %PUT NOTE: Old temp directory specification was &path_temp_dir;
  %LET path_temp_dir = &new_td;
  %PUT NOTE: New temp directory specification is  &path_temp_dir;

  %if %quote(&path_jar_file) = %then %do;
    %put ERROR: HDPS JAR FILE PATH IS MISSING;
    %goto EXIT_WITH_ERROR;
  %end;
  %hd_InitClasspathUpdate;
  %hd_AddToClasspath(&path_jar_file);

  %if &var_patient_id = %then %do;
    %put ERROR: VAR var_patient_id IS MISSING;
    %goto EXIT_WITH_ERROR;
  %end;

  %if &input_cohort = %then %do;
    %put ERROR: VAR input_cohort IS MISSING;
    %goto EXIT_WITH_ERROR;
  %end;

  %GOTO EXIT_MACRO;
  
  %EXIT_WITH_ERROR: ;
  %LET hd_Error = 1;
  
  %EXIT_MACRO: ; 
%mend;

%macro hd_CheckVarSelectionConditions;
  %let hd_Error = 0;

  %if &input_dim1 = %then %do;
    %put ERROR: NO dimensions are specified, or Dimension 1 is missing ;
    %goto EXIT_WITH_ERROR;
  %end;

  %hd_SetLibraryExists(&output_cohort);
  %IF &hd_LibraryExists = 0 %THEN %DO;
    %PUT ERROR: The output library does not exist ;
    %GOTO EXIT_WITH_ERROR;
  %END;

  %GOTO EXIT_MACRO;

  %EXIT_WITH_ERROR: ;
  %LET hd_Error = 1;
  
  %EXIT_MACRO: ; 
%mend;

%macro hd_CheckEstimationConditions(out_dataset);
  %let hd_Error = 0;

  %hd_SetLibraryExists(&input_cohort);
  %IF &hd_LibraryExists = 0 %THEN %DO;
    %PUT ERROR: The input library does not exist ;
    %GOTO EXIT_WITH_ERROR;
  %END;

  %hd_SetLibraryExists(&output_cohort);
  %IF &hd_LibraryExists = 0 %THEN %DO;
    %PUT ERROR: The output library does not exist ;
    %GOTO EXIT_WITH_ERROR;
  %END;

  %hd_SetLibraryExists(&out_dataset);
  %IF &hd_LibraryExists = 0 %THEN %DO;
    %PUT ERROR: The output library does not exist ;
    %GOTO EXIT_WITH_ERROR;
  %END;

  %hd_SetLibraryExists(&result_estimates);
  %IF &hd_LibraryExists = 0 %THEN %DO;
    %PUT ERROR: The results library does not exist ;
    %GOTO EXIT_WITH_ERROR;
  %END;

  %hd_SetLibraryExists(&result_diagnostic);
  %IF &hd_LibraryExists = 0 %THEN %DO;
    %PUT ERROR: The results library does not exist ;
    %GOTO EXIT_WITH_ERROR;
  %END;
  
  %GOTO EXIT_MACRO;
  
  %EXIT_WITH_ERROR: ;
  %LET hd_Error = 1;
  
  %EXIT_MACRO: ; 
%mend;

%macro hd_ExportFile(dataset_name, dataset);
  %let file_name = "&path_temp_dir.&dataset_name..txt";

  %put NOTE: Exporting &dataset to &file_name;

  OPTION NONOTES;
  PROC EXPORT DATA=&dataset
    OUTFILE=&file_name
    DBMS=TAB REPLACE;
  RUN;
  OPTION NOTES;
%mend;

%macro hd_ExportDimension(dataset, id_field, code_field, date_field);
  %let file_name = &path_temp_dir.hd_&dataset..txt;
  DATA _null_;
    file "&file_name" delimiter='09'x DSD DROPOVER lrecl=32767;
    if _n_ = 1 then do;
      put
        "f1_patient_id" '09'x
        "f2_code" '09'x
        "f3_date"
      ;
      end;
      
    SET &dataset;
      LENGTH  f1_patient_id $255 
           f2_code $255 
           f3_date $3;

      f1_patient_id = put(&id_field, $255.);
      f2_code = &code_field;
      %IF &date_field ^= %THEN %DO;
	      f3_date = &date_field;
	  %END;
	  %ELSE %DO;
	      f3_date = "0";
	  %END;	  

      do;
        put f1_patient_id $ @;
        put f2_code $ @;
        put f3_date ;
      ;
      end;
  RUN;
%mend;

%macro hd_ExportPatients;
  DATA hd_Export;
    SET &input_cohort;

    LENGTH f1_patient_id $255 f2_exposure $10 f3_outcome $10;

    f1_patient_id = &var_patient_id;
    f2_exposure = &var_exposure;
    f3_outcome = &var_outcome;
    %if &var_person_time ^=  %then %let futime_val = &var_person_time;
    %else %let futime_val = 1;
    
    f4_futime = &futime_val ;

    KEEP f1_patient_id f2_exposure f3_outcome f4_futime;
  RUN;

  %hd_ExportFile(hd_patients, hd_Export);
%mend;

%macro hd_OpenMaster;
  %PUT NOTE: Checking master file and initializing variables;

  %let tableid = %sysfunc(open(&input_cohort));
  %if &tableid = 0 %then %do;
    %put ERROR: FILE &input_cohort DOES NOT EXIST;
    %goto EXIT_WITH_ERROR;
  %end;
  
  %let badid = %sysfunc(scan(&var_patient_id, 2));
  %if &badid ^= %THEN %DO;
    %put ERROR: Multiple patient ID variables specified, but only one variable is supported;
    %goto EXIT_WITH_ERROR;
  %end;

  %let vnum = %sysfunc(varnum(&tableid, &var_patient_id)); 
  %if &vnum = 0 %then %do;
    %put ERROR: Patient ID variable &var_patient_id does not exist on cohort table;
    %goto EXIT_WITH_ERROR;
  %end;
  
  %let vtype = %sysfunc(vartype(&tableid, &vnum));
  %if &vtype = N %then %do;
    %put ERROR: Patient ID variable &var_patient_id is numeric, but a character variable is required ;
    %goto EXIT_WITH_ERROR;
  %end;

  %let retval = %sysfunc(close(&tableid));
  
  %if (&selection_mode = LOCAL) %then %do;
    %hd_ExportPatients;
  %end;

  %GOTO EXIT_MACRO;
  
  %EXIT_WITH_ERROR: ;
  %LET hd_Error = 1;
  
  %EXIT_MACRO: ; 
%mend;

%macro hd_InitializeVariableLists;
  /* get a list of all variables on the input cohort */
  ods output Variables = hd_VarsAll;
  proc contents data=&input_cohort;
  run;
  
  data hd_VarsAll;
    set hd_VarsAll(keep=variable type);
    if (type = "Char") then
      c_flag = "C";
    else
      c_flag = "I";

    variable = compress(upcase(compress(variable)) || "|" || c_flag);
  run;
  
  proc sql;
    select variable
    into :hd_VarsAll separated by ' '
    from hd_VarsAll;
  quit;
  
  %let NVarsAll = &sqlobs;
  %put NVarsAll = &NVarsAll;
  
  %LET hd_JavaError = 1;
  DATA _NULL_;
    LENGTH hd_VarsDemogCont $ 16384
         hd_VarsDemogCat $ 16384
         hd_VarsPredefCont $ 16384
         hd_VarsPredefCat $ 16384;
  
      DECLARE JavaObj sh("org/drugepi/hdps/HdpsSASHelper");
    sh.exceptionDescribe(1);
    
    sh.setStringField("inputVarsAll", "&hd_VarsAll");
  
    sh.setStringField("inputVarPatId", "&var_patient_id");
    sh.setStringField("inputVarExposure", "&var_exposure");
    sh.setStringField("inputVarOutcome", "&var_outcome");
    
    sh.setStringField("inputVarsDemographic", "&vars_demographic");
    sh.setStringField("inputVarsPredefined", "&vars_predefined");
    sh.setStringField("inputVarsForceCategorical", "&vars_force_categorical");
    sh.setStringField("inputVarsIgnore", "&vars_ignore");
    
    sh.callVoidMethod("computeVariableLists");

    sh.getStringField("outputVarsDemographicCategorical", hd_VarsDemogCat);
    sh.getStringField("outputVarsDemographicContinuous", hd_VarsDemogCont);
    sh.getStringField("outputVarsPredefCategorical", hd_VarsPredefCat);
    sh.getStringField("outputVarsPredefContinuous", hd_VarsPredefCont);
    
    CALL SYMPUTX("hd_VarsDemogCont", hd_VarsDemogCont, "GLOBAL");
    CALL SYMPUTX("hd_VarsDemogCat", hd_VarsDemogCat, "GLOBAL");
    CALL SYMPUTX("hd_VarsPredefCat", hd_VarsPredefCat, "GLOBAL");
    CALL SYMPUTX("hd_VarsPredefCont", hd_VarsPredefCont, "GLOBAL");
    
    PUT hd_VarsDemogCat=;
    PUT hd_VarsDemogCont=;
    PUT hd_VarsPredefCat=;
    PUT hd_VarsPredefCont=;
    
    rc = sh.ExceptionCheck(e);
    IF NOT e AND _ERROR_ = 0 THEN DO;
      CALL SYMPUT("hd_JavaError", 0);
      sh.delete();
    END;
  RUN;
  %IF &hd_JavaError > 0 %THEN %DO;
    %PUT "ERROR: Could not process variable lists";
    %GOTO EXIT_WITH_ERROR;
  %END;
  
  %if &hd_Error > 0 %then %goto EXIT_WITH_ERROR;
  
  %GOTO EXIT_MACRO;
  
  %EXIT_WITH_ERROR: ;
  %LET hd_Error = 1;
  
  %EXIT_MACRO: ; 
%mend;

%macro hd_OpenDims;
	%PUT NOTE: Checking dimension files and initializing variables;
	
	%let N = 0;
	%let T = .;
	
	%let hd_NumDims = 0;
	
	%do I2 = 1 %to 100;
		%if &&input_dim&I2 ^=  %then %do;
			%let hd_NumDims = %eval(&hd_NumDims + 1);
	
			%PUT input_dim&i2 is &&input_dim&i2 ;
	
			%global Dim&i2;
			%global Field&i2;
			%global DateField&i2;
			%let Dim&I2   = %scan(&&input_dim&I2,1,' ');
			%let Field&I2 = %scan(&&input_dim&I2,2,' ');
			
			%let DateField&I2 = %scan(&&input_dim&I2,3,' ');
			%if (&selection_mode = DB) %then %do;
				/* set date field to 0 to indicate no field.  in db mode, this will */
				/* result in a SELECT 0.  in local mode, a 0 will be exported. */
				%if %trim(&&DateField&I2) =  %then %let DateField&I2 = 0;
			%end;
			
			%let Pos = %index(&&Dim&I2,&T);
			%if &Pos > 0 %then %do;
				%let MemName&I2 = %substr(&&Dim&I2,%eval(&Pos+1));
				%let Type&I2    = %upcase(&&Field&I2.._&&MemName&I2);
				%let Dir&I2     = %upcase(%substr(&&Dim&I2,1,%eval(&pos-1)));
			%end;
			%else %do;
				%let MemName&I2 = &&Dim&I2;
				%let Type&I2    = %upcase(&&Field&I2.._&&Dim&I2);
				%let Dir&I2     = work;
			%end;
		%end;
	%end;
	
	%do I0 = 1 %to &hd_NumDims;
		%if %sysfunc(libref(&&Dir&I0)) ^= 0 %then %do;
			%put ERROR: DIRECTORY &&Dir&I0 DOES NOT EXIST;
			%goto EXIT_WITH_ERROR;
		%end;
	%end;
	
	%if (&selection_mode = LOCAL) %then %do;
		%do L0 = 1 %to &hd_NumDims;
			%let File = %sysfunc(open(&&Dim&L0));
	
			%if &File = 0 %then %do;
				%put ERROR: FILE &&Dim&L0 DOES NOT EXIST;
				%goto EXIT_WITH_ERROR;
			%end;
			%else %do;
				%let nvars = %sysfunc(attrn(&File,nvars));
				%let patient_id_exists = 0;    
				%let dim_field_exists = 0;
				%let date_field_exists = 0;
				%do I1 = 1 %to &NVars;
					%let VarName = %sysfunc(varname(&File,&I1));
					%let VarType = %sysfunc(vartype(&File,&I1));
	
					%if %upcase(&VarName) = %upcase(&var_patient_id) %then 
						%let patient_id_exists = 1;
	
					%if %upcase(&VarName) = %upcase(&&Field&L0) %then
						%let dim_field_exists = 1;
	
					%if %upcase(&VarName) = %upcase(&&DateField&L0) %then
						%let date_field_exists = 1;
				%end;
	
				/*  
				%let Flag1 = %eval(&Flag1+1);
				%global TypeField&L0;
				%let TypeField&L0 = &VarType;
				%end;
				*/
			%end;
	
			%if &patient_id_exists = 0 %then %do;
				%put "VAR &var_patient_id DOES NOT EXIST IN FILE &&Dim&L0";
				%let hd_Error = 1;
			%end;
	
			%if &dim_field_exists = 0 %then %do;
				%put "VAR &&Field&L0 DOES NOT EXIST IN FILE &&Dim&L0";
				%let hd_Error = 1;
			%end;
	
			/* set to a constant 0 if the field doesn't exist */
			%if &date_field_exists = 0 %then %let DateField&L0 = 0;
	
			%let rc  = %sysfunc(close(&File));

			%hd_ExportDimension(&&Dim&L0, &var_patient_id, &&Field&L0, &&DateField&L0);
		%end;
	%end;

	%GOTO EXIT_MACRO;
	
	%EXIT_WITH_ERROR: ;
	%LET hd_Error = 1;
	
	%EXIT_MACRO: ; 
%mend;

%macro hd_InputVarsFile(in_filename, out_dataset);
  data &out_dataset;
    infile "&in_filename" delimiter='09'x MISSOVER DSD lrecl=32767 firstobs=2 ;

    informat dimension $18. ;
    informat code_id $40. ;
    informat var_name $17. ;
    informat selected_for_ps $6. ;
    informat e1 best32. ;
    informat e0 best32. ;
    informat pt_e1 best32. ;
    informat pt_e0 best32. ;
    informat d1 best32. ;
    informat d0 best32. ;
    informat c1 best32. ;
    informat c0 best32. ;
    informat pt_c1 best32. ;
    informat pt_c0 best32. ;
    informat e1c1 best32. ;
    informat e1c0 best32. ;
    informat e0c1 best32. ;
    informat e0c0 best32. ;
    informat d1c1 best32. ;
    informat d1c0 best32. ;
    informat d0c1 best32. ;
    informat d0c0 best32. ;
    informat pc_e1 best32. ;
    informat pc_e0 best32. ;
    informat num_events best32. ;
    informat c1_num_events best32. ;
    informat c0_num_events best32. ;
    informat mean_outcome best32. ;
    informat c1_mean_outcome best32. ;
    informat c0_mean_outcome best32. ;
    informat rr_ce best32. ;
    informat rr_cd best32. ;
    informat bias best32. ;
    informat exp_assoc_ranking_var best32. ;
    informat outcome_assoc_ranking_var best32. ;
    informat bias_ranking_var best32. ;
    informat z_bias_score best32. ;
    informat hash_value $255. ;

    input
      dimension $
      code_id $
      var_name $
      selected_for_ps $
      e1
      e0
      pt_e1
      pt_e0
      d1
      d0
      c1
      c0
      pt_c1
      pt_c0
      e1c1
      e1c0
      e0c1
      e0c0
      d1c1
      d1c0
      d0c1
      d0c0
      pc_e1
      pc_e0
      num_events
      c1_num_events
      c0_num_events
      mean_outcome
      c1_mean_outcome
      c0_mean_outcome
      rr_ce
      rr_cd
      bias
      exp_assoc_ranking_var
      outcome_assoc_ranking_var
      bias_ranking_var
      z_bias_score
      hash_value $
    ;
  run;
%mend;

%macro hd_InputDimFile(in_filename, out_dataset);
  data &out_dataset;                                    ;
    infile "&in_filename" delimiter='09'x MISSOVER DSD lrecl=32767 firstobs=2 ;
    informat dim_num best32.;
    informat dim_name $100. ;
    informat code_id $44. ;
    informat num_patients best32. ;
    informat consider_for_selection $6. ;
    informat prevalence best32. ;
    informat median best32. ;
    informat q3 best32. ;
    
    input
      dim_num 
      dim_name $
      code_id $
      num_patients
      consider_for_selection $
      prevalence
      median
      q3
    ;
  run;
%mend;

%macro hd_InputCohort(in_filename, out_dataset, var_dataset);
  PROC SQL NOPRINT;
    SELECT var_name
    INTO :var_name_1 - :var_name_10000
    FROM &var_dataset
    WHERE UPPER(selected_for_ps) = 'TRUE';
  QUIT;
  
  %LET num_hd_vars = &sqlobs;
  
  data &out_dataset;                                    ;
    infile "&in_filename" delimiter='09'x MISSOVER DSD lrecl=32767 firstobs=2 ;

    informat patient_id $255. ;

    %do var_i = 1 %to &num_hd_vars;
      informat &&var_name_&var_i 8.;
    %end;
    
    input
      patient_id $

      %do var_i = 1 %to &num_hd_vars;
        &&var_name_&var_i 
      %end;
    ;
  run;
%mend;


%macro hd_RunVarSelection;
  %LET hd_JavaError = 1;
  DATA _null_;
    DECLARE JavaObj hdps("org/drugepi/hdps/Hdps");

    put "NOTE: hd-PS internal program beginning.  This may take some time.";
  
    hdps.exceptionDescribe(1);
  
    hdps.setIntField("topN", &top_n);
    hdps.setIntField("k", &hd_k);
    hdps.setIntField("frequencyMin", &frequency_min);
    hdps.setStringField("tempDirectory", "&path_temp_dir");
    hdps.setStringField("variableRankingMethod", "&ranking_method");
    hdps.setStringField("outcomeType", "&outcome_type");
    hdps.setStringField("fullOutputFilename", "output_full_cohort.txt");

    hdps.setIntField("useOutcomeZeroCellCorrection", &outcome_zero_cell_corr);
    hdps.setIntField("inferServiceIntensityVars", &infer_service_intensity_vars);
    hdps.setIntField("createTimeInteractions", &create_time_interactions);
    hdps.setIntField("createProfileScores", &create_profile_scores);
    hdps.setIntField("dbKeepOutputTables", &db_keep_output_tables);
    
    /* !!! hack!  for hd-DRS.  not supported. */
    %if %symexist(hdps_Requested_Variables) %then %do;
		%let hdps_max_requested_i = %sysfunc(countw(&hdps_Requested_Variables));

    	%do requested_i = 1 %to &hdps_max_requested_i;
	    	%let hdps_Requested_Variable = %scan(&hdps_Requested_Variables, &requested_i);
    		hdps.callVoidMethod("addRequestedVariable", "&hdps_Requested_Variable");
    	%end;
    %end;

    %if &selection_mode = LOCAL %then %do;
      hdps.callVoidMethod("setMode", "LOCAL");
      hdps.callVoidMethod("addPatients", "&path_temp_dir.hd_patients.txt");
      
      %do i = 1 %to &hd_NumDims;
        hdps.callVoidMethod("addDimension", "&&Dim&I", "&path_temp_dir.hd_&&Dim&i...txt");
      %end;
    %end;
    %else %do;
      hdps.callVoidMethod("setMode", "DB");
      hdps.setStringField("dbDriverClass", "&db_driver_class");
      hdps.setStringField("dbUrl", "&db_url");
      hdps.setStringField("dbUsername", "&db_username");
      hdps.setStringField("dbPassword", "&db_password");
      
      hdps.callVoidMethod("addPatients", "&db_driver_class", "&db_url", "&db_username", "&db_password", 
                "SELECT &var_patient_id, &var_exposure, &var_outcome FROM &input_cohort_table");

      %do i = 1 %to &hd_NumDims;
      	%if &&DateField&i = 0 %then %do;
			hdps.callVoidMethod("addDimension", "&&Dim&i", "&db_driver_class", "&db_url", "&db_username", "&db_password", 
					  "SELECT &var_patient_id, &&Field&i FROM &&Dim&i");
		%end;
		%else %do;
			hdps.callVoidMethod("addDimension", "&&Dim&i", "&db_driver_class", "&db_url", "&db_username", "&db_password", 
					  "SELECT &var_patient_id, &&Field&i, &&DateField&i FROM &&Dim&i");
		%end;
	  %end;
    %end;

    hdps.callVoidMethod("run");
    
    rc = hdps.ExceptionCheck(e);
    IF NOT e AND _ERROR_ = 0 THEN DO;
      CALL SYMPUT("hd_JavaError", 0);
      hdps.delete();
    END;
  RUN;
  %IF &hd_JavaError > 0 %THEN %GOTO EXIT_WITH_ERROR;

  /* read in the variables files */
  %hd_InputVarsFile(&path_temp_dir.output_all_vars.txt, &result_diagnostic._all_vars);

  /* using the variables file, make the cohort */
  %hd_InputCohort(&path_temp_dir.output_full_cohort.txt, work.hd_cohort, &result_diagnostic._all_vars);
  
  /* read in the dimensions file */ 
  %hd_InputDimFile(&path_temp_dir.output_dimension_codes.txt, &result_diagnostic._dim_codes);

  PROC SORT DATA=&input_cohort;
    BY &var_patient_id;
  RUN;

  DATA &output_cohort;
    MERGE &input_cohort
           work.hd_cohort(RENAME=(patient_id=&var_patient_id));
    BY &var_patient_id;
  RUN;
  
  %GOTO EXIT_MACRO;
  
  %EXIT_WITH_ERROR: ;
  %LET hd_Error = 1;
  
  %EXIT_MACRO: ; 
%mend;

%macro hd_InitResults;
  PROC SQL NOPRINT;
    CREATE TABLE results_field_info (
      field_name    char(50),
      field_order   int,
      field_type    char(1)
    );    
  
    INSERT INTO results_field_info VALUES("model_number",   1,    "N"); 
    INSERT INTO results_field_info VALUES("model_name",     2,    "C");
    INSERT INTO results_field_info VALUES("score_var",      3,    "C");
    INSERT INTO results_field_info VALUES("c_stat",       8,    "N");
    INSERT INTO results_field_info VALUES("score_model_converged",    9,    "N");
    INSERT INTO results_field_info VALUES("deciles_n",      14,   "N");
    INSERT INTO results_field_info VALUES("deciles_converged",  13,   "N");
    INSERT INTO results_field_info VALUES("deciles_est",    10,   "N");
    INSERT INTO results_field_info VALUES("deciles_ci_lower", 11,   "N");
    INSERT INTO results_field_info VALUES("deciles_ci_upper", 12,   "N");
    INSERT INTO results_field_info VALUES("deciles_trim_n",      19,   "N");
    INSERT INTO results_field_info VALUES("deciles_trim_converged",  18,   "N");
    INSERT INTO results_field_info VALUES("deciles_trim_est",    15,   "N");
    INSERT INTO results_field_info VALUES("deciles_trim_ci_lower", 16,   "N");
    INSERT INTO results_field_info VALUES("deciles_trim_ci_upper", 17,   "N");
    INSERT INTO results_field_info VALUES("matched_n",    24,   "N");
    INSERT INTO results_field_info VALUES("matched_converged",  23,   "N");
    INSERT INTO results_field_info VALUES("matched_est",    20,   "N");
    INSERT INTO results_field_info VALUES("matched_ci_lower", 21,   "N");
    INSERT INTO results_field_info VALUES("matched_ci_upper", 22,   "N");
    
    DELETE FROM results_field_info WHERE field_name IS NULL;
    
    CREATE TABLE results_store (
      model_number  int,
      score_var   char(20),
      field_name    char(50),
      value_c     char(100),
      value_n     int
    );
  QUIT;
%mend;

%macro hd_InitScoreResults(score_var);
  %hd_AddResult(1, %UPCASE(&score_var), model_name, %QUOTE(Unadjusted), );
  %hd_AddResult(2, %UPCASE(&score_var), model_name, %QUOTE(Demographics), );
  %hd_AddResult(3, %UPCASE(&score_var), model_name, %QUOTE(Demographics + Predefined), );
  %hd_AddResult(4, %UPCASE(&score_var), model_name, %QUOTE(Demographics + Predefined + Empirical), );
  %hd_AddResult(5, %UPCASE(&score_var), model_name, %QUOTE(Demographics + Empirical), );
%mend;

%macro hd_AddResult(model_number, score_var, field_name, value_c, value_n);
  *OPTION NONOTES NOSOURCE NOSOURCE2;
  
  %IF &value_c =  %THEN %DO;
    %LET value_c = NULL;
  %END;
  %ELSE %DO;
    %LET value_c = "&value_c";
  %END; 

  %IF "&value_n" = "%QUOTE(<0.001)" %THEN %DO;
    %LET value_n = 0;
  %END;
  %ELSE %IF "&value_n" = "%QUOTE(>999.999)" %THEN %DO;
    %LET value_n = 1000;
  %END;
  %ELSE %IF &value_n = %THEN %DO;
    %LET value_n = -999;
  %END;
  %ELSE %IF &value_n = . %THEN %DO;
    %LET value_n = -999;
  %END; 
  
  PROC SQL NOPRINT;
    INSERT INTO results_store(model_number, score_var, field_name, value_c, value_n)
    VALUES(&model_number, "%UPCASE(&score_var)", "&field_name", &value_c, &value_n);
  QUIT;
  
  OPTION NOTES SOURCE SOURCE2;
%mend;


%macro hd_AddPSModelResult(model_number, score_var, dataset_c, dataset_conv);
    *OPTION NONOTES;

  PROC SQL NOPRINT;
    SELECT PUT(nValue2, 32.8)
    INTO :c_stat
    FROM &dataset_c
    WHERE UPCASE(Label2) = "C";
    
    SELECT (1 - status)
    INTO :converged
    FROM &dataset_conv;
  QUIT;
    %hd_AddResult(&model_number, %UPCASE(&score_var), c_stat, , &c_stat);
    %hd_AddResult(&model_number, %UPCASE(&score_var), score_model_converged, , &converged);

  OPTION NOTES;
%mend;


%macro hd_AddRegressionResult(dataset_cohort, procedure, model_number, score_var, est_name, dataset_est, dataset_conv);
    OPTION NONOTES;

	PROC SQL;
		SELECT COUNT(*) 
		INTO :n_patients
		FROM &dataset_cohort;
	QUIT;

  %IF %UPCASE(&procedure) = LOGISTIC %THEN %DO;
    DATA t_or;
	  SET &dataset_est;
		
	  point_est = exp(Estimate);
	  ci_lower = exp(Estimate - 1.96*StdErr);
	  ci_upper = exp(Estimate + 1.96*StdErr);
	  WHERE UPCASE(variable) = UPCASE("&var_exposure");
    RUN;
  %END;

  PROC SQL NOPRINT;
  	/*
  	--- DEPRECATED
    %IF %UPCASE(&procedure) = LOGISTIC %THEN %DO;
      SELECT PUT(OddsRatioEst, 32.8), PUT(LowerCL, 32.8), PUT(UpperCL, 32.8)
      INTO :point_est, :ci_lower, :ci_upper
      FROM &dataset_est
      WHERE UPCASE(effect) = UPCASE("&var_exposure");
      
    %END;
    */
    %IF %UPCASE(&procedure) = LOGISTIC %THEN %DO;
      SELECT PUT(point_est, 32.8), PUT(ci_lower, 32.8), PUT(ci_upper, 32.8)
      INTO :point_est, :ci_lower, :ci_upper
      FROM t_or;
    %END;
    %ELSE %IF %UPCASE(&procedure) = PHREG %THEN %DO;
      SELECT PUT(HazardRatio, 32.8), PUT(HRLowerCL, 32.8), PUT(HRUpperCL, 32.8)
      INTO :point_est, :ci_lower, :ci_upper
      FROM &dataset_est
      WHERE UPCASE(parameter) = UPCASE("&var_exposure");
    %END;
  
    SELECT (1 - status)
    INTO :converged
    FROM &dataset_conv;
  QUIT;
    %hd_AddResult(&model_number, %UPCASE(&score_var), &est_name._n, , &n_patients);
    %hd_AddResult(&model_number, %UPCASE(&score_var), &est_name._converged, , &converged);
    %hd_AddResult(&model_number, %UPCASE(&score_var), &est_name._est, , &point_est);
    %hd_AddResult(&model_number, %UPCASE(&score_var), &est_name._ci_lower, , &ci_lower);
    %hd_AddResult(&model_number, %UPCASE(&score_var), &est_name._ci_upper, , &ci_upper);
    
    OPTION NOTES;
%mend;

%global hd_VarsSelected;

%macro hd_RunSinglePSModel(model_num, class_vars, indep_vars);
  %put NOTE: Running PS model &model_num;
  ods output Association = t_c;
  ods output ConvergenceStatus = t_converge;
  proc logistic data = &output_cohort(keep=&var_patient_id &var_exposure &class_vars &indep_vars) descending;
    class &class_vars;
    model &var_exposure = &indep_vars;
    output out = hd_&model_num(keep=&var_patient_id ps&model_num) pred=ps&model_num;
  run;
  %hd_AddPSModelResult(&model_num, ps, t_c, t_converge);
  
  PROC SORT DATA=hd_&model_num;
    BY &var_patient_id;
  RUN;
%mend;

%macro hd_EstimateHDPS;
  %PUT NOTE: Running hd-PS models;

  %if %sysfunc(exist(&result_diagnostic._all_vars)) = 0 %then %do;
    %let hd_VarsSelected = ;
  %end;
  %else %do;
    proc sql noprint;
      select var_name
      into :hd_VarsSelected separated by ' ' 
      from &result_diagnostic._all_vars
      where UPPER(selected_for_ps) = 'TRUE';
    quit;
  %end;

  %PUT NOTE: The empirical variables being entered into the hd-PS are ;
  %PUT &hd_VarsSelected ;
  
  %if &score_type_2 = 1 %then %do;
    %hd_RunSinglePSModel(2, &hd_VarsDemogCat, 
                  &hd_VarsDemogCat &hd_VarsDemogCont)
  %end;

  %if &score_type_3 = 1 %then %do;
    %hd_RunSinglePSModel(3, &hd_VarsDemogCat &hd_VarsPredefCat, 
                  &hd_VarsDemogCat &hd_VarsDemogCont 
                  &hd_VarsPredefCat &hd_VarsPredefCont)
  %end;
  
  %if &score_type_4 = 1 %then %do;
    %hd_RunSinglePSModel(4, &hd_VarsDemogCat &hd_VarsPredefCat, 
                  &hd_VarsDemogCat &hd_VarsDemogCont 
                  &hd_VarsPredefCat &hd_VarsPredefCont &hd_VarsSelected)
  %end;

  %if &score_type_5 = 1 %then %do;
    %hd_RunSinglePSModel(5, &hd_VarsDemogCat, 
                  &hd_VarsDemogCat &hd_VarsDemogCont 
                  &hd_VarsSelected)
  %end;
  
  DATA &output_cohort_scored;
    MERGE &output_cohort
    %DO ps_i = 2 %TO &hd_NumModels;
      %IF &&score_type_&ps_i = 1 %THEN %DO;
        hd_&ps_i
      %END;
    %END;
    ;
    BY &var_patient_id;
  RUN;
%mend;

%macro hd_RunSingleDRSModel(model_num, class_vars, indep_vars);
  %put NOTE: Running DRS model &model_num;
  ods output Association = t_c;
  ods output ConvergenceStatus = t_converge;

  proc logistic data = &output_cohort(keep=&var_patient_id &var_exposure &var_outcome &class_vars &indep_vars) descending;
    class &class_vars;
    model &var_outcome = &indep_vars;
    where &var_exposure = 0;
    score data=&output_cohort out=hddrs_&model_num(rename=(p_1=drs&model_num) keep=&var_patient_id p_1);
  run;
  %hd_AddPSModelResult(&model_num, drs, t_c, t_converge);

  PROC SORT DATA=hddrs_&model_num;
    BY &var_patient_id;
  RUN;
%mend;

%macro hd_EstimateHDDRS;
  %PUT NOTE: Running hd-DRS models;

  %if %sysfunc(exist(&result_diagnostic._all_vars)) = 0 %then %do;
    %let hd_VarsSelected = ;
  %end;
  %else %do;
    proc sql noprint;
      select var_name
      into :hd_VarsSelected separated by ' ' 
      from &result_diagnostic._all_vars
      where UPPER(selected_for_ps) = 'TRUE';
    quit;
  %end;

  %if &score_type_2 = 1 %then %do;
    %hd_RunSingleDRSModel(2, &hd_VarsDemogCat, 
                  &hd_VarsDemogCat &hd_VarsDemogCont)
  %end;

  %if &score_type_3 = 1 %then %do;
    %hd_RunSingleDRSModel(3, &hd_VarsDemogCat &hd_VarsPredefCat, 
                  &hd_VarsDemogCat &hd_VarsDemogCont 
                  &hd_VarsPredefCat &hd_VarsPredefCont)
  %end;
  
  %if &score_type_4 = 1 %then %do;
    %hd_RunSingleDRSModel(4, &hd_VarsDemogCat &hd_VarsPredefCat, 
                  &hd_VarsDemogCat &hd_VarsDemogCont 
                  &hd_VarsPredefCat &hd_VarsPredefCont &hd_VarsSelected)
  %end;

  %if &score_type_5 = 1 %then %do;
    %hd_RunSingleDRSModel(5, &hd_VarsDemogCat, 
                  &hd_VarsDemogCat &hd_VarsDemogCont 
                  &hd_VarsSelected)
  %end;
  
  DATA &output_cohort_scored;
    MERGE &output_cohort
    %DO ps_i = 2 %TO &hd_NumModels;
      %IF &&score_type_&ps_i = 1 %THEN %DO;
        hddrs_&ps_i
      %END;
    %END;
    ;
    BY &var_patient_id;
  RUN;
%mend;

%macro hd_MakeDeciles(dataset, model_num, trimming);
  proc sort data=&dataset;
  	by &var_patient_id;
  run;

  %if &trimming = 1 %then %do;
	DATA t_trim;
		SET &dataset(KEEP=&var_patient_id &var_exposure &analysis_score_var&model_num);
	
		keep_observation = -1;
		above_25 = -1;
		below_975 = -1;
	RUN;
	
	PROC SORT DATA=t_trim;
		BY &var_exposure;
	RUN;
	
	PROC UNIVARIATE DATA=t_trim;
		BY &var_exposure;
		VAR &analysis_score_var&model_num;
		OUTPUT OUT=t_percentile pctlpre=p pctlpts=0 to 5 by 2.5, 95 to 100 by 2.5;
	RUN;
	
	PROC SQL;
		UPDATE t_trim
		SET above_25 = (&analysis_score_var&model_num >= 
			(SELECT p2_5 FROM t_percentile WHERE &var_exposure = 1));
	
		UPDATE t_trim
		SET below_975 = (&analysis_score_var&model_num <= 
			(SELECT p97_5 FROM t_percentile WHERE &var_exposure = 0));
	
		UPDATE t_trim
		SET keep_observation = (above_25 = 1 AND below_975 = 1);
	QUIT;
	
	PROC FREQ DATA=t_trim;
		TABLE above_25 below_975 keep_observation;
	RUN;
	
	PROC SORT DATA=t_trim;
		BY &var_patient_id;
	RUN;
	
	DATA &dataset;
		MERGE &dataset
			  t_trim(KEEP=&var_patient_id keep_observation);
		BY &var_patient_id;
	RUN;
  %end;
  
  /* hack -- much faster than deleting the dataset with proc datasets */
  data t_rank_out;
  	set _null_;
  run;
  
  proc rank data=&dataset out=t_rank_out groups=10;
    var &analysis_score_var&model_num;
    ranks ps_rank;
    
    /* if trimming, cut off the top and bottom 2.5 percentiles */
    %if &trimming = 1 %then %do;
    	where keep_observation = 1;
    %end;
  run;

  /* if trimming deleted patients, be sure to merge them back in */
  data &dataset;
    merge &dataset(IN=in1) t_rank_out(IN=in2 keep = &var_patient_id ps_rank);
    by &var_patient_id;

    if in2 then score_decile = ps_rank + 1;
    else score_decile = .;
    
    array score_temp{10} decile1 - decile10;
    do i = 1 to 10;
      score_temp(i) = 0;
    end;
    
    if (in2) then score_temp(score_decile) = 1;
    drop i in1 in2;
  run;
%mend;

%macro hd_Match(dataset, model_number);
  DATA hd_Export;
    SET &dataset;

    f1_patient_id = &var_patient_id;
    f2_exposure = &var_exposure;
    f3_ps = &analysis_score_var&model_number;

    KEEP f1_patient_id f2_exposure f3_ps;
  RUN;

  %hd_ExportFile(match_data, hd_Export);
  
  %LET hd_JavaError = 1;
  DATA _null_;
    DECLARE JavaObj match("org/drugepi/match/Match");
  
    put "NOTE: Internal match program beginning";
  
    match.exceptionDescribe(1);
  
  
    match.callVoidMethod("initMatch", "greedy", "2");

    match.setIntField("startDigit", 5);
    match.setIntField("endDigit", 1);
    match.callVoidMethod("addMatchGroup", "0");
    match.callVoidMethod("addMatchGroup", "1");
    match.setStringField("outfilePath", "&path_temp_dir.matches.txt");
    match.setIntField("fixedRatio", 1);
    match.callVoidMethod("addPatients", "&path_temp_dir.match_data.txt");
    match.callVoidMethod("run");

    rc = match.exceptionCheck(e);
    IF NOT e AND _ERROR_ = 0 THEN DO;
      CALL SYMPUT("hd_JavaError", 0);
      match.delete();
    END;
  RUN;
  %IF &hd_JavaError > 0 %THEN %GOTO EXIT_WITH_ERROR;
  
  PROC IMPORT OUT=hd_Import
      DATAFILE= "&path_temp_dir.matches.txt" 
      DBMS=TAB REPLACE;
      GETNAMES=YES;
      DATAROW=2; 
  RUN;
  
  PROC SORT DATA=hd_Import;
    BY pat_id;
  RUN;
  
  DATA &dataset.Match;
    MERGE &dataset(IN=in1)
        hd_Import(IN=in2 RENAME=(pat_id=&var_patient_id));
    BY &var_patient_id;
    
    IF in1 AND in2;
  RUN;
  
  %GOTO EXIT_MACRO;

  %EXIT_WITH_ERROR: ;
  %LET hd_Error = 1;
  
  %EXIT_MACRO: ; 
%mend;

%macro hd_DoORDecileAnalysis(trimming);
  %PUT NOTE: Running outcome models;
  
  %let tag=deciles;
  %if &trimming = 1 %then %do;
  	%let tag=deciles_trim;
  %end;
  
  %if &score_type_1 = 1 %then %do;
    %put NOTE: Running outcome model 1;
    ods output ParameterEstimates = t_est;
    ods output ConvergenceStatus = t_converge;
    proc logistic data = &analysis_cohort descending;
      model &var_outcome = &var_exposure / RL;
    run;
    %hd_AddRegressionResult(&analysis_cohort, LOGISTIC, 1, &analysis_score_var, &tag, t_est, t_converge);
  %end;
  
  %do NM = 2 %to &hd_NumModels;
    %if &&score_type_&NM = 1 %then %do;
      %put NOTE: Running outcome model &NM, trimming=&trimming;
      
      %hd_MakeDeciles(&analysis_cohort, &NM, &trimming);
      
      data t_analysis_cohort;
      	set &analysis_cohort;
      	
      	if score_decile ^= .;
      	
      	keep &var_outcome &var_exposure decile1-decile10;
      run;
    
      ods output ParameterEstimates = t_est;
      ods output ConvergenceStatus = t_converge;
      proc logistic data = t_analysis_cohort descending;
        model &var_outcome = &var_exposure decile1 - decile4 decile6 - decile10 / RL;
      run;
      %hd_AddRegressionResult(t_analysis_cohort, LOGISTIC, &NM, &analysis_score_var, &tag, t_est, t_converge);
    %end;
  %end;
%mend;

%macro hd_DoHRDecileAnalysis(trimming);
  %PUT NOTE: Running outcome models;
  
  %let tag=deciles;
  %if &trimming = 1 %then %do;
  	%let tag=deciles_trim;
  %end;

  %if &score_type_1 = 1 %then %do;
    %put NOTE: Running outcome model 1;
    ods output ParameterEstimates = t_est;
    ods output ConvergenceStatus = t_converge;
    proc phreg data = &analysis_cohort descending;
      model &var_person_time * &var_outcome.(0) = &var_exposure;
    run;
    %hd_AddRegressionResult(&analysis_cohort, PHREG, 1, &analysis_score_var, &tag, t_est, t_converge);
  %end;

  %do NM = 2 %to &hd_NumModels;
    %if &&score_type_&NM = 1 %then %do;
      %put NOTE: Running outcome model &NM;
      
      %hd_MakeDeciles(&analysis_cohort, &NM, &trimming);
      
      data t_analysis_cohort;
      	set &analysis_cohort;
      	
      	if score_decile ^= .;
      	
      	keep &var_outcome &var_exposure decile1-decile10;
      run;
    
      ods output ParameterEstimates = t_est;
      ods output ConvergenceStatus = t_converge;
      proc phreg data = t_analysis_cohort descending;
        model &var_person_time * &var_outcome.(0) = &var_exposure decile1 - decile4 decile6 - decile10;
      run;
      %hd_AddRegressionResult(t_analysis_cohort, PHREG, &NM, &analysis_score_var, &tag, t_est, t_converge);
    %end;
  %end;
%mend;

%macro hd_DoORMatchedAnalysis;
  %do NM = 2 %to &hd_NumModels;
    %if &&score_type_&NM = 1 %then %do;
      %put NOTE: Running matched output model &NM;
      %hd_Match(&analysis_cohort, &NM);
      %if &hd_Error > 0 %then %goto EXIT_WITH_ERROR;
      
      ods output ParameterEstimates = t_est;
      ods output ConvergenceStatus = t_converge;
      proc logistic data = &analysis_cohort.Match descending;
        model &var_outcome = &var_exposure / RL;
      run;    
      %hd_AddRegressionResult(&analysis_cohort.Match, LOGISTIC, &NM, &analysis_score_var, matched, t_est, t_converge);
    %end;
  %end;
  
  %GOTO EXIT_MACRO;

  %EXIT_WITH_ERROR: ;
  %LET hd_Error = 1;
  
  %EXIT_MACRO: ;  
%mend;

%macro hd_DoHRMatchedAnalysis;
  %do NM = 2 %to &hd_NumModels;
    %if &&score_type_&NM = 1 %then %do;
      %put NOTE: Running matched output model &NM;
      %hd_Match(&analysis_cohort, &NM);
      %if &hd_Error > 0 %then %goto EXIT_WITH_ERROR;
      
      ods output ParameterEstimates = t_est;
      ods output ConvergenceStatus = t_converge;
      proc phreg data = &analysis_cohort.Match descending;
        model &var_person_time * &var_outcome.(0) = &var_exposure;
      run;
      %hd_AddRegressionResult(&analysis_cohort.Match, PHREG, &NM, &analysis_score_var, matched, t_est, t_converge);
    %end;
  %end;
  
  %GOTO EXIT_MACRO;

  %EXIT_WITH_ERROR: ;
  %LET hd_Error = 1;
  
  %EXIT_MACRO: ;  
%mend;


%macro hd_CompileResults;
  OPTION NONOTES;

  PROC SQL NOPRINT;
    DELETE FROM results_store 
    WHERE field_name IS NULL;

    SELECT COUNT(*) AS x
    INTO :num_fields
    FROM results_field_info;
  
    SELECT field_name, field_type
    INTO :field1 - :field100, :type1 - :type100
    FROM results_field_info
    ORDER BY field_order;
      
    CREATE TABLE &result_estimates (
      %DO i = 1 %TO &num_fields;
        %IF &&type&i EQ N %THEN %DO;
          &&field&i int
        %END;
        %ELSE %DO;
          &&field&i char(150)
        %END;
        
        %IF &i < &num_fields %THEN %DO;
          ,
        %END;
      %END;
    );

    SELECT MAX(model_number) AS x
    INTO :num_models
    FROM results_store;
    
    SELECT DISTINCT(score_var) AS X
    INTO :score_var1 - :score_var1000
    FROM results_store;
    
	%DO score_var_i = 1 %TO &sqlobs;
		%DO model_num = 1 %TO &num_models;
		  INSERT INTO &result_estimates(model_number, score_var) VALUES(&model_num, "&&score_var&score_var_i");
		  
		  SELECT COUNT(*) AS x
		  INTO :q
		  FROM &result_estimates;
		%END;
	%END;

    SELECT COUNT(*) AS x
    INTO :num_results
    FROM results_store;
  
    SELECT model_number, score_var, field_name, value_c, value_n
    INTO :model_number1 - :model_number1000,
         :score_var1 - :score_var1000,
         :field_name1 - :field_name1000,
         :value_c1 - :value_c1000,
         :value_n1 - :value_n1000
    FROM results_store;

    %DO result_num = 1 %TO &num_results;
      UPDATE &result_estimates
  
      %IF &&value_n&result_num ^= -999 %THEN %DO;
        SET &&field_name&result_num = &&value_n&result_num
      %END;
      %ELSE %DO;
        SET &&field_name&result_num = "&&value_c&result_num"
      %END;       
      
      WHERE model_number = &&model_number&result_num AND score_var = "&&score_var&result_num";
    %END;

    DELETE FROM &result_estimates
    WHERE model_number IS NULL;
  QUIT;

  OPTION NOTES;

  PROC SORT DATA=&result_estimates;
    BY DESCENDING score_var model_number;
  RUN;
  
  ODS LISTING;  
  proc print data = &result_estimates noobs;
  run;
  ODS LISTING CLOSE;

  OPTION NONOTES;
  PROC EXPORT DATA=&result_estimates
    OUTFILE="&path_temp_dir.output_estimates.txt"
    DBMS=TAB REPLACE;
  RUN;
  OPTION NOTES;
%mend;

%macro hd_PrintDiagnostic;
  ODS LISTING;
  ODS LISTING CLOSE;

  OPTION NOTES;
%mend;

%macro hd_InitClasspathUpdate;
  DATA _null_;
      LENGTH  path_separator $ 2
              orig_classpath $ 500;

      DECLARE JavaObj f("java.io.File", "");
      f.getStaticStringField("pathSeparator", path_separator);

      orig_classpath = STRIP(SYSGET("CLASSPATH"));

      IF _ERROR_ = 1 OR LENGTH(orig_classpath) = 0 THEN DO;
      PUT "NOTE: Due to SAS limitations, ignore any classpath-related messages from the prior or next statement(s)";
          orig_classpath = "";
    END;

      CALL SYMPUTX('CP_orig_classpath', STRIP(orig_classpath), 'GLOBAL');
      CALL SYMPUTX('CP_path_separator', STRIP(path_separator), 'GLOBAL');
      f.delete();
  RUN;
%mend;

%macro hd_AddToClasspath(cp_addition);
  DATA _null_;
      LENGTH  current_classpath $ 500
              new_classpath $ 500;

      current_classpath = STRIP(SYSGET("CLASSPATH"));

      IF _ERROR_ = 1 OR LENGTH(current_classpath) = 0 THEN DO;
      PUT "NOTE: Due to SAS limitations, ignore any classpath-related messages from the prior or next statement(s)";
          new_classpath = "&cp_addition";
    END;
      ELSE DO;
      IF FIND(current_classpath, "&cp_addition", "ti") = 0 THEN 
            new_classpath = STRIP(current_classpath) || "&CP_path_separator" || "&cp_addition";
      ELSE
        new_classpath = current_classpath;
    END;

      CALL SYMPUTX('CP_new_classpath', STRIP(new_classpath), 'GLOBAL');
  RUN;

  %PUT NOTE: Setting Java classpath to &CP_new_classpath;
  OPTIONS SET=CLASSPATH "&CP_new_classpath";
%mend;

%macro hd_UploadResults;
  %LET hd_JavaError = 1;
  
  %LET local_analysis_num = %CMPRES(&analysis_num);

  %PUT NOTE: Uploading results to hdpharmacoepi.org with analysis number &local_analysis_num ;

  DATA _null_;
    DECLARE JavaObj uploader("org/drugepi/upload/Uploader");

    uploader.exceptionDescribe(1);
  
    uploader.callStaticVoidMethod("uploadFile", "&local_analysis_num", "&path_temp_dir.output_all_vars.txt");
    uploader.callStaticVoidMethod("uploadFile", "&local_analysis_num", "&path_temp_dir.output_estimates.txt");
    uploader.callStaticVoidMethod("uploadFile", "&local_analysis_num", "&path_temp_dir.output_z_bias.txt");
    uploader.callStaticVoidMethod("uploadFile", "&local_analysis_num", "&path_temp_dir.output_dimension_codes.txt");

    rc = uploader.ExceptionCheck(e);
    IF NOT e AND _ERROR_ = 0 THEN DO;
      CALL SYMPUT("hd_JavaError", 0);
      uploader.delete();
    END;
  RUN;
  %IF &hd_JavaError > 0 %THEN %GOTO EXIT_WITH_ERROR;

  %EXIT_WITH_ERROR: ;
  %LET hd_Error = 1;
  
  %EXIT_MACRO: ; 
%mend;

%macro hd_CheckParameterSpelling(param1, param2, default=);
  DATA _NULL_;
    LENGTH p1 $200 p2 $200;

    p1 = "";
    p2 = "";

    IF (SYMEXIST("&param1")) THEN p1 = STRIP(PUT(SYMGET("&param1"), $200.));
    IF (SYMEXIST("&param2")) THEN p2 = STRIP(PUT(SYMGET("&param2"), $200.));

    PUT p1=;
    PUT p2=;

    * undefined parameter will return its own name;
    IF ((p1 EQ "") AND (p2 NE "")) THEN DO;
      PUT "WARNING: Setting parameter &param1 to value specified by &param2 since &param1 was not set and &param2 was set ";
      PUT p2=;
      CALL SYMPUTX("&param1", p2, "GLOBAL");
    END;

    IF (("&default" NE "") AND (p1 EQ "&default") AND (p2 NE "")) THEN DO;
      PUT "WARNING: Setting parameter &param1 to value specified by &param2 since &param1 was a default value and &param2 contained a nondefault value ";
      PUT p2=;
      CALL SYMPUTX("&param1", p2, "GLOBAL");
    END;
  RUN;
%mend;

%macro hd_ResetClasspath;
  %PUT NOTE: Setting Java classpath back to its original state: &CP_orig_classpath;
  OPTIONS SET=CLASSPATH "&CP_orig_classpath";
%mend;

%macro InitHDMacros(
        selection_mode    = LOCAL,
        var_patient_id    = ,
        var_exposure      = ,
        var_outcome       = ,
        var_person_time   = ,

        vars_force_categorical  = ,
        vars_demographic        = ,
        vars_predefined         = ,
        vars_ignore             = ,

        analysis_num      = ,
        upload_results    = 0,
        
        top_n           = 200,
        k               = 500,
        frequency_min	= 0,
        cutpoints       = ,
        ranking_method  = BIAS,
        outcome_type    = DICHOTOMOUS,

        path_temp_dir   =,
        path_jar_file   =,

        db_driver_class     = org.netezza.Driver,
        db_driver_classpath =,
        db_url              =,
        db_username         =,
        db_password         =,
        
        db_keep_output_tables = 0,

        output_cohort       = output_cohort,
        result_estimates    = result_estimates,
        result_diagnostic   = result_diagnostic,

        infer_service_intensity_vars  = 0,
        create_time_interactions      = 0,
        create_profile_scores		  = 0,
        outcome_zero_cell_corr        = 0,
        
        score_type_1      = 1,
        score_type_2      = 1,
        score_type_3      = 1,
        score_type_4      = 1,
        score_type_5      = 1,
        
        outcome_model_deciles = 1,
        outcome_model_matched = 1,

        input_cohort       = ,
        input_cohort_table = ,
        
        input_dim1  = ,   input_dim2  = ,
        input_dim3  = ,   input_dim4  = ,
        input_dim5  = ,   input_dim6  = ,
        input_dim7  = ,   input_dim8  = ,
        input_dim9  = ,   input_dim10 = ,
        input_dim11 = ,   input_dim12 = ,
        input_dim13 = ,   input_dim14 = ,
        input_dim15 = ,   input_dim16 = ,
        input_dim17 = ,   input_dim18 = ,
        input_dim19 = ,   input_dim20 = ,
        input_dim21 = ,   input_dim22 = ,
        input_dim23 = ,   input_dim24 = ,
        input_dim25 = ,   input_dim26 = ,
        input_dim27 = ,   input_dim28 = ,
        input_dim29 = ,   input_dim30 = ,
        input_dim31 = ,   input_dim32 = ,
        input_dim33 = ,   input_dim34 = ,
        input_dim35 = ,   input_dim36 = ,
        input_dim37 = ,   input_dim38 = ,
        input_dim39 = ,   input_dim40 = ,
        input_dim41 = ,   input_dim42 = ,
        input_dim43 = ,   input_dim44 = ,
        input_dim45 = ,   input_dim46 = ,
        input_dim47 = ,   input_dim48 = ,
        input_dim49 = ,   input_dim50 = ,
        input_dim51 = ,   input_dim52 = ,
        input_dim53 = ,   input_dim54 = ,
        input_dim55 = ,   input_dim56 = ,
        input_dim57 = ,   input_dim58 = ,
        input_dim59 = ,   input_dim60 = ,
        input_dim61 = ,   input_dim62 = ,
        input_dim63 = ,   input_dim64 = ,
        input_dim65 = ,   input_dim66 = ,
        input_dim67 = ,   input_dim68 = ,
        input_dim69 = ,   input_dim70 = ,
        input_dim71 = ,   input_dim72 = ,
        input_dim73 = ,   input_dim74 = ,
        input_dim75 = ,   input_dim76 = ,
        input_dim77 = ,   input_dim78 = ,
        input_dim79 = ,   input_dim80 = ,
        input_dim81 = ,   input_dim82 = ,
        input_dim83 = ,   input_dim84 = ,
        input_dim85 = ,   input_dim86 = ,
        input_dim87 = ,   input_dim88 = ,
        input_dim89 = ,   input_dim90 = ,
        input_dim91 = ,   input_dim92 = ,
        input_dim93 = ,   input_dim94 = ,
        input_dim95 = ,   input_dim96 = ,
        input_dim97 = ,   input_dim98 = ,
        input_dim99 = ,   input_dim100 = 

);

  %local flag maxdim newmaxdim tmp_i tmp_j tmp_k;
  %local num_cp num_bin num_entr t_dot t_Pos t_dimname t_dumvar t_relday;
  * Check to make sure there are no gaps in the listing of input_dim;
  %let flag = 0;
  %let maxdim = 0;
  %do tmp_i = 1 %to 100;
    * If no cutpoints are entered, the number of entries should be 2;
    %if &cutpoints eq %str() and &&input_dim&tmp_i ne %str() %then %do;
      %if %sysfunc(countw(%quote(&&input_dim&tmp_i), " ")) > 3 %then %do;
      %put ERROR: There can only be 3 entries in input_dim&tmp_i : id, code, day : &&input_dim&tmp_i;
      %goto EXIT_ERROR;
    %end;

    %end;
    %if &flag = 0 and &&input_dim&tmp_i = %str() %then %do;
      %let flag = 1;
      %let maxdim = %eval(&tmp_i - 1);
    %end;
    %else %if &flag = 1 and &&input_dim&tmp_i ne %str() %then %do;
      * If this happens, there is a gap in the listing of input_dim;
      * Should be 1, 2, 3, 4, 5, ... not 1, 2, 4, 5, ...; 
      %let flag = 2;
      %put ERROR: Prior to input_dim&tmp_i there is a skip in the dimension entry;
      %put ERROR: Last read entry before skip is input_dim&maxdim; 
      %goto EXIT_ERROR;
    %end;


  %end;
  %put NOTE: No gaps found in entry order of dim;

  * If there are cutpoints entered go through the process;
  * of setting up new input_dim;
  %if &cutpoints ne %str() %then %do;
    %put NOTE: Cutpoints entered in macro;
    /*  Parse the cutpoints*/
    /*  Count up how many cutpoints there are*/
    %let num_cp = %sysfunc(countw(&cutpoints));

    /*  Count the number of bins that exist between the cutpoints*/
    %let num_bin = %eval(&num_cp - 1);

    /*  Create cutpoint macros: cp1, cp2, ... */
    /*  For example, if the cutpoints are 0 -30 -60, the cutpoints will*/
    /*  be cp1 = 0, cp2 = -30, cp3 = -60*/
    %do tmp_i = 1 %to &num_cp;
      %let cp&tmp_i = %scan(&cutpoints,&tmp_i);
    %end;

    %do tmp_i = 1 %to &maxdim;
      %let old_input_dim&tmp_i = &&input_dim&tmp_i;
    %end;

    %let tmp_j = 1;
    %do tmp_i = 1 %to &maxdim;
      %let num_entr = %sysfunc(countw(%quote(&&old_input_dim&tmp_i), " "));
      %if &num_entr = 2 %then %do;
        %let input_dim&tmp_j = &&old_input_dim&tmp_i;
        %let tmp_j = %eval(&tmp_j + 1);
      %end;
      %else %if &num_entr = 3 %then %do;
        %do tmp_k = 1 %to &num_bin;
          %let tmp_k1 = %eval(&tmp_k + 1);
          * Create new input_dim a, b, c ... (number depends on how many bins there are);
          * extract new dataset name is old dataset name_1;
          %let t_dumvar = %sysfunc(scan(&&old_input_dim&tmp_i,2));
          %let t_relday = %sysfunc(scan(&&old_input_dim&tmp_i,-1));


          %let t_dot = .;
          %let t_Pos = %index(&&old_input_dim&tmp_i,&t_dot);
          %if &t_Pos > 0 %then %do;
            %let fullname = %sysfunc(scan(&&old_input_dim&tmp_i,1, " "));
            %let t_dimname = %substr(%sysfunc(scan(&&old_input_dim&tmp_i,1, " ")),%eval(&t_Pos+1));
          %end;
          %else %do;
            %let t_dimname = %sysfunc(scan(&&old_input_dim&tmp_i,1));
          %end;


          proc sql;
            create table &t_dimname._&tmp_k as
            select * from &t_dimname
            where &&cp&tmp_k le &t_relday le &&cp&tmp_k1;
          quit;
          %let input_dim&tmp_j = &t_dimname &t_dumvar;
          %let tmp_j = %eval(&tmp_j + 1);
        %end;
      %end;
      %else %do;
        %put ERROR: input_dim &tmp_j has &num_entr entries.  The dim should have 2 or 3 entries;
        %goto EXIT_ERROR;
      %end;

    %end;
    %let newmaxdim = %eval(&tmp_j - 1);
    %if &newmaxdim > 100 %then %do;
      %put ERROR: Over 100 input_dim created in loop.  ;
      %put ERROR: During cutpoint separation &newmaxdim input_dim are created;
      %goto EXIT_ERROR;
    %end;
    %put NOTE: Dim have been reset using cutpoints;
  %end;

  %hd_CheckParameterSpelling(score_type_1, score_type1, default=1);
  %hd_CheckParameterSpelling(score_type_2, score_type2, default=1);
  %hd_CheckParameterSpelling(score_type_3, score_type3, default=1);
  %hd_CheckParameterSpelling(score_type_4, score_type4, default=1);
  %hd_CheckParameterSpelling(score_type_5, score_type5, default=1);

  %hd_CheckParameterSpelling(result_estimates, results_estimates, default=result_estimates);
  %hd_CheckParameterSpelling(result_diagnostic, results_diagnostic, default=result_diagnostic);

  PROC SQL;
    CREATE TABLE hd_parameters (
      param_name      varchar(40),
      param_value     varchar(3000)
    );
    
    INSERT INTO hd_parameters(param_name, param_value) 
      VALUES ("selection_mode", "%QUOTE(&selection_mode)")
      VALUES ("var_patient_id", "%QUOTE(&var_patient_id)")
      VALUES ("var_exposure", "%QUOTE(&var_exposure)")
      VALUES ("var_outcome", "%QUOTE(&var_outcome)")
      VALUES ("var_person_time", "%QUOTE(&var_person_time)")
      VALUES ("vars_force_categorical", "%QUOTE(&vars_force_categorical)")
      VALUES ("vars_demographic", "%QUOTE(&vars_demographic)")
      VALUES ("vars_predefined", "%QUOTE(&vars_predefined)")
      VALUES ("vars_ignore", "%QUOTE(&vars_ignore)")
      VALUES ("top_n", "%QUOTE(&top_n)")
      VALUES ("hd_k", "%QUOTE(&k)")
      VALUES ("frequency_min", "%QUOTE(&frequency_min)")
      VALUES ("upload_results", "%QUOTE(&upload_results)")
      VALUES ("analysis_num", "%QUOTE(&analysis_num)")
      VALUES ("ranking_method", "%QUOTE(&ranking_method)")
      VALUES ("outcome_type", "%QUOTE(&outcome_type)")
      VALUES ("path_temp_dir", "%QUOTE(&path_temp_dir)")
      VALUES ("path_jar_file", "%QUOTE(&path_jar_file)")
      VALUES ("db_driver_class", "%QUOTE(&db_driver_class)")
      VALUES ("db_driver_classpath", "%QUOTE(&db_driver_classpath)")
      VALUES ("db_url", "%QUOTE(&db_url)")
      VALUES ("db_username", "%QUOTE(&db_username)")
      VALUES ("db_password", "%QUOTE(&db_password)")
      VALUES ("db_keep_output_tables", "%QUOTE(&db_keep_output_tables)")
      VALUES ("output_cohort", "%QUOTE(&output_cohort)")
      VALUES ("result_estimates", "%QUOTE(&result_estimates)")
      VALUES ("input_cohort", "%QUOTE(&input_cohort)")
      VALUES ("result_diagnostic", "%QUOTE(&result_diagnostic)")
      VALUES ("input_cohort_table", "%QUOTE(&input_cohort_table)")
      VALUES ("infer_service_intensity_vars", "%QUOTE(&infer_service_intensity_vars)")
      VALUES ("create_time_interactions", "%QUOTE(&create_time_interactions)")
      VALUES ("create_profile_scores", "%QUOTE(&create_profile_scores)")
      VALUES ("outcome_zero_cell_corr", "%QUOTE(&outcome_zero_cell_corr)")
      VALUES ("score_type_1", "%QUOTE(&score_type_1)")
      VALUES ("score_type_2", "%QUOTE(&score_type_2)")
      VALUES ("score_type_3", "%QUOTE(&score_type_3)")
      VALUES ("score_type_4", "%QUOTE(&score_type_4)")
      VALUES ("score_type_5", "%QUOTE(&score_type_5)")
      VALUES ("outcome_model_deciles", "%QUOTE(&outcome_model_deciles)")
      VALUES ("outcome_model_matched", "%QUOTE(&outcome_model_matched)")
      VALUES ("input_dim1", "%QUOTE(&input_dim1)")
      VALUES ("input_dim2", "%QUOTE(&input_dim2)")
      VALUES ("input_dim3", "%QUOTE(&input_dim3)")
      VALUES ("input_dim4", "%QUOTE(&input_dim4)")
      VALUES ("input_dim5", "%QUOTE(&input_dim5)")
      VALUES ("input_dim6", "%QUOTE(&input_dim6)")
      VALUES ("input_dim7", "%QUOTE(&input_dim7)")
      VALUES ("input_dim8", "%QUOTE(&input_dim8)")
      VALUES ("input_dim9", "%QUOTE(&input_dim9)")
      VALUES ("input_dim10", "%QUOTE(&input_dim10)")
      VALUES ("input_dim11", "%QUOTE(&input_dim11)")
      VALUES ("input_dim12", "%QUOTE(&input_dim12)")
      VALUES ("input_dim13", "%QUOTE(&input_dim13)")
      VALUES ("input_dim14", "%QUOTE(&input_dim14)")
      VALUES ("input_dim15", "%QUOTE(&input_dim15)")
      VALUES ("input_dim16", "%QUOTE(&input_dim16)")
      VALUES ("input_dim17", "%QUOTE(&input_dim17)")
      VALUES ("input_dim18", "%QUOTE(&input_dim18)")
      VALUES ("input_dim19", "%QUOTE(&input_dim19)")
      VALUES ("input_dim20", "%QUOTE(&input_dim20)")
      VALUES ("input_dim21", "%QUOTE(&input_dim21)")
      VALUES ("input_dim22", "%QUOTE(&input_dim22)")
      VALUES ("input_dim23", "%QUOTE(&input_dim23)")
      VALUES ("input_dim24", "%QUOTE(&input_dim24)")
      VALUES ("input_dim25", "%QUOTE(&input_dim25)")
      VALUES ("input_dim26", "%QUOTE(&input_dim26)")
      VALUES ("input_dim27", "%QUOTE(&input_dim27)")
      VALUES ("input_dim28", "%QUOTE(&input_dim28)")
      VALUES ("input_dim29", "%QUOTE(&input_dim29)")
      VALUES ("input_dim30", "%QUOTE(&input_dim30)")
      VALUES ("input_dim31", "%QUOTE(&input_dim31)")
      VALUES ("input_dim32", "%QUOTE(&input_dim32)")
      VALUES ("input_dim33", "%QUOTE(&input_dim33)")
      VALUES ("input_dim34", "%QUOTE(&input_dim34)")
      VALUES ("input_dim35", "%QUOTE(&input_dim35)")
      VALUES ("input_dim36", "%QUOTE(&input_dim36)")
      VALUES ("input_dim37", "%QUOTE(&input_dim37)")
      VALUES ("input_dim38", "%QUOTE(&input_dim38)")
      VALUES ("input_dim39", "%QUOTE(&input_dim39)")
      VALUES ("input_dim40", "%QUOTE(&input_dim40)")
      VALUES ("input_dim41", "%QUOTE(&input_dim41)")
      VALUES ("input_dim42", "%QUOTE(&input_dim42)")
      VALUES ("input_dim43", "%QUOTE(&input_dim43)")
      VALUES ("input_dim44", "%QUOTE(&input_dim44)")
      VALUES ("input_dim45", "%QUOTE(&input_dim45)")
      VALUES ("input_dim46", "%QUOTE(&input_dim46)")
      VALUES ("input_dim47", "%QUOTE(&input_dim47)")
      VALUES ("input_dim48", "%QUOTE(&input_dim48)")
      VALUES ("input_dim49", "%QUOTE(&input_dim49)")
      VALUES ("input_dim50", "%QUOTE(&input_dim50)")
      VALUES ("input_dim51", "%QUOTE(&input_dim51)")
      VALUES ("input_dim52", "%QUOTE(&input_dim52)")
      VALUES ("input_dim53", "%QUOTE(&input_dim53)")
      VALUES ("input_dim54", "%QUOTE(&input_dim54)")
      VALUES ("input_dim55", "%QUOTE(&input_dim55)")
      VALUES ("input_dim56", "%QUOTE(&input_dim56)")
      VALUES ("input_dim57", "%QUOTE(&input_dim57)")
      VALUES ("input_dim58", "%QUOTE(&input_dim58)")
      VALUES ("input_dim59", "%QUOTE(&input_dim59)")
      VALUES ("input_dim60", "%QUOTE(&input_dim60)")
      VALUES ("input_dim61", "%QUOTE(&input_dim61)")
      VALUES ("input_dim62", "%QUOTE(&input_dim62)")
      VALUES ("input_dim63", "%QUOTE(&input_dim63)")
      VALUES ("input_dim64", "%QUOTE(&input_dim64)")
      VALUES ("input_dim65", "%QUOTE(&input_dim65)")
      VALUES ("input_dim66", "%QUOTE(&input_dim66)")
      VALUES ("input_dim67", "%QUOTE(&input_dim67)")
      VALUES ("input_dim68", "%QUOTE(&input_dim68)")
      VALUES ("input_dim69", "%QUOTE(&input_dim69)")
      VALUES ("input_dim70", "%QUOTE(&input_dim70)")
      VALUES ("input_dim71", "%QUOTE(&input_dim71)")
      VALUES ("input_dim72", "%QUOTE(&input_dim72)")
      VALUES ("input_dim73", "%QUOTE(&input_dim73)")
      VALUES ("input_dim74", "%QUOTE(&input_dim74)")
      VALUES ("input_dim75", "%QUOTE(&input_dim75)")
      VALUES ("input_dim76", "%QUOTE(&input_dim76)")
      VALUES ("input_dim77", "%QUOTE(&input_dim77)")
      VALUES ("input_dim78", "%QUOTE(&input_dim78)")
      VALUES ("input_dim79", "%QUOTE(&input_dim79)")
      VALUES ("input_dim80", "%QUOTE(&input_dim80)")
      VALUES ("input_dim81", "%QUOTE(&input_dim81)")
      VALUES ("input_dim82", "%QUOTE(&input_dim82)")
      VALUES ("input_dim83", "%QUOTE(&input_dim83)")
      VALUES ("input_dim84", "%QUOTE(&input_dim84)")
      VALUES ("input_dim85", "%QUOTE(&input_dim85)")
      VALUES ("input_dim86", "%QUOTE(&input_dim86)")
      VALUES ("input_dim87", "%QUOTE(&input_dim87)")
      VALUES ("input_dim88", "%QUOTE(&input_dim88)")
      VALUES ("input_dim89", "%QUOTE(&input_dim89)")
      VALUES ("input_dim90", "%QUOTE(&input_dim90)")
      VALUES ("input_dim91", "%QUOTE(&input_dim91)")
      VALUES ("input_dim92", "%QUOTE(&input_dim92)")
      VALUES ("input_dim93", "%QUOTE(&input_dim93)")
      VALUES ("input_dim94", "%QUOTE(&input_dim94)")
      VALUES ("input_dim95", "%QUOTE(&input_dim95)")
      VALUES ("input_dim96", "%QUOTE(&input_dim96)")
      VALUES ("input_dim97", "%QUOTE(&input_dim97)")
      VALUES ("input_dim98", "%QUOTE(&input_dim98)")
      VALUES ("input_dim99", "%QUOTE(&input_dim99)")
      VALUES ("input_dim100", "%QUOTE(&input_dim100)")
  ;   
    
  QUIT;
  
  %hd_InitResults;

  %EXIT_ERROR: ;
%mend;

%macro hd_ReadMacroVariables;
  PROC SQL;
    SELECT param_name, param_value
    INTO :param_name1-:param_name150, :param_value1-:param_value150
    FROM hd_parameters;
  QUIT;
  
  %DO param_i = 1 %to &sqlobs;
    %GLOBAL &&param_name&param_i;
    %LET &&param_name&param_i = &&param_value&param_i;
  %END;
%mend;


%macro DoHDVariableSelection;
  ODS LISTING CLOSE;

  %hd_ReadMacroVariables;

  %let hd_Error = 0;

  %hd_PrintBanner;  
  %hd_CheckGeneralConditions;

  %if &hd_Error = 0 %then %hd_OpenDims;
  %else %goto EXIT_ERROR;
    
  %if &selection_mode = LOCAL %then %do;
    %if &hd_Error = 0 %then %hd_CheckVarSelectionConditions;
    %else %goto EXIT_ERROR;
      
    %if &hd_Error = 0 %then %hd_OpenMaster;
    %else %goto EXIT_ERROR;
    
  %end;
  %else %do;
    %hd_AddToClasspath(&db_driver_classpath);
  %end;

  %if &hd_Error = 0 %then %hd_RunVarSelection;
  %else %goto EXIT_ERROR;

  %if &hd_Error = 0 %then %do;
    %if &upload_results = 1 %then %do;
      %hd_UploadResults;
    %end;
  %end;
  %else %goto EXIT_ERROR;
  

  %PUT NOTE: hd-PS macro done ;
  
  %GOTO EXIT_CLEAN;

  %EXIT_ERROR: ;
    %PUT ERROR: The hd-PS macro ended with an error;
    %GOTO EXIT_FINAL;
  %EXIT_CLEAN: ;
  %EXIT_FINAL: ;
    %hd_ResetClasspath;
    ODS LISTING;
%mend;

%macro EstimateHDPS(the_cohort);
  ODS LISTING CLOSE;

  %hd_ReadMacroVariables;

  %GLOBAL output_cohort_scored;
  %LET output_cohort_scored = &the_cohort;

  %let hd_Error = 0;

  %hd_PrintBanner;  
  %hd_CheckGeneralConditions;
  %hd_CheckEstimationConditions(&output_cohort_scored);
    
  %if &hd_Error = 0 %then %hd_OpenMaster;
  %else %goto EXIT_ERROR;

  %if &hd_Error = 0 %then %hd_InitializeVariableLists;
  %else %goto EXIT_ERROR;

  %if &hd_Error = 0 %then %hd_EstimateHDPS;
  %else %goto EXIT_ERROR;
    
  %if &hd_Error = 0 %then %hd_PrintDiagnostic;
  %else %goto EXIT_ERROR;

  %if &hd_Error = 0 %then %do;
    %if &upload_results = 1 %then %do;
      %hd_UploadResults;
    %end;
  %end;
  %else %goto EXIT_ERROR;

  %PUT NOTE: hd-PS macro done ;
  
  %GOTO EXIT_CLEAN;

  %EXIT_ERROR: ;
    %PUT ERROR: The hd-PS macro ended with an error;
    %GOTO EXIT_FINAL;
  %EXIT_CLEAN: ;
  %EXIT_FINAL: ;
    %hd_ResetClasspath;
    ODS LISTING;
%mend;

%macro EstimateHDDRS(the_cohort);
  ODS LISTING CLOSE;

  %GLOBAL output_cohort_scored;
  %LET output_cohort_scored = &the_cohort;

  %hd_ReadMacroVariables;

  %let hd_Error = 0;

  %hd_PrintBanner;  
  %hd_CheckGeneralConditions;
  *%hd_CheckEstimationConditions(&output_cohort_scored);
    
  %if &hd_Error = 0 %then %hd_OpenMaster;
  %else %goto EXIT_ERROR;

  %if &hd_Error = 0 %then %hd_InitializeVariableLists;
  %else %goto EXIT_ERROR;

  %if &hd_Error = 0 %then %hd_EstimateHDDRS;
  %else %goto EXIT_ERROR;
    
  %if &hd_Error = 0 %then %hd_PrintDiagnostic;
  %else %goto EXIT_ERROR;

  %if &hd_Error = 0 %then %do;
    %if &upload_results = 1 %then %do;
      %hd_UploadResults;
    %end;
  %end;
  %else %goto EXIT_ERROR;

  %PUT NOTE: hd-PS macro done ;
  
  %GOTO EXIT_CLEAN;

  %EXIT_ERROR: ;
    %PUT ERROR: The hd-PS macro ended with an error;
    %GOTO EXIT_FINAL;
  %EXIT_CLEAN: ;
  %EXIT_FINAL: ;
    %hd_ResetClasspath;
    ODS LISTING;
%mend;


%macro RunOutcomeModels(cohort_dataset, score_var);
  ODS LISTING CLOSE;
  
  %hd_ReadMacroVariables;

  %GLOBAL analysis_cohort;
  %LET analysis_cohort = &cohort_dataset;

  %GLOBAL analysis_score_var;
  %LET analysis_score_var = %UPCASE(&score_var);
  
  %let hd_Error = 0;

  %hd_PrintBanner;  
  %hd_CheckGeneralConditions;
  %hd_CheckEstimationConditions;
    
  %if &hd_Error = 0 %then %hd_OpenMaster;
  %else %goto EXIT_ERROR;

  %if &hd_Error = 0 %then %hd_InitializeVariableLists;
  %else %goto EXIT_ERROR;

  %hd_InitScoreResults(&analysis_score_var);

  %if &hd_Error = 0 %then %do;
    %if &outcome_model_deciles = 1 %then %do;
      %if &var_person_time = %then %do;
      	%hd_DoORDecileAnalysis(trimming=0);
      	%hd_DoORDecileAnalysis(trimming=1);
      %end;
      %else %do;
      	%hd_DoHRDecileAnalysis(trimming=0);
      	%hd_DoHRDecileAnalysis(trimming=1);
      %end;
    %end;
  %end;
  %else %goto EXIT_ERROR;
  
  %if &hd_Error = 0 %then %do;
    %if &outcome_model_matched = 1 %then %do;
      %if &var_person_time = %then %hd_DoORMatchedAnalysis;
      %else %hd_DoHRMatchedAnalysis;
    %end;
  %end;
  %else %goto EXIT_ERROR;

  %if &hd_Error = 0 %then %hd_CompileResults;
  %else %goto EXIT_ERROR;

  %if &hd_Error = 0 %then %hd_PrintDiagnostic;
  %else %goto EXIT_ERROR;

  %if &hd_Error = 0 %then %do;
    %if &upload_results = 1 %then %do;
      %hd_UploadResults;
    %end;
  %end;
  %else %goto EXIT_ERROR;

  %PUT NOTE: hd-PS macro done ;
  
  %GOTO EXIT_CLEAN;

  %EXIT_ERROR: ;
    %PUT ERROR: The hd-PS macro ended with an error;
    %GOTO EXIT_FINAL;
  %EXIT_CLEAN: ;
  %EXIT_FINAL: ;
    %hd_ResetClasspath;
    ODS LISTING;
%mend;


