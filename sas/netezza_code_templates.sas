/* *****************************************************************	*/
/* GENERAL -- PLEASE DO ONCE BEFORE BEGINNING	 						*/
/* ***************************************************************** 	*/
/*
1.  Create a file in your home directory called netezza_connect.sas.
2.  Place the code below in that file.
3.  Set the file to be only readable by you:
    chmod 600 netezza_connect.sas
*/
%GLOBAL DB_SERVER;
%LET DB_SERVER = "dope-twinfin.partners.org";

%GLOBAL DB_USERNAME;
%LET DB_USERNAME = [USERNAME];

%GLOBAL DB_PASSWORD;
%LET DB_PASSWORD = [PASSWORD];

%GLOBAL nz_connection_string;
%LET nz_connection_string = server=&DB_SERVER user=&DB_USERNAME password=&DB_PASSWORD ;



/* ***************************************************************** */
/* ***************************************************************** */
/* ***************************************************************** */
%INCLUDE "netezza_connect.sas";
%GLOBAL DB_NAME;
%LET DB_NAME = [DATABASE NAME ON NETEZZA];


/* ***************************************************************** */
/* CONNECT TO A NETEZZA DATABASE VIA LIBNAME */
/* ***************************************************************** */
LIBNAME netezzadb NETEZZA &nz_connection_string database=&DB_NAME;


/* *****************************************************************	*/
/* CONNECT TO A NETEZZA DATABASE DIRECTLY IN PROC SQL 					*/
/* (Often faster than running PROC SQL on a Netezza LIBNAME)			*/
/* (No SAS functions allowed in SQL code)								*/
/* *****************************************************************	*/
PROC SQL;
	CONNECT TO netezza (&nz_connection_string database=&DB_NAME);

	EXECUTE (
		/* SQL STATEMENTS HERE */
		SELECT 1;
		SELECT 2;
		SELECT 3;
	) BY NETEZZA;
QUIT;


/* ***************************************************************** 	*/
/* RUN NETEZZA-BASED HD-PS 												*/
/* ***************************************************************** 	*/

%INCLUDE "/path/to/hdmacros.sas";

/* STEP 1: Initialize the HD macros */
%InitHDMacros(
	/* required parameters */
	var_patient_id				= [PATIENT_ID],
	var_exposure               	= [EXPOSURE],
	var_outcome                	= [OUTCOME],
	vars_demographic           	= [DEMOGRAPHIC VARS],

	path_temp_dir				= [SPACE FOR TEMPORARY FILES],
	path_jar_file				= [PATH TO pharmacoepi_v3.jar FILE],

	selection_mode				= DB,
	db_url						= jdbc:netezza://&DB_SERVER/&DB_NAME,
	db_username					= &DB_USERNAME,
	db_password					= &DB_PASSWORD,

	/* common parameters that have defaults */
	vars_force_categorical     	= [NUMERIC VARIABLES TO TREAT AS CATEGORICAL, SUCH AS YEAR],
	vars_predefined 	    	= [PREDEFINED VARIABLES],
	top_n                       = [N; default=200],
	k                           = [k; default=500],
	ranking_method	 			= [BIAS, EXP_ASSOC, OR OUTCOME_ASSOC; default=BIAS],

	output_cohort				= [NAME OF COHORT WITH NEWLY CREATED VARIABLES],

	/* less common optional parameters */
	result_estimates			= [default = result_estimates],
	result_diagnostic			= [default = result_diagnostic],

	analysis_num 				= [REGISTERED hdpharmcoepi.org ANALYSIS NUMBER],
	upload_results 				= [default = 0],

	infer_service_intensity_vars= [default = 0],
	exp_zero_cell_corr 			= [default = 0],
	outcome_zero_cell_corr 		= [default = 0],
	
	score_type_1				= [default = 1],
	score_type_2				= [default = 1],
	score_type_3				= [default = 1],
	score_type_4				= [default = 1],
	score_type_5				= [default = 1],
	
	outcome_model_deciles		= [default = 1],
	outcome_model_matched		= [default = 1],


	/* required study definition parameters */

	/* name of the SAS dataset with the cohort */
	input_cohort            	= [SAS DATASET],
	/* name of the Netezza table with the cohort */
	/* note: if the SAS dataset is a Netezza-based LIBNAME, then this may *
	/*       be the same physical table as input_cohort */
	input_cohort_table			= [NETEZZA TABLE],

	/* dimension tables and fields */
	input_dim1	= [DIM 1 NETEZZA TABLE]    [DIM 1 NETEZZA FIELD],
	input_dim2	= [DIM 2 NETEZZA TABLE]    [DIM 2 NETEZZA FIELD],
	/* ... */
);

/* STEP 2: Do variable selection -- runs the Netezza-based variable selection procedure */
%DoHDVariableSelection;

/* OPTIONAL STEPS */

/* Estimate hd-PS */
%EstimateHDPS([NAME OF OUTPUT DATASET]);
/* Estimate hd-PS-based outcome models */
%RunOutcomeModels([NAME OF OUTPUT DATASET], ps);

/* Estimate hd-DRS */
%EstimateHDDRS([NAME OF OUTPUT DATASET]);
/* Estimate hd-DRS-based outcome models */
%RunOutcomeModels([NAME OF OUTPUT DATASET], drs);


