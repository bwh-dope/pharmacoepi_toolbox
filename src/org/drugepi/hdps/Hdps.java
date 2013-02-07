/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

package org.drugepi.hdps;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.drugepi.PharmacoepiTool;
import org.drugepi.hdps.db.HdpsDbController;
import org.drugepi.hdps.local.HdpsLocalController;
import org.drugepi.util.RowReader;


/**
 * Implementation of the hd-PS algorithm.
 * <p>
 * The algorithm is fully described in:
 * Schneeweiss S, Rassen JA, Glynn RJ, Avorn J, Mogun H, Brookhart MA. High-dimensional propensity score 
 * adjustment in studies of treatment effects using health care claims data. <I>Epidemiology</I> 2009;20:512-22.
 * <p>
 * Version 2 of the algorithm is described in Rassen JA, Schneewiss S, et al.:
 * Rassen JA, Glynn RJ, Brookhart MA, Schneeweiss S. Observed performance of high-dimensional propensity 
 * score analyses of treatment effects in small samples.  In submission 2010.
 * <p>
 * This class can be used alone or in conjunction with SAS or R.  For full documentation, please see
 * the drugepi.org or hdhub.org web sites.
 * <p>
 * Parameters to the algorithm can be specified by setting public variables, or by setter functions.
 * Setter functions that take doubles rather than ints are provided for rJava compatibility.
 * <p>
 * Patient information is specified through the {@link org.drugepi.PharmacoepiTool} addPatient method.
 * Patient data must be specified in the following order:
 * <ul>
 * 	<li>patient ID
 * 	<li>exposure status (1=exposed, 0=unexposed)
 * 	<li>outcome status (1=outcome; 0=no outcome)
 * </ul>
 * <p>
 * Dimension information is specified through the {@link #addDimension(String, RowReader)} or 
 * {@link #addDimension(String, String, String, String, String, String)} methods.
 * Dimension data must be specified in the following order:
 * <ul>
 * 	<li>patient ID
 * 	<li>code (e.g., diagnosis code, drug name, etc.)
 * 	<li>date that code occurred (value currently ignored; can leave as 0)
 * </ul>
 * 
 * @author Jeremy A. Rassen
 * @version 2.1.0
 * 
 */
public class Hdps extends PharmacoepiTool 
{
	/**
	 * Description string.
	 */
	public final String description = "High-Dimensional Propensity Score Variable Selection Module";

	/**
	 * Constant string indicating local mode operation. 
	 */
	public static final String hdpsModeLocal = "LOCAL";

	/**
	 * Constant string indicating database mode operation. 
	 */
	public static final String hdpsModeDB = "DB";
	
	/**
	 * Constant string indicating bias-based variable ranking. 
	 */
	public static final String RANKING_METHOD_BIAS = "BIAS";

	/**
	 * Constant string indicating exposure-association-based variable ranking. 
	 */
	public static final String RANKING_METHOD_EXP = "EXP_ASSOC";
	
	/**
	 * Constant string indicating outcome-association-based variable ranking. 
	 */
	public static final String RANKING_METHOD_OUTCOME = "OUTCOME_ASSOC";
	
	/**
	 * Constant string indicating no particular variable ranking; this
	 * is meant to be used when particular variables are selected. 
	 */
	public static final String RANKING_METHOD_NONE = "NO_RANKING";
	
	/**
	 * Constant string indicating dichotomous/binary outcome type. 
	 */
	public static final String OUTCOME_TYPE_DICHOTOMOUS = "DICHOTOMOUS";

	/**
	 * Constant string indicating dichotomous/binary outcome type. 
	 */
	public static final String OUTCOME_TYPE_BINARY = "BINARY";

	/**
	 * Constant string indicating count outcome type. 
	 */
	public static final String OUTCOME_TYPE_COUNT = "COUNT";
	
	/**
	 * Constant string indicating continuous outcome type. 
	 */
	public static final String OUTCOME_TYPE_CONTINUOUS = "CONTINUOUS";


	/**
	 * The <i>n</i> most prevalent empirical covariates to consider from each dimension of data.  Default is 200.
	 */
	public int topN;

	/**
	 * The number of empirical covariates to include in the resulting propensity score.  Default is 500.
	 */
	public int k;
	

	/**
	 * The minimum number of times a variable has to appear in order to be considered for inclusion.
	 * Note that in all previous versions, this value was 100.  It now defaults to 0.
	 */
	public int frequencyMin;

	/**
	 * The type of variable ranking to do: BIAS, EXPOSURE_ASSOC, OUTCOME_ASSOC.  Default is BIAS.
	 */
	public String variableRankingMethod;

	/**
	 * An indicator for whether to screen variables based on their confounder/exposure association only.  Default is 0.
	 * NOTE: Deprecated.  Should now be specified as EXPOSURE_ASSOC ranking method.
	 */
	public int exposureOnlyScreen;
	
	/**
	 * An indicator for whether to screen variables with a zero correction added to each cell 
	 * in the confounder/outcome 2x2 table.  Recommended when the number of exposed outcomes 
	 * is less than 150.  Default is 0.
	 */
	public int useOutcomeZeroCellCorrection;

	/**
	 * An indicator for whether to include variables that describe the intensity of service 
	 * usage in each dimension. 
	 */
	public int inferServiceIntensityVars;
	
	/**
	 * An indicator for whether to create time interactions to "discount" variables that
	 * occur farther in the past.  EXPERIMENTAL ONLY. 
	 */
	public int createTimeInteractions;
	
	/**
	 * An indicator for whether to create profile scores to determine whether a variable  
	 * is gaining or losing frequency.  EXPERIMENTAL ONLY. 
	 */
	public int createProfileScores;
	
	/**
	 * The type of the outcome variable: DICHTOTOMOUS (or BINARY), or CONTINUOUS.  Default is DICHOTOMOUS.
	 */
	public String outcomeType;
	
	
	/**
	 * An indicator for whether the algorithm should output a full cohort with all variable information
	 * for all patients.  Default is 1.
	 */
	public int doFullOutput;
	
	/**
	 *  The name of the full output file, if full output has been requested.  Default is hdps_cohort.txt.
	 */
	public String fullOutputFilename = "output_full_cohort.txt";
	
	
	/**
	 * An indicator for whether the algorithm should output a sparse cohort with a variable list
	 * for each patient.  Default is 0.
	 */
	public int doSparseOutput;
	
	/**
	 *  The name of the sparse output file, if sparse output has been requested.  Default is hdps_sparse.txt.
	 */
	public String sparseOutputFilename = "output_sparse_cohort.txt";

	
	/**
	 * The path to a directory where the hd-PS algorithm can store temporary files.  There should be 
	 * enough space in the directory to hold a second copy of the input cohort and each of the dimensions.
	 * No default; must be specified.
	 */
	public String tempDirectory;
	
	
	/**
	 * Via a method below, there is also public access to the list of varibles to output.
	 * Users may specify hash codes of variables to include in any output cohort; these 
	 * variables will be in addition to any others normally output.
	 * 
	 */

	
	/*
	 * ===========================================
	 * PUBLIC VARIABLES FOR DB MODE 
	 * ===========================================
	 */
	public String dbDriverClass;
	public String dbUrl;
	public String dbUsername;
	public String dbPassword;
	
	/**
	 * Whether to maintain the output tables after the run.
	 */
	public int dbKeepOutputTables;
	
	
	/*
	 * ===========================================
	 * PROTECTED VARIABLES
	 * ===========================================
	 */
	protected static final int MAX_DIMENSIONS = 100;
	protected int numDimensions = 0;
	protected String mode;
	protected List<String> requestedVariables;

	private HdpsController hdpsController;
	
	/**
	 * Constructor for the hd-PS class using default values for all parameters.
	 */
	public Hdps()
	{
		super();
		this.k = 500;
		this.topN = 200;
		this.frequencyMin = 0;
		this.exposureOnlyScreen = 0;
		this.variableRankingMethod = RANKING_METHOD_BIAS;
		this.outcomeType = OUTCOME_TYPE_DICHOTOMOUS;
		this.useOutcomeZeroCellCorrection = 0;
		this.inferServiceIntensityVars = 0;
		this.doFullOutput = 1;
		this.doSparseOutput = 0;
		this.dbKeepOutputTables = 0;
		this.requestedVariables = new ArrayList<String>();
		try {
			this.setMode(Hdps.hdpsModeLocal);
		} catch (Exception e) {
			// no exceptions possible 
		}
	}
	
	/**
	 * Constructor for the hd-PS class using default values for all parameters.
	 * 
	 * @param tempDirectory		tempDirectory parameter.
	 */
	public Hdps(String tempDirectory)
	{
		this();
		this.tempDirectory = tempDirectory;
	}
	
	public void addPatients(RowReader reader)
	throws Exception 
	{
		this.hdpsController.addPatients(reader);
	}
		
	/**
	 * Add a dimension to the hd-PS run, with dimension data stored in a tab-delimited file.
	 * 
	 * @param description	Description of the dimension.
	 * @param filePath		Path of the dimension data file.  The file should contain three columns:
	 * 						patient_id, code, date.  Columns must be stored in this order.
	 * @throws Exception	
	 */
	public void addDimension(String description, String filePath)
	throws Exception
	{
		numDimensions++;
		
		if (numDimensions > Hdps.MAX_DIMENSIONS) 
			throw new HdpsException("Too many dimensions specified.");
		
		this.hdpsController.addDimension(description, filePath);
	}
	
	/**
	 * Add a dimension to the hd-PS run, with dimension data stored in a database.
	 * 
	 * @param description	Description of the dimension.
	 * @param dbDriverClass	Name of the database driver, or null to use the globally-specified default.
	 * @param dbURL			JDBC URL of the database, or null to use the globally-specified default
	 * @param dbUser		Database user name, or null to use the globally-specified default.
	 * @param dbPassword	Database user's password, or null to use the globally-specified default.
	 * @param dbQuery		The query that will result in the dimension data.  The query should return 
	 * 						three columns: patient_id, code, date.  Columns must be returned in this order.
	 * @throws Exception
	 */
	public void addDimension(String description, String dbDriverClass, 
							String dbURL, String dbUser, String dbPassword,
							String dbQuery)
	throws Exception
	{
		numDimensions++;
		
		if (numDimensions > Hdps.MAX_DIMENSIONS) 
			throw new HdpsException("Too many dimensions specified.");
		
		this.hdpsController.addDimension(description, 
				(dbDriverClass == null ? this.dbDriverClass : dbDriverClass), 
				(dbURL == null ? this.dbUrl : dbURL), 
				(dbUser == null ? this.dbUsername : dbUser), 
				(dbPassword == null ? this.dbPassword : dbPassword), 
				dbQuery);
	}
	
	
	/**
	 * Users may specify hash codes of variables to include in any variables to output.
	 * These variables will be in addition to any variables normally include in the output
	 * cohorts.
	 * 
	 * @param hashValue
	 */
	public void addRequestedVariable(String hashValue)
	{
		if (hashValue != null)
			this.requestedVariables.add(hashValue);
	}
	
	/*
	 * ===========================================
	 * PROTECTED METHODS
	 * ===========================================
	 */
	
	/**
	 * Begin execution of the hd-PS algorithm.
	 * 
	 * @throws Exception
	 */
	public void run()
    throws Exception
    {
		this.hdpsController.run();
    }
	
	/*
	 * ===========================================
	 * GETTERS AND SETTERS
	 * ===========================================
	 */
	
    /**
     * @see #topN 
     */
    public int getTopN() {
		return topN;
	}

	/**
     * @see #topN 
	 */
	public void setTopN(int topN) {
		this.topN = topN;
	}

	/**
     * @see #k 
	 */
	public int getK() {
		return k;
	}

	/**
     * @see #k 
	 */
	public void setK(int k) {
		this.k = k;
	}

	/**
     * @see #exposureOnlyScreen 
	 */
	public int getExpsoureOnlyScreen() {
		return exposureOnlyScreen;
	}

	/**
     * @see #exposureOnlyScreen 
	 */
	public void setExposureOnlyScreen(int exposureOnlyScreen) {
		this.exposureOnlyScreen = exposureOnlyScreen;
	}

	/**
     * @see #tempDirectory
	 */
	public String getTempDirectory() {
		return tempDirectory;
	}

	/**
     * @see #tempDirectory
	 */
	public void setTempDirectory(String tempDirectory) {
		this.tempDirectory = tempDirectory;
	}

	/**
     * @see #useOutcomeZeroCellCorrection
	 */
	public void setUseOutcomeZeroCellCorrection(int useZeroCellCorrection) {
		this.useOutcomeZeroCellCorrection = useZeroCellCorrection;
	}

	
	/**
     * @see #doFullOutput
	 */
	public int getDoFullOutput() {
		return doFullOutput;
	}

	/**
     * @see #doSparseOutput
	 */
	public int getDoSparseOutput() {
		return doSparseOutput;
	}

	/**
     * @see #doFullOutput
	 */
	public void setDoFullOutput(int doFullOutput) {
		this.doFullOutput = doFullOutput;
	}

	/**
     * @see #doSparseOutput
	 */
	public void setDoSparseOutput(int doSparseOutput) {
		this.doSparseOutput = doSparseOutput;
	}
	
	public int getNumDimensions() {
		return numDimensions;
	}
	
	public void setMode(String mode)
	throws Exception 
	{
		this.mode = mode;
		if (this.mode.equalsIgnoreCase(Hdps.hdpsModeLocal)) {
			this.hdpsController = new HdpsLocalController(this);
		} else if (this.mode.equalsIgnoreCase(Hdps.hdpsModeDB)) {
			this.hdpsController = new HdpsDbController(this);
		} else {
			throw new HdpsException(String.format(
					"Invalid mode %s specified.  Mode must be either LOCAL or DB.", mode));
		}
	}

	public String getVaribleRankingMethod() {
		return variableRankingMethod;
	}

	public void setVaribleRankingMethod(String varibleRankingMethod) {
		this.variableRankingMethod = varibleRankingMethod;
	}

	public int getInferServiceIntensityVars() {
		return inferServiceIntensityVars;
	}

	public void setInferServiceIntensityVars(int inferServiceIntensityVars) {
		this.inferServiceIntensityVars = inferServiceIntensityVars;
	}

	public int getExposureOnlyScreen() {
		return exposureOnlyScreen;
	}

	public int getUseOutcomeZeroCellCorrection() {
		return useOutcomeZeroCellCorrection;
	}

	/**
	 * @return the outcomeType
	 */
	public String getOutcomeType() {
		return outcomeType;
	}

	/**
	 * @param outcomeType the outcomeType to set
	 */
	public void setOutcomeType(String outcomeType) {
		this.outcomeType = outcomeType;
	}

	/**
	 * @return the fullOutputFilename
	 */
	public String getFullOutputFilename() {
		return fullOutputFilename;
	}

	/**
	 * @param fullOutputFilename the fullOutputFilename to set
	 */
	public void setFullOutputFilename(String fullOutputFilename) {
		this.fullOutputFilename = fullOutputFilename;
	}

	/**
	 * @return the sparseOutputFilename
	 */
	public String getSparseOutputFilename() {
		return sparseOutputFilename;
	}

	/**
	 * @param sparseOutputFilename the sparseOutputFilename to set
	 */
	public void setSparseOutputFilename(String sparseOutputFilename) {
		this.sparseOutputFilename = sparseOutputFilename;
	}

	/**
	 * @return the dbDriverClass
	 */
	public String getDbDriverClass() {
		return dbDriverClass;
	}

	/**
	 * @param dbDriverClass the dbDriverClass to set
	 */
	public void setDbDriverClass(String dbDriverClass) {
		this.dbDriverClass = dbDriverClass;
	}

	/**
	 * @return the dbUrl
	 */
	public String getDbUrl() {
		return dbUrl;
	}

	/**
	 * @param dbUrl the dbUrl to set
	 */
	public void setDbUrl(String dbUrl) {
		this.dbUrl = dbUrl;
	}

	/**
	 * @return the dbUsername
	 */
	public String getDbUsername() {
		return dbUsername;
	}

	/**
	 * @param dbUsername the dbUsername to set
	 */
	public void setDbUsername(String dbUsername) {
		this.dbUsername = dbUsername;
	}

	/**
	 * @return the dbPassword
	 */
	public String getDbPassword() {
		return dbPassword;
	}

	/**
	 * @param dbPassword the dbPassword to set
	 */
	public void setDbPassword(String dbPassword) {
		this.dbPassword = dbPassword;
	}
	
	/**
	 * @return the database's patient table name, if one was created
	 */
	public String getDbPatientTableName() {
		if ((this.hdpsController != null) &&
			(this.hdpsController.getClass() ==  HdpsDbController.class)) {
			
			HdpsDbController d = (HdpsDbController) this.hdpsController;
			return d.getPatientTableName();
		}
		return null;
	}
	
	/**
	 * @return the database's variable table name, if one was created
	 */
	public String getDbVarTableName() {
		if ((this.hdpsController != null) &&
			(this.hdpsController.getClass() ==  HdpsDbController.class)) {
			
			HdpsDbController d = (HdpsDbController) this.hdpsController;
			return d.getVarTableName();
		}
		return null;
	}
	
	/**
	 * @return the database's patient/variable table name, if one was created
	 */
	public String getDbPatientVarTableName() {
		if ((this.hdpsController != null) &&
			(this.hdpsController.getClass() ==  HdpsDbController.class)) {
			
			HdpsDbController d = (HdpsDbController) this.hdpsController;
			return d.getPatientVarTableName();
		}
		return null;
	}
	
	public static void main(String[] args) {
		String hdpsType;
		String rankingMethod;
		int zeroCellCorrection;
		
		hdpsType = "db";
		zeroCellCorrection = 0;
		rankingMethod = "BIAS";
		
		String dataDirectory = "/Users/jeremy/Documents/Windows Shared/Data/projects/hdps_java/";
		
		Hdps hdps = new org.drugepi.hdps.Hdps();
		hdps.k = 500;
		hdps.topN = 200;
		hdps.frequencyMin = 0;

		hdps.useOutcomeZeroCellCorrection = zeroCellCorrection;
		hdps.variableRankingMethod = rankingMethod;
		
		hdps.doFullOutput = 1;
		hdps.doSparseOutput = 0;

		hdps.tempDirectory = "/tmp";
		hdps.inferServiceIntensityVars = 1;

		try {
			if (hdpsType == "local") {
				hdps.addPatients(dataDirectory + "patients.txt");

				hdps.addDimension("Amb Proc", dataDirectory + "dim_ambproc.txt");
				hdps.addDimension("Hosp Proc", dataDirectory + "dim_hospproc.txt");
				hdps.addDimension("Hosp DX", dataDirectory + "dim_hospdx.txt");
				hdps.addDimension("NH DX", dataDirectory + "dim_nhdx.txt");
				hdps.addDimension("MD DX", dataDirectory + "dim_mddx.txt");
				hdps.addDimension("MD Proc", dataDirectory + "dim_mdproc.txt");
				hdps.addDimension("Drugs", dataDirectory + "dim_drugs.txt");
				hdps.addDimension("Amb DX", dataDirectory + "dim_ambdx.txt");

			} else if (hdpsType == "db") {
				hdps.setMode("DB");

				Properties properties = new Properties();
			    properties.load(new FileInputStream("testing.properties"));
			    
			    hdps.createProfileScores = 0;
			    hdps.dbKeepOutputTables = 1;

				String dbDriver = properties.getProperty("HDPS_DB_DRIVER");
				String dbURL = properties.getProperty("HDPS_DB_URL");
				String dbUser = properties.getProperty("HDPS_DB_USER");
				String dbPassword = properties.getProperty("HDPS_DB_PASSWORD");

				hdps.addPatients(dbDriver, dbURL, dbUser, dbPassword,
						"SELECT ssn, i_cox, gi_event FROM nsaid_master");

				hdps.dbDriverClass = dbDriver;
				hdps.dbUrl = dbURL;
				hdps.dbUsername = dbUser;
				hdps.dbPassword = dbPassword;
				
				hdps.addDimension("Amb Proc", null, null, null, null,
						"SELECT ssn, prcdr, index - service_start_dt AS days_before_index FROM nsaid_ambproc");

				hdps.addDimension("Hosp Proc", null, null, null, null,
						"SELECT ssn, prcdr, index - service_start_dt AS days_before_index  FROM hdps.nsaid_hospproc");
	
//				hdps.addDimension("Hosp DX", null, null, null, null,
//						"SELECT ssn, dx3, index - service_start_dt AS days_before_index  FROM hdps.nsaid_hospdx");
//	
//				hdps.addDimension("NH DX", null, null, null, null,
//						"SELECT ssn, dx3, index - service_start_dt AS days_before_index  FROM hdps.nsaid_nhdx");
//	
//				hdps.addDimension("MD DX", null, null, null, null,
//						"SELECT ssn, dx3, index - service_start_dt AS days_before_index  FROM hdps.nsaid_mddx");
//	
//				hdps.addDimension("MD Proc", null, null, null, null,
//						"SELECT ssn, prcdr, index - service_start_dt AS days_before_index  FROM hdps.nsaid_mdproc");
//	
//				hdps.addDimension("Drugs", null, null, null, null,
//						"SELECT ssn, generic, index - service_start_dt AS days_before_index  FROM hdps.nsaid_drugs");
//
//				hdps.addDimension("Amb DX", null, null, null, null,
//						"SELECT ssn, dx3, index - service_start_dt AS days_before_index  FROM hdps.nsaid_ambdx");
				
				hdps.addRequestedVariable("029e7b3588b4980b8b9ebcd08fe1e7ec239868c524643834c833ab1a4ea24e9195b784a99298d682c74e2b9b16380e8ba3f7624929f50c222941dead029aeb4f");
				hdps.addRequestedVariable("1aeab07364bb4c32f0a5e80d77e5acb6fc40a645d85c01d297a1ae197b9a5c4315987d62eeccffeff2fc87ca04c401a7a68eb7e0393f8bf3d0a58018e952417f");
				hdps.addRequestedVariable("398b6285ae67716d0891309e8820bdbeedff9c0035c9369d3ff0fb778a07dec5fe474159650f0a98f0bfc6d558a5728f4ec9a9259aaf1a3bd2ddf5478954033e");
				hdps.addRequestedVariable("018f3dcdb9eb28334ae200955d903aa0fc456e6791dd7e99bc109a08a92d008e1ccfddd2d51df6fb9bff8775c5e14dea20b41dc2e123f028df84dc7a634b5133");
				hdps.addRequestedVariable("38d999ac6e663e01f048e405e328192dcb7559d4bd0c16555848213fd9114c003e191e51fc7ac5b0ae1ccdf07962f666723563036b60942b0a698c00898024a2");
				hdps.addRequestedVariable("fab8c704210e49edc25ce41e54231d541b4f90f4eabf1abc4a309966aa75903818e7d224e471ca957d70cd290bc87bef24e0d4ffb6b768d86bd98922d908eb36");
				hdps.addRequestedVariable("c57e911363bf1510a38e8095b2a44bab1e9e0880d3f06152e7b4b77593ff3e6ee2f53abbb07650cc69765065a158da776f61b9fccc10570f23da61555d7e3aee");
				hdps.addRequestedVariable("c679d2e9b027a0dcc0865a479752ef8e838529497b859ba15820d9c7280b7b1bb12adce8392340413521122e7bf83accf7d76df528ee7aff399bc7ad41512728");
				hdps.addRequestedVariable("fe7ec0364c880c9e1608722f81b09826dde2718a098add0c634e5fa7cf0958e57b934a6a42c40144f70763f6652dc07b1bf610b28352af9e7faa429f5887e027");
				hdps.addRequestedVariable("37450e51327b3fe6fe0f3632e48694764eb49e803c63b56cdd8d9dd8b309317c9834ec897b61d665e0ca8c5c544af1f748fda2372bb5576609217f7e4d724585");
				hdps.addRequestedVariable("40fb98adb2b40e0c627bd387a3fa304925cbbd00297743ccd3bdf3369d5c6176aba12f2fbc471c6397bcb4ba56a169081b24a0c6b6109035eb1ff1beb4eb07a3");
				hdps.addRequestedVariable("b1225d136c7efe7b0b5e0ab227ee74d6cc0f674443951506c9d5d3be75c2d8566c2a505f320601639d23c90b53bc05f2348415c84e19e72efadc4336cc9b43d5");
				hdps.addRequestedVariable("89c3d2bbcb3e82a24e056ea894b3baee45847840fc4a01e337d5c84666593013967c4834544d53534397ac507b668f328bc4739179cd8018eb820069173ea8e2");
				hdps.addRequestedVariable("37ed80baff2d232efb562278f40b0510ede689ec3823735f421d0d03b7bd5d997b4da5f86227fddd7398f6333b83cd307bc9389acfd4ccf205ca60d7a874bf1a");
				hdps.addRequestedVariable("f070d003c88b3a2bb9d507e07b06143d0623983f19696bc080137b765ae9ec24467a0728ed9b617d5b417189d76d7b3c5f6d4b766bda2f2ba98224aec5063431");
				hdps.addRequestedVariable("ce66955e263a61424ef1e35556dd83b8a1b60fe393ba3ba29f97dfdfaeb19cb7d2e585b8cb68c53dc0dc7840ef79c80e98d4f6c975f0920cd5bb6afb5fbcc4cb");
				hdps.addRequestedVariable("0b740a3168baee05dbb052613863adb5ee1f175ac2ffe19bf88c6b088a0ad8b5932280ff6abcb699a8a4bfc9d6e791ecbb0ad1e518956360d5ea39a7a52edfe3");
				hdps.addRequestedVariable("f88ab9456b1a81286a13f0ac8bfef171ec62e866424b2b1a2d0bdd91a31365501bdd1adca733189f96560f8467fd6e9b67589baff698869ca72f57018b68bf9a");
				hdps.addRequestedVariable("35f52d3f6ff9a75c334aadfc43426c9fdb615411cc5cc3d1365ec74b995528e770381f3312e1384bd78c35650f2e87c545165242601870a99c42325d412925a2");
				hdps.addRequestedVariable("cacc9b2c9522c2e29f80e743603797f284b438cd295fceafd72c25af618f80905ca31c52761f5465118b46d45141dac605a3f3798a8e52fb449aeacd17e9d711");
				hdps.addRequestedVariable("e4463b77c68210dfc4189ad6053d9bb6a3d0081b5c167933ca6f74b90705c62ec035ca6b04c1114a18654be247533e66837693fb2b10ebda85f4907e1da88e6f");
				hdps.addRequestedVariable("4c1c82b60633d58328a2dffacfd4105e04624e78d786abe68b764a22d7bb905f7873da01fd48d97bfa63886e6a35b710899079a3f8338bd7121aa61b517f250e");
				hdps.addRequestedVariable("ac0c99c60a98e3470b4e17e5da40da9cf8dea782971d03b2b68e149b8a65c6fc761e6accd672e09dd184acca238bf1b0d7f01d7b50e954238768c7a48b119786");
				hdps.addRequestedVariable("2b82201a4141e9efa4a0a4266c30295b73ee9559e741c59b27e989b5dd51f3d56b5864242837264007fa7d9ad8f98c84601820d2ef6c69e966e378b0875fdf1e");
				hdps.addRequestedVariable("67334fbf8258b1c58be85aab039f7bbb8e7895853adcdc79fcaed17a233d1991f917553f8610d9753bacdf760ff63712b373fa21adcc6548fa75783e95b79da3");
				hdps.addRequestedVariable("14603d62402fac2b45d98c69394bd735693c623c95af8a766c77e2165fae833ee36faa7e634c4439ebf9551c78dcb9a6f85af02687fec7954447209ad6c21d04");
				hdps.addRequestedVariable("61a9488a6cb08c0617d89814cfdca828495f977720ffaf20f939f96d225831ee92ddf69c2f72e71c647bfccaa366f899928f193a213ea2fd7203a970c725ddff");
				hdps.addRequestedVariable("47a990fa770a6634afac507735ea69bc12c1c2967450743c069e86352d3a9ca24c9ead5df87549f6349207db3bd99206b814aa642d985c9e65cc6ca0283bf235");
				hdps.addRequestedVariable("5a29647390cdfcfc40e75758481b2d92a227899d2655530f9cf2363f479331299b19169f6afc7bbae53a44d699f6e8666ea90b5dfee51cfab6954426c9c09f4e");
				hdps.addRequestedVariable("cd0210147a7741ec28c49dfbe3c04e0c98d21dd0656a6a7d5e28ec8a3edfbdc93b137355ce1230d4bcfeca2e71068d75172b15b86486f2a96d646748d87780bb");
				hdps.addRequestedVariable("c77d47f3d541270e2db4aafd268fa2894c807f50074780fe30a2599cb6fa567a1f671655f55c7113dc7ec0877d7b3a5b1fb6ff4cc7a3e65e0f7a6fcbd464bd2f");
				hdps.addRequestedVariable("03254152dedaa10da4dd25671ffdca62bfa5c9351da0e8e86ffa9c8692ce1e209632b43b678caaacda71575694c945c54a81500de0e200691637dba08dd8a17b");
				hdps.addRequestedVariable("882013d919b99e44549f113cc8843d7f1cf19edced0360c889be9809a71f37e8264612bdac5c2cb1cf229c8ce77775e3aa3cd2f813002b3f04a0bc49f7789ed7");
				hdps.addRequestedVariable("76f531b635172b4ee44573b4eadca95a79df424b9733b326ad9c066298a88202868c29d1844d4e411b51ef07ae0e8893b1caeb69677f49bc3761b59662404a10");
				hdps.addRequestedVariable("8d508b66ea341fccfc58d93e13079e433fb1e13f7e7a70739a209de984fa85181eea8e400b0eab06887bc495bc2fe0c98b8e3c78d4628969fc948de17e7a2b9d");
				hdps.addRequestedVariable("d3538ee7b3d2a61505230018c2ff90f44663ca2a5f89ae98bfd032031cb58bdec93e8388df9bc1b867780a4ae7318e8ff4f0ea640ab0dd5b40b06d093e003e3c");
				hdps.addRequestedVariable("132bc9da44f47fd2e3497e524ad22bbcc2ecc244f19daee4d7e133b04c3caa5beb6bde1913404a43ce0803876ea83e8aa100b75f58603f5889024a266f0acbf4");
				hdps.addRequestedVariable("f8da36578eac9a628b0c9dfa12f7704ef0ae4010e012d4f2681ae5f0762661140425b863c4f7d682c2ff5bbb4b339d94d1eafe33557898236e2b08f2e635bfb3");
				hdps.addRequestedVariable("bfa748c83af42f68fa35859b6397c237a8b64f63643bb2ad9071066431b5e8854083335194f9b085bdf5ae1b7d6bd113698d8820ca4e197ff80234b8f01fe86a");
				hdps.addRequestedVariable("85ffe9bec9c62ce4cfac72eccf3ea48238080d205a82f92c0c943a0085d0c1867fb5564fe69d113e62dec652b02fb5613847791282def5437be55b0de0fcee25");
				hdps.addRequestedVariable("fc7fe5c775ccc8f43494d931ac3b70d7489620e9768b1683b4ffdf91604e28a22ec7ec4b1db6d522323c9e912c7f105acd9e779703a8cc4406ccfefb75050c01");
				hdps.addRequestedVariable("e37a1ce87c7e8f13040c8a1e77f4388bdc542358074e05576628d7eff529862cd322136bb5e30f49bafa738d5899b36cff76dfd7e9a1cde082ab6fd930213038");
				hdps.addRequestedVariable("bd409491de7aaac51c5e68e6378ce0068ac69e9752b4957fdbf859b0374679c831bad9444c63943a0847285497ddcc0850f2cc74d598858b49b7c97af9d59b79");
				hdps.addRequestedVariable("7116dacb6316b29f06bd4bde81d8e88807661af775521e669b42a3bd0c5568d673aa7835feea0b514c2be14427bbd4493d4591d7fa8cce0de9a41d893c0d8e88");
				hdps.addRequestedVariable("e039232ed1d5bc27d27935d8752b6d48c787640f1aae2fc0173fe57c544744f8877649cf15ed65d1c8223dee9b9b5dbdc24280a6b5f41534b6244212ef6f267a");
				hdps.addRequestedVariable("c4d3057c5cd08f9a8946862561d7823b7bb51413fe51947e5b46629741e1cc6fb71d69143b46b16efad16b6f75565a60ab06b953e989200fd427d5c501dcb702");
				hdps.addRequestedVariable("6bafd8176a60eb527e6ea6bef620d9ad4ac5b0201d1b5334eaedab185426c4e5874d95c142109b7905f55d00b476dfffc19584a1613eb90b69e199635f386523");
				hdps.addRequestedVariable("93345be3d5011633ee00a630e7a16d17352a91b62747dcfa241ff511776f8cd7bc2909b5965907535ee942e51790f91399389c8b9cd509cea878a0bdc687611f");
				hdps.addRequestedVariable("1fa978a910459901e43393829230942a93bb13ae2dc609b2a0e5674844d45523a6c2e1c1080ffa3d8cf0928abafe7dc1a3f164df8ea32b5435f2412c027178ce");
				hdps.addRequestedVariable("f37f05088f4daefe070cf1c08e066c57f30e7465e2d5a70ac1cedae0781205b3760ac4d3eb17167f3b4eab194feb65c759d7dc73b4671ee26a06f06b5995887f");
				hdps.addRequestedVariable("cf34ed9aa667fb9fcf3e7121558e86eeac9837f9c465ca70a5e08298fa7bd11664f2814a2fe64f9f83292bc096613c5e99443c3a081124964c5bddd0d3bfd3a6");
				hdps.addRequestedVariable("f4d5eadb83260fdd16c5ffdb5e1574469583686addb8a713e75d618fa37f49d65d49c9029b8c3cab8bbb956bbad0e6ec62e4a243dd7747a4d1e351b166609281");				
			}

			hdps.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

