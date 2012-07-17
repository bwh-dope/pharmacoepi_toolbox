/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.match.storage;

import java.util.*;

public class MultiMatchPatient extends MatchPatient {
	public List<List<MultiMatchPatient>> potentialMatches;
	
	// hack!
	private List<MatchGroup> matchGroups;

	public MultiMatchPatient(int numGroups) {
		super(numGroups);
		
		this.potentialMatches = new ArrayList<List<MultiMatchPatient>>();
		this.matchGroups = null;
	}
	
	public void addPotentialMatch(MatchGroup mg, MultiMatchPatient p) 
	{
		List<MultiMatchPatient> potentialMatchList = this.potentialMatches.get(mg.groupNumber);
		potentialMatchList.add(p);
	}
	
	public List<MultiMatchPatient> getPotentialMatches(MatchGroup mg)
	{
		return (this.potentialMatches.get(mg.groupNumber));
	}
	
	public void removePotentialMatches(Collection<MatchPatient> c)
	{
		for (List<MultiMatchPatient> l: this.potentialMatches) {
			l.removeAll(c);
		}
	}
	
	public void clearPotentialMatches()
	{
		for (List<MultiMatchPatient> l: this.potentialMatches) {
			l.clear();
		}
	}
	
	public void setMatchGroups(List<MatchGroup> matchGroups) {
		this.matchGroups = matchGroups;
		
		for (int i = 0; i < this.matchGroups.size(); i++)
			potentialMatches.add(new ArrayList<MultiMatchPatient>());
		
		List<MultiMatchPatient> myList = potentialMatches.get(this.matchGroup.groupNumber);
		myList.add(this);
	}
	
	public boolean noMatchPossible()
	{
		for (List<MultiMatchPatient> l: this.potentialMatches)
			if ((l == null) || (l.size() == 0))
				return true;
		
		return false;
	}
	
	public MatchSet getSingletonMatch() 
	{
		// !!! hack fix
		MatchSet ms = new MatchSet(10);

		for (List<MultiMatchPatient> l: this.potentialMatches)
			if (l.size() == 1) {
				MultiMatchPatient p = l.get(0);
				ms.put(p);
			} else
				return null;
		
		ms.put(this);

		return ms;
	}
	
	public int getNumPermutations()
	{
		int numPermutations = 1;
		int numPermutationLevels = this.matchGroups.size();
		
		permutationCurrentIndexes = new int[numPermutationLevels];
		permutationMaxIndexes = new int[numPermutationLevels];
		permutationMaxLevelIndex = numPermutationLevels - 1;
		for (int i = 0; i < numPermutationLevels; i++) {
			MatchGroup mg = this.matchGroups.get(i);
			if (mg != this.matchGroup) {
				List<MultiMatchPatient> potentialMatchesForPt = this.getPotentialMatches(mg);
				if ((potentialMatchesForPt == null) || (potentialMatchesForPt.size() == 0)) 
					return 0;
				numPermutations = numPermutations * potentialMatchesForPt.size();
			}
		}
		
		return numPermutations;
	}

	private int permutationCurrentIndexes[]; 
	private int permutationMaxIndexes[]; 
	private int permutationMaxLevelIndex;
	public boolean resetPermutions()
	{
		int numPermutationLevels = this.matchGroups.size();
		
		permutationCurrentIndexes = new int[numPermutationLevels];
		permutationMaxIndexes = new int[numPermutationLevels];
		permutationMaxLevelIndex = numPermutationLevels - 1;
		for (int i = 0; i < numPermutationLevels; i++) {
			permutationCurrentIndexes[i] = 0;

			MatchGroup mg = this.matchGroups.get(i);
			if (mg == this.matchGroup)
				permutationMaxIndexes[i] = 0;
			else {
				List<MultiMatchPatient> potentialMatchesForPt = this.getPotentialMatches(mg);
				if ((potentialMatchesForPt == null) || (potentialMatchesForPt.size() == 0)) 
					return false;

				permutationMaxIndexes[i] = potentialMatchesForPt.size() - 1;
			}
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
			// !!! hack -- fix!
			MatchSet ms = new MatchSet(10);
			for (int i = 0; i <= permutationMaxLevelIndex; i++) {
				MatchGroup mg = this.matchGroups.get(i);
				if (mg == this.matchGroup)
					ms.put(this);
				else {
					List<MultiMatchPatient> potentialMatchesForPt = this.getPotentialMatches(mg);
					if (potentialMatchesForPt == null) {
					} else {
						MatchPatient p = potentialMatchesForPt.get(permutationCurrentIndexes[i]);
						ms.put(p);
					}
				}
			}

			permutationCurrentIndexes[levelToIncrement]++;
			
			return ms;
		}
		
		return null;
	}
}
