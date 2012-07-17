package org.drugepi.match;

import java.util.*;

import org.drugepi.match.storage.*;

public class MatchPermuter {
	List<List<MultiMatchPatient>> patients;
	private int numGroups;
	
	public MatchPermuter(int numGroups) {
		this.numGroups = numGroups;
		
		this.patients = new ArrayList<List<MultiMatchPatient>>();
	}
	
	public void addPatientGroup(List<MultiMatchPatient> group)
	{
		this.patients.add(group);
	}
	
//	public int getNumPermutations()
//	{
//		int numPermutations = 1;
//		int numPermutationLevels = this.matchGroups.size();
//		
//		permutationCurrentIndexes = new int[numPermutationLevels];
//		permutationMaxIndexes = new int[numPermutationLevels];
//		permutationMaxLevelIndex = numPermutationLevels - 1;
//		for (int i = 0; i < numPermutationLevels; i++) {
//			MatchGroup mg = this.matchGroups.get(i);
//			if (mg != this.matchGroup) {
//				List<MultiMatchPatient> potentialMatchesForPt = this.getPotentialMatches(mg);
//				if ((potentialMatchesForPt == null) || (potentialMatchesForPt.size() == 0)) 
//					return 0;
//				numPermutations = numPermutations * potentialMatchesForPt.size();
//			}
//		}
//		
//		return numPermutations;
//	}

	private int permutationCurrentIndexes[]; 
	private int permutationMaxIndexes[]; 
	private int permutationMaxLevelIndex;
	public boolean startPermutations()
	{
		int numPermutationLevels = numGroups;
		
		permutationCurrentIndexes = new int[numPermutationLevels];
		permutationMaxIndexes = new int[numPermutationLevels];
		permutationMaxLevelIndex = numPermutationLevels - 1;
		for (int i = 0; i < numPermutationLevels; i++) {
			permutationCurrentIndexes[i] = 0;
			permutationMaxIndexes[i] = this.patients.get(i).size() - 1;
			if (permutationMaxIndexes[i] == -1)
				return false;
		}
		
		return true;
	}
	
	public MatchSet getNextPermutation()
	{
		// find the next patient that hasn't get been looked at
		int levelToIncrement = -1;
		for (int i = permutationMaxLevelIndex; i >= 0; i--) {
			if (permutationCurrentIndexes[i] < permutationMaxIndexes[i]) {
				levelToIncrement = i;
				break;
			} else {
				permutationCurrentIndexes[i] = 0;
			}
		}
		
		// if anyone was found, return a match set with the permutation
		if (levelToIncrement > 0) {
			MatchSet ms = new MatchSet(this.numGroups);
			for (int i = 0; i <= permutationMaxLevelIndex; i++) {
				MatchPatient p = this.patients.get(i).get(permutationCurrentIndexes[i]);
				ms.put(p);
			}

			permutationCurrentIndexes[levelToIncrement]++;
			
			return ms;
		}
		
		return null;
	}
}
