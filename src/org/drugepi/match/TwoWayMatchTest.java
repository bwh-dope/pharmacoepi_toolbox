/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.match;

import static org.junit.Assert.*;

import org.drugepi.match.Match.MatchType;
import org.drugepi.util.TabDelimitedFileReader;
import org.junit.*;

public class TwoWayMatchTest {
	private String outfilePath = String.format("/Users/jeremy/Desktop/match_output.txt"); 
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	
	private void checkOutput(int caseNum)
	throws Exception
	{
		TabDelimitedFileReader matchOutputReader = new TabDelimitedFileReader(this.outfilePath);
		TabDelimitedFileReader answerReader = new TabDelimitedFileReader("testing/match_test_2way_check.txt");
		
		// toss the first rows
		answerReader.getNextRow();
		matchOutputReader.getNextRow();
		
		String[] answerRow;
		String[] outputRow;
		while ((answerRow = answerReader.getNextRow()) != null) {
			if (answerRow[0].equals(Integer.toString(caseNum))) {
				outputRow = matchOutputReader.getNextRow();
				assertNotNull(outputRow);
				assertEquals(answerRow[1], outputRow[0]);
				assertEquals(answerRow[2], outputRow[1]);
			}
		}
			
		matchOutputReader.close();
		answerReader.close();
	}
	
	private void doTwoWayMatchingTest(MatchType matchType, int matchRatio, int fixedRatio, int parallel)
	throws Exception
	{
		Match g = new Match();
		g.initMatch(matchType, 2);
		g.matchRatio = matchRatio;
		g.fixedRatio = fixedRatio;
		g.parallelMatchingMode = parallel;

		g.outfilePath = outfilePath;

		String infileName = "testing/match_test_2way.txt";
		g.addMatchGroup("0");
		g.addMatchGroup("1");
		g.addPatients(infileName);
		g.run();
		
	}

	@Test
	public void twoWayMatchTest1()
	throws Exception 
	{
		this.doTwoWayMatchingTest(MatchType.NEAREST_NEIGHBOR, 2, 0, 1);
		this.checkOutput(1);
	}

	@Test
	public void twoWayMatchTest2()
	throws Exception 
	{
		this.doTwoWayMatchingTest(MatchType.NEAREST_NEIGHBOR, 2, 0, 1);
		this.checkOutput(1);
	}

	@Test
	public void twoWayMatchTest3()
	throws Exception 
	{
		this.doTwoWayMatchingTest(MatchType.NEAREST_NEIGHBOR, 2, 0, 1);
		this.checkOutput(1);
	}

	@Test
	public void twoWayMatchTest4()
	throws Exception 
	{
		this.doTwoWayMatchingTest(MatchType.NEAREST_NEIGHBOR, 2, 0, 1);
		this.checkOutput(1);
	}

	@Test
	public void twoWayMatchTest5()
	throws Exception 
	{
		this.doTwoWayMatchingTest(MatchType.NEAREST_NEIGHBOR, 2, 0, 1);
		this.checkOutput(1);
	}
}
