/*
 * MATCHING.SAS
 *
 * Copyright 2010-11 Brigham and Women's Hospital.  
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


%GLOBAL match_Error;

%macro match_PrintBanner;
	%PUT %STR(NOTE: Matching Macros);
	%PUT %STR(NOTE: See http://www.hdhpharmacoepi.org for full details);
	%PUT %STR(NOTE: Report bugs to bugs@hdhpharmacoepi.org);
	%PUT %STR(NOTE: Copyright 2008-11 Division of Pharmacoepidemiology, Brigham and Womens Hospital);
%mend;

%global match_groups;
%global match_num_groups;
%global match_file_name;
%macro match_MatchPrep(match_type, in_dataset, out_dataset, working_directory, 
				       var_patient_id, var_exposure, exp_groups, var_ps);

	%match_PrintBanner;

	%LET match_Error = 0;

	%LET match_num_groups = 0;
	%LET match_group = INVALID;
	%DO %UNTIL (&match_group=);
		%LET match_num_groups = %EVAL(&match_num_groups+1);
		%LET match_group = %SCAN(&exp_groups, &match_num_groups);
	%END;
	/* hack -- bad */
	%LET match_num_groups = %EVAL(&match_num_groups-1);
	
	%PUT NOTE: &exp_groups is &match_num_groups groups;
	
	DATA match_Export;
		SET &in_dataset;

		f1_patient_id = &var_patient_id;
		f2_expcat = &var_exposure;

		%DO group_num = 1 %TO %EVAL(&match_num_groups - 1);
			%LET ps_var = %SCAN(&var_ps, &group_num);
			%IF &ps_var = . %THEN %DO;
				%PUT Not enough propensity score variables for the number of specified match groups;
				%GOTO EXIT_WITH_ERROR;
			%END;
				
			f3_ps&group_num = &ps_var;
		%END;

		/* keep only the number of propensity scores needed */
		KEEP f1_patient_id f2_expcat f3_ps1 - f3_ps%EVAL(&match_num_groups-1);
	RUN;

	%LET match_file_name = &working_directory.&in_dataset..txt;
	PROC EXPORT DATA=match_Export
		OUTFILE="&match_file_name"
		DBMS=TAB REPLACE;
	RUN;

	DATA _null_;
		DECLARE JavaObj match("org/drugepi/match/Match");
		LENGTH version $ 100;
		match.getStaticStringField("version", version);
		put version;
		
		match.exceptionDescribe(1);

		match.callVoidMethod("initMatch", "&match_type", "&match_num_groups");

		%DO group_num = 1 %TO &match_num_groups;
			match.callVoidMethod("addMatchGroup", "%SCAN(&exp_groups, &group_num)");
		%END;
		match.setStringField("outfilePath", "&working_directory.matches.txt");
		match.callVoidMethod("addPatients", "&match_file_name");

	/* DATA set continues in MatchFinish */
	
	%GOTO EXIT_MACRO;
		
	%EXIT_WITH_ERROR: ;
		%LET match_Error = 1;
		%PUT "ERROR: Match failed.  See the log above for more information.";
	
	%EXIT_MACRO: ; 	
%mend;


%macro match_MatchFinish(in_dataset, out_dataset, working_directory, 
				       var_patient_id, var_exposure, var_ps);

	%LET match_JavaError = 1;

		/* continues DATA step from MatchPrep */

		match.callVoidMethod("run");

		/* Check for exception */ 
		LENGTH e 8;
		rc = match.ExceptionCheck(e); 
		IF NOT e AND _ERROR_ = 0 THEN DO;
			CALL SYMPUT("match_JavaError", 0);
			match.delete();
		END;
	RUN;
	%IF &match_JavaError > 0 %THEN %GOTO EXIT_WITH_ERROR;
	RUN;

	PROC IMPORT OUT=work.match_Import
			DATAFILE= "&working_directory.matches.txt" 
			DBMS=TAB REPLACE;
			GETNAMES=YES;
			DATAROW=2; 
	RUN;

	PROC SORT DATA=&in_dataset;
		BY &var_patient_id;
	RUN;
	
	PROC SORT DATA=work.match_Import;
		BY pat_id;
	RUN;
	
	DATA &out_dataset;
		MERGE &in_dataset(IN=in1)
			  match_Import(IN=in2 RENAME=(pat_id=&var_patient_id));
		BY &var_patient_id;
		
		IF in1 AND in2;
	RUN;
	
	%GOTO EXIT_MACRO;
		
	%EXIT_WITH_ERROR: ;
		%LET match_Error = 1;
		%PUT "ERROR: Match failed.  See the log above for more information.";
	
	%EXIT_MACRO: ; 	
%mend;


%macro match_GreedyMatch(in_dataset, out_dataset, working_directory, 
				       	var_patient_id = id, var_exposure = exp, var_ps = ps, 
				       	exp_groups = 0 1, start_digit = 5, end_digit = 1, ratio=1, fixed_ratio=0);

	%match_MatchPrep(greedy, &in_dataset, &out_dataset, &working_directory,
						var_patient_id = &var_patient_id, var_exposure = &var_exposure, exp_groups = &exp_groups,
						var_ps = &var_ps);

	/* set algorithm-specific parameters */
	match.setIntField("startDigit", &start_digit);
	match.setIntField("endDigit", &end_digit);
	match.setIntField("matchRatio", &ratio);
	match.setIntField("fixedRatio", &fixed_ratio);

	%match_MatchFinish(&in_dataset, &out_dataset, &working_directory,
						var_patient_id = &var_patient_id, var_exposure = &var_exposure, 
						var_ps = &var_ps);
%mend;


%macro match_NearestNeighborMatch(in_dataset, out_dataset, working_directory, 
				       var_patient_id = id, var_exposure = exp, 
				       exp_groups = 1 0, var_ps = ps, caliper=0.05, ratio=1, fixed_ratio=0,
				       balanced=0);

	%IF &balanced = 0 %THEN %LET match_type = nn; %ELSE %LET match_type = balanced_nn;
	%PUT &match_type;

	%match_MatchPrep(&match_type, &in_dataset, &out_dataset, &working_directory,
						var_patient_id = &var_patient_id, var_exposure = &var_exposure, 
						exp_groups = &exp_groups, var_ps = &var_ps);

	/* set algorithm-specific parameters */
	match.setDoubleField("caliper", &caliper);
	match.setIntField("matchRatio", &ratio);
	match.setIntField("fixedRatio", &fixed_ratio);
	
	%match_MatchFinish(&in_dataset, &out_dataset, &working_directory,
						var_patient_id = &var_patient_id, var_exposure = &var_exposure, 
						var_ps = &var_ps);
%mend;


%macro match_MakeNWayMatchedCohort(pat_id, master,
								 out_dataset = work.matched_cohort,
								 ref_indicator = 0, 
								 exp_indicator = 1,
								 match_cohort_1 = ,
								 match_cohort_2 = ,
								 match_cohort_3 = ,
								 match_cohort_4 = ,
								 match_cohort_5 = 
								);

	%LET num_cohorts = 0;
	%DO i = 1 %TO 5;
		%IF &&match_cohort_&i ^=  %THEN %LET num_cohorts = &i;
	%END;
	%PUT num_coh = &num_cohorts;

	PROC SQL;
		CREATE TABLE all_ref_pts AS
		SELECT DISTINCT &pat_id, 1 AS cohort_num
		FROM &match_cohort_1
		WHERE STRIP(PUT(group_indicator, $50.)) = "&ref_indicator"

		%DO i = 2 %TO &num_cohorts;
			UNION ALL
			SELECT DISTINCT &pat_id, &i AS cohort_num 
			FROM &&match_cohort_&i
			WHERE STRIP(PUT(group_indicator, $50.)) = "&ref_indicator"
		%END;
		;

		/* patients in all match cohorts */
		CREATE TABLE common_ref_pts AS
		SELECT DISTINCT &pat_id 
		FROM all_ref_pts
		GROUP BY &pat_id
		HAVING COUNT(*) = &num_cohorts;

		%DO i = 1 %TO &num_cohorts;
			CREATE TABLE filtered_matches_&i as
			SELECT ref.set_num, ref.&pat_id as ref_pat_id, exp.&pat_id AS exp_pat_id 
			FROM &&match_cohort_&i ref,
				 &&match_cohort_&i exp
			WHERE ref.set_num = exp.set_num AND
				  STRIP(PUT(ref.group_indicator, $50.)) = "&ref_indicator" AND
				  STRIP(PUT(exp.group_indicator, $50.)) = "&exp_indicator" AND
				  ref.&pat_id IN (SELECT &pat_id FROM common_ref_pts);
		%END;

		CREATE TABLE &out_dataset AS
		SELECT *
		FROM &master 
		WHERE &pat_id IN 
			(SELECT &pat_id FROM common_ref_pts
			%DO i = 1 %TO &num_cohorts;
				UNION ALL
				SELECT exp_pat_id FROM filtered_matches_&i
			%END;
		);				
	QUIT;
%mend;


