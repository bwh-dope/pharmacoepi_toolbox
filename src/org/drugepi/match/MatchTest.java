/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.match;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.*;

public class MatchTest {
	private String tempDirectoryBase;
	private String dataDirectory; 
	private Properties properties;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		this.properties = new Properties();
	    this.properties.load(new FileInputStream("testing.properties"));
	    this.dataDirectory = properties.getProperty("MATCH_DATA_DIRECTORY");
		this.tempDirectoryBase = properties.getProperty("MATCH_TEMP_DIRECTORY") +
				RandomStringUtils.randomAlphabetic(8);
		File f = new File(this.tempDirectoryBase);
		if (!f.exists()) {
			f.mkdirs();
		}
	}

	@After
	public void tearDown() throws Exception {
	}

	private void runMatch(String matchType, int numGroups, int matchRatio) throws Exception { 
		for (int i = 0; i < 1; i++) {
			Match g = new Match();
			g.initMatch(matchType, numGroups);
			g.outfilePath = this.tempDirectoryBase + String.format("/match_%s_%d_output.txt", matchType,
							numGroups);
			for (int j = 1; j <= numGroups; j++)
			{
				g.addMatchGroup(Integer.toString(j));
			}
			g.parallelMatchingMode = 1;
			g.matchRatio = matchRatio;
			g.fixedRatio = 0;
			g.caliper = 0.05;
			g.addPatients(this.dataDirectory + String.format("/match_test_%d.txt", numGroups));
			g.run();
		}
	}
	
	
	@Test
	public void matchTest2() throws Exception {
		runMatch("nn", 2, 1);
		runMatch("nn", 2, 2);
		runMatch("nn", 2, 3);
		runMatch("nn", 2, 4);
		runMatch("nn", 2, 5);
		runMatch("nn", 2, 6);
		runMatch("nn", 2, 7);
		runMatch("nn", 2, 8);
		runMatch("balanced_nn", 2, 1);
		runMatch("balanced_nn", 2, 2);
		runMatch("balanced_nn", 2, 3);
		runMatch("balanced_nn", 2, 4);
		runMatch("balanced_nn", 2, 5);
		runMatch("balanced_nn", 2, 6);
		runMatch("balanced_nn", 2, 7);
		runMatch("balanced_nn", 2, 8);
		runMatch("greedy", 2, 1);
		runMatch("greedy", 2, 2);
		runMatch("greedy", 2, 3);
		runMatch("greedy", 2, 4);
		runMatch("greedy", 2, 5);
		runMatch("greedy", 2, 6);
		runMatch("greedy", 2, 7);
		runMatch("greedy", 2, 8);
	}

	@Test
	public void matchTest3() throws Exception {
		runMatch("nn", 3, 1);
		runMatch("nn", 4, 1);
		runMatch("nn", 5, 1);
	}
}
