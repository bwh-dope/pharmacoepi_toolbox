/**
 * 
 */
package org.drugepi.hdps;

import static org.junit.Assert.assertEquals;

import java.io.*;
import java.util.*;

import org.apache.commons.lang.RandomStringUtils;
import org.drugepi.hdps.storage.HdpsVariable;
import org.drugepi.hdps.storage.comparators.*;
import org.drugepi.util.TabDelimitedFileReader;
import org.junit.*;

/**
 * @author jeremy
 * 
 */
public class HdpsTest {
	private String dataDirectory;
	private static String tempDirectoryBase;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		tempDirectoryBase = "/Users/jeremy/Desktop/hdps_output/"
			+ RandomStringUtils.randomAlphabetic(8);
	}

	@Before
	public void setUp() throws Exception
	{
		dataDirectory = "/Users/jeremy/Documents/Windows Shared/Data/projects/hdps_java/";
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	public String testHdps(String dataDirectory,
			String tempDirectoryBase, String hdpsType, int zeroCellCorrection,
			String rankingMethod) throws Exception {

		System.out
		.printf("TEST: Checking variable selection.  Mode = %s, Zero Cell = %d, Ranking = %s\n",
				hdpsType, zeroCellCorrection, rankingMethod);

		Properties properties = new Properties();
	    properties.load(new FileInputStream("testing.properties"));
		
		String tempDirectory = String.format("%s/%s_ZC%d_%s",
				tempDirectoryBase, hdpsType, zeroCellCorrection, rankingMethod);
		File f = new File(tempDirectory);
		if (!f.exists()) {
			f.mkdirs();
		}

		Hdps hdps = new org.drugepi.hdps.Hdps();
		hdps.k = 500;
		hdps.topN = 200;

		hdps.variableRankingMethod = rankingMethod;

		hdps.doFullOutput = 1;
		hdps.doSparseOutput = 0;

		hdps.tempDirectory = tempDirectory;
		hdps.inferServiceIntensityVars = 0;

		hdps.fullOutputFilename = "output_full_cohort.txt";

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
			"SELECT ssn, prcdr, service_start_dt FROM nsaid_ambproc");

			hdps.addDimension("Hosp Proc", null, null, null, null,
			"SELECT ssn, prcdr, service_start_dt FROM hdps.nsaid_hospproc");

			hdps.addDimension("Hosp DX", null, null, null, null,
			"SELECT ssn, dx3, service_start_dt FROM hdps.nsaid_hospdx");

			hdps.addDimension("NH DX", null, null, null, null,
			"SELECT ssn, dx3, service_start_dt FROM hdps.nsaid_nhdx");

			hdps.addDimension("MD DX", null, null, null, null,
			"SELECT ssn, dx3, service_start_dt FROM hdps.nsaid_mddx");

			hdps.addDimension("MD Proc", null, null, null, null,
			"SELECT ssn, prcdr, service_start_dt FROM hdps.nsaid_mdproc");

			hdps.addDimension("Drugs", null, null, null, null,
			"SELECT ssn, generic, service_start_dt FROM hdps.nsaid_drugs");

			hdps.addDimension("Amb DX", null, null, null, null,
			"SELECT ssn, dx3, service_start_dt FROM hdps.nsaid_ambdx");
		}

		hdps.run();

		return tempDirectory;
	}

	private List<HdpsVariable> buildVarsList(String dir, Comparator<HdpsVariable> sortBy) 
	throws Exception
	{
		TabDelimitedFileReader reader = new TabDelimitedFileReader(dir + 
		"/output_all_vars.txt");
		String row[];

		List<HdpsVariable> varsList = new ArrayList<HdpsVariable>();

		while ((row = reader.getNextRow()) != null) {
			HdpsVariable varA = new HdpsVariable(null, row);

			if (varA.selectedForPs)
				varsList.add(varA);
		}

		reader.close();

		// can sort by bias or exposure assoc -- doesn't matter
		// as long as both lists should sort the same (and they should)
		Collections.sort(varsList, sortBy);

		return varsList;
	}

	private void checkVarsFile(String dirA, String dirB, String rankingMethod)
	throws Exception {

		Comparator<HdpsVariable> sortBy;

		if (rankingMethod.equalsIgnoreCase(Hdps.RANKING_METHOD_BIAS))
			sortBy = new HdpsVariableReverseBiasComparator();
		else
			sortBy = new HdpsVariableReverseExposureAssociationComparator();

		List<HdpsVariable> listA = buildVarsList(dirA, sortBy);
		List<HdpsVariable> listB = buildVarsList(dirB, sortBy);

		assertEquals(listA.size(), listB.size());

		double biasSumA = 0;
		double biasSumB = 0;
		for (int i = 0; i < listA.size(); i++) {
			HdpsVariable varA = listA.get(i);
			HdpsVariable varB = listB.get(i);

			biasSumA += varA.biasRankingVariable;
			biasSumB += varB.biasRankingVariable;

			assertEquals(varA.code.codeString, varB.code.codeString);
			System.out.printf("%s -- %s\n", varA.code.codeString, varB.code.codeString);

			assertEquals(new Double(varA.numEvents), 
					new Double(varB.numEvents));
			assertEquals(new Double(varA.c1NumEvents), 
					new Double(varB.c1NumEvents));
			assertEquals(new Double(varA.pt_e1), 
					new Double(varB.pt_e1));
			assertEquals(new Double(varA.pt_e0), 
					new Double(varB.pt_e0));
			assertEquals(new Double(varA.c1), 
					new Double(varB.c1));
			assertEquals(new Double(varA.c0), 
					new Double(varB.c0));
			assertEquals(new Double(varA.zBiasScore), 
					new Double(varB.zBiasScore));

			if (rankingMethod.equalsIgnoreCase(Hdps.RANKING_METHOD_BIAS)) {
				assertEquals(new Double(varA.bias), 
						new Double(varB.bias));
				assertEquals(new Double(varA.biasRankingVariable), 
						new Double(varB.biasRankingVariable));
				assertEquals(new Double(biasSumA), new Double(biasSumB));
			} else {
				assertEquals(new Double(varA.expAssocRankingVariable), 
						new Double(varB.expAssocRankingVariable));
				assertEquals(new Double(varA.rrCe), 
						new Double(varB.rrCe));
			}
		}
	}

	private void checkCohortFile(String dirA, String dirB)
	throws Exception {
		TabDelimitedFileReader reader = 
			new TabDelimitedFileReader(dirA + "/output_full_cohort.txt");
		
		String row[];

		long totalOnesInA = 0;
		final String one = "1";
		
		while ((row = reader.getNextRow()) != null) {
			for (int i = 1; i < row.length; i++) 
				if (row[i].equals(one))
					totalOnesInA++;
		}
		
		reader = 
			new TabDelimitedFileReader(dirB + "/output_full_cohort.txt");
		
		long totalOnesInB = 0;
		
		while ((row = reader.getNextRow()) != null) {
			for (int i = 1; i < row.length; i++) 
				if (row[i].equals(one))
					totalOnesInB++;
		}

		assertEquals(totalOnesInA, totalOnesInB);
	}


	@Test
	public void testNoCorrBias() 
	throws Exception {
		String localResults = testHdps(dataDirectory, tempDirectoryBase,
				"local", 0, Hdps.RANKING_METHOD_BIAS);
		String dbResults = testHdps(dataDirectory, tempDirectoryBase, "db", 0,
				Hdps.RANKING_METHOD_BIAS);
		checkVarsFile(localResults, dbResults, Hdps.RANKING_METHOD_BIAS);
		checkCohortFile(localResults, dbResults);
	}

	@Test
	public void testNoCorrExp() 
	throws Exception {
		String localResults = testHdps(dataDirectory, tempDirectoryBase, "local", 0,
				Hdps.RANKING_METHOD_EXP);
		String dbResults = testHdps(dataDirectory, tempDirectoryBase, "db", 0,
				Hdps.RANKING_METHOD_EXP);
		checkVarsFile(localResults, dbResults, Hdps.RANKING_METHOD_EXP);
	}	

	@Test
	public void testCorrExp() 
	throws Exception {
		String localResults = testHdps(dataDirectory, tempDirectoryBase, "local", 1,
				Hdps.RANKING_METHOD_EXP);
		String dbResults = testHdps(dataDirectory, tempDirectoryBase, "db", 1,
				Hdps.RANKING_METHOD_EXP);
		checkVarsFile(localResults, dbResults, Hdps.RANKING_METHOD_EXP);
	}	

	@Test
	public void testCorrBias() 
	throws Exception {
		String localResults = testHdps(dataDirectory, tempDirectoryBase, "local", 1,
				Hdps.RANKING_METHOD_BIAS);
		String dbResults = testHdps(dataDirectory, tempDirectoryBase, "db", 1, Hdps.RANKING_METHOD_BIAS);
		checkVarsFile(localResults, dbResults, Hdps.RANKING_METHOD_BIAS);
	}
}


