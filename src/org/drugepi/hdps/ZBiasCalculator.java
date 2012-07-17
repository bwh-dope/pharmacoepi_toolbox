package org.drugepi.hdps;

import java.util.*;

import org.apache.commons.math.stat.descriptive.rank.Percentile;
import org.drugepi.hdps.storage.HdpsVariable;
import org.drugepi.hdps.storage.comparators.*;

public class ZBiasCalculator {
	public static void scoreVariables(List<HdpsVariable> variableList)
	{
		// copy variables list
		List<HdpsVariable> expSortVariableList = 
				new ArrayList<HdpsVariable>();
		List<HdpsVariable> outcomeSortVariableList = 
			new ArrayList<HdpsVariable>();
		
		for (HdpsVariable var: variableList) {
			var.zBiasScore = 0;
			
			if ((var.expAssocRankingVariable != HdpsVariable.INVALID) &&
				(var.outcomeAssocRankingVariable != HdpsVariable.INVALID)) {
				expSortVariableList.add(var);
				outcomeSortVariableList.add(var);
			}
		}
		
		// sort variables by exposure association (strongest first) 
		Collections.sort(expSortVariableList, new HdpsVariableReverseExposureAssociationComparator());
		
		// sort variables by outcome association (weakest first) 
		Collections.sort(outcomeSortVariableList, new HdpsVariableReverseOutcomeAssociationComparator());
		Collections.reverse(outcomeSortVariableList);

		// create an array of outcome strengths
		double[] outcomeStrengths = new double[outcomeSortVariableList.size()];
		for (int i = 0; i < outcomeStrengths.length; i++) 
			outcomeStrengths[i] = outcomeSortVariableList.get(i).outcomeAssocRankingVariable;
			
		// array that will store breaks between deciles
		// find the median of outcome strength 
		Percentile pctile = new Percentile();
		
		// Find quintiles 1 through 5 of outcome weakness
		// AMONG the weakest half of the variables.
		// List is sorted strongest first, so the weakest variables 
		// will be at the end
		// quintile 1 = weakest 
		// don't use startsOfQuintile[0]
		
		double median = pctile.evaluate(outcomeStrengths, 50.0);
		int searchCeiling = Arrays.binarySearch(outcomeStrengths, median);
		if (searchCeiling < 0)
			searchCeiling = -(searchCeiling + 1);
		
		int startsOfQuintile[] = new int[7];

		for (int quintile = 1; quintile <= 5; quintile++) {
			// find the probability that *begins* this quintile
			double p = (quintile - 1) * 20;
			if (p > 0) {
				double quintileStartP = pctile.evaluate(outcomeStrengths, 
								0, searchCeiling, 
								(quintile - 1) * 20);
				
				startsOfQuintile[quintile] = Arrays.binarySearch(outcomeStrengths, quintileStartP); 
				if (startsOfQuintile[quintile] < 0)
					startsOfQuintile[quintile] = -(startsOfQuintile[quintile] + 1);
			} else 
				startsOfQuintile[quintile] = 0;
		}
		startsOfQuintile[6] = searchCeiling;
		
		// score the variables, BUT make quintile 5 the weakest
		for (int quintile = 1; quintile <= 5; quintile++) { 
			for (int i = startsOfQuintile[quintile]; i < startsOfQuintile[quintile + 1]; i++) { 
				HdpsVariable v = outcomeSortVariableList.get(i);
				v.zBiasScore = 6 - quintile;
			}
		}
		
//		for (HdpsVariable v: outcomeSortVariableList) {
//			System.out.printf("%s    %1.4f    %d\n", v.varName, v.outcomeAssocRankingVariable, v.zBiasScore);
//		}
	}
}
