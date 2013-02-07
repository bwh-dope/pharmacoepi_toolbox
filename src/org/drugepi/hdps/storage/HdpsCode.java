/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.hdps.storage;

import java.sql.ResultSet;

import org.drugepi.hdps.HdpsDimensionController;
import org.drugepi.hdps.HdpsException;
import org.drugepi.util.Utils;

@SuppressWarnings({"all"})

public class HdpsCode {
	public String id;
    public String codeType;
	public String codeString;
	public HdpsDimensionController dimension;
    public boolean considerForPs;
    public boolean usedInPs;
    public double prevalence = -1;
    public int numUniqueOccurrences;
    
	private static final int kHistogramMaxBins = 10;
	private int[] histogram = new int[kHistogramMaxBins + 1];
    
    public double median = 0;
    public double q3 = 0;
    
    // Two code types: standard for those associated with variables, and
    // "intensity" associated with the special codes made for measuring
    // intensity.  If new special codes are added, they should each receive
    // a new code type and possibly a new variable type.
    public static final String CODE_TYPE_STANDARD = "Standard";
    public static final String CODE_TYPE_INTENSITY = "Intensity";
    
    // Index of where to store associated variable in the variable array
    public static final int kFrequentVarIndex = 0;
    public static final int kSporadicVarIndex = 1;
    public static final int kOnceVarIndex = 2;

    public static final int kIntensityVarIndex = 0;
    
    public HdpsVariable[] vars;
    
    // NOTE: if making a change here, also change in %hdps_InputVarsFile
    public static final String[] outputFieldNames = {
    	"dimension_num", 
    	"dimension", 
    	"code_id", 
    	"num_patients",
    	"consider_for_selection", 
    	"prevalence", 
    	"median",
    	"q3"
	};
    
    public HdpsCode(String id) {
    	this(id, CODE_TYPE_STANDARD);
    } 
    
    public HdpsCode(String id, String codeType) {
    	this.codeType = codeType;
    	this.considerForPs = false;
        this.usedInPs = false;
        this.id = id;
        this.createVariables();
        
		for (int i = 0; i <= kHistogramMaxBins; i++) {
			histogram[i] = 0;
		}
    } 
    
    public HdpsCode(String id, ResultSet r)
    throws Exception
	{
    	this(id);
    	
    	this.codeString = r.getString("code");
    	this.numUniqueOccurrences = r.getInt("frequency");
    	this.considerForPs = (r.getInt("consider_for_ps") == 1);
    	this.prevalence = r.getDouble("prevalence");
    	this.median = r.getDouble("median_occurrences");
    	this.q3 = r.getDouble("q3_occurrences");
	}
    
    public void putInRecurrenceBin(int bin)
    {
		if (bin < HdpsCode.kHistogramMaxBins) {
			histogram[bin]++;
		} else {
			histogram[HdpsCode.kHistogramMaxBins]++;
		}
    }
    
    public void switchRecurrenceBin(int oldBin, int newBin)
    {
		if (oldBin < HdpsCode.kHistogramMaxBins) {
			histogram[oldBin]--;
		} else {
			histogram[HdpsCode.kHistogramMaxBins]--;
		}
		this.putInRecurrenceBin(newBin);
    }
    
	private int findPoint(int[] cumulative, int point, int start) {
		if (point <= cumulative[start])
			return start;

		for (int i = start + 1; i < cumulative.length; i++) {
			if ((point > cumulative[i - 1]) && (point <= cumulative[i]))
				return i;
		}

		return cumulative.length;
	}
	
	public double calcPercentile(double percentile, int[] cumulative, int total) 
	{
		double center = percentile * total;
		boolean isEven = (Math.floor(center) == center);
		double value = 0;
		
		if (isEven) {
			// if n = 4, average persons 2 and 3
			int centerInt = (int) Math.floor(center);
			int v1 = findPoint(cumulative, centerInt, 0);
			int v2 = findPoint(cumulative, centerInt + 1, v1);
			value = ((double) (v1 + v2)) / 2d;
		} else {
			// if n = 5, get person 3
			int centerInt = (int) Math.ceil(center);
			int v = findPoint(cumulative, centerInt, 0);
			value = (double) v;
		}
		
		return value;
	}

    public void calcMedian()
    {
    	if (! this.isStandardCode()) 
    		return;
    	
		int[] cumulative = new int[HdpsCode.kHistogramMaxBins + 1];

		for (int i = 0; i <= HdpsCode.kHistogramMaxBins; i++) 
			cumulative[i] = 0;
    	
    	cumulative[0] = this.histogram[0];
		for (int j = 1; j <= HdpsCode.kHistogramMaxBins; j++) {
			cumulative[j] = cumulative[j - 1] + histogram[j];
		}

		int totalOccurrences = cumulative[HdpsCode.kHistogramMaxBins];

		// Median
		this.median = this.calcPercentile(0.50, cumulative, totalOccurrences);

		// Q3
		this.q3 = this.calcPercentile(0.75, cumulative, totalOccurrences);
    }
    
    private void createVariables() {
    	if (this.isStandardCode()) {
//	        this.vars = new HdpsVariable[4];
	        this.vars = new HdpsVariable[3];
	
//	    	this.vars[kAnyVarIndex] = new HdpsVariable(this, HdpsVariable.kAnyVarType);
	    	this.vars[kOnceVarIndex] = new HdpsVariable(this, HdpsVariable.VAR_TYPE_ONCE);
	    	this.vars[kSporadicVarIndex] = new HdpsVariable(this, HdpsVariable.VAR_TYPE_SPORADIC);
	    	this.vars[kFrequentVarIndex] = new HdpsVariable(this, HdpsVariable.VAR_TYPE_FREQUENT);
    	} else if (this.codeType.equalsIgnoreCase(CODE_TYPE_INTENSITY)) {
	        this.vars = new HdpsVariable[1];
	    	this.vars[kIntensityVarIndex] = new HdpsVariable(this, HdpsVariable.VAR_TYPE_SERVICE_INTENSITY);
    	} else {
    		// !!! should really throw an exception
    		System.out.println("*** *** *** Unknown code type -- this is an error");
    	}
    }
    
    public HdpsVariable getVariableByType(String type)
    {
    	if (type.equals(HdpsVariable.VAR_TYPE_ONCE))
    		return this.vars[kOnceVarIndex];
    	
    	if (type.equals(HdpsVariable.VAR_TYPE_SPORADIC))
    		return this.vars[kSporadicVarIndex];

    	if (type.equals(HdpsVariable.VAR_TYPE_FREQUENT))
    		return this.vars[kFrequentVarIndex];

    	if (type.equals(HdpsVariable.VAR_TYPE_SERVICE_INTENSITY))
    		return this.vars[kIntensityVarIndex];

    	return null;
    }
    
    public boolean isStandardCode() {
    	return this.codeType.equalsIgnoreCase(CODE_TYPE_STANDARD);
    }
    
	public String[] toStringArray() {
		final String quoteStr = "\"";
    	
		String[] s = {
   			Integer.toString(this.dimension.dimensionId),
			quoteStr + this.dimension.dimensionDescription + quoteStr, 
			quoteStr + this.codeString + quoteStr,
			Integer.toString(this.numUniqueOccurrences),
			Boolean.toString(this.considerForPs),
			Utils.formatOutputDouble(this.prevalence), 
			Utils.formatOutputDouble(this.median), 
			Utils.formatOutputDouble(this.q3)
		};
		
		return s;
	}
}
