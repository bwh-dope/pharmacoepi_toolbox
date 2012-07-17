/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.hdps.storage;

import java.sql.ResultSet;

import org.drugepi.util.Utils;

public class HdpsVariable  {
	public static double INVALID = -9999d;
	
	public HdpsCode code;
	public String varName;
	public String type;
	public double N = 0;
	public double pt = 0;

	public double nMissing;
	
	public double e1;
	public double e0;
	
	public double c1;
	public double c0;

	public double d1;
	public double d0;
	
	public double pt_e1;
	public double pt_e0;

	public double pt_c1;
	public double pt_c0;
	
	public double e1c1 = 0;
	public double e1c0 = 0;
	public double e0c1 = 0;
	public double e0c0 = 0;

	public double e1Missing = 0;
	public double e0Missing = 0;
	public double d1Missing = 0;
	public double d0Missing = 0;
	
	public double d1c1 = 0;
	public double d1c0 = 0;
	public double d0c1 = 0;
	public double d0c0 = 0;

	/* mean outcome overall, in c1, and in c0 */
	public double numEvents = 0;
	public double c1NumEvents = 0;
	public double c0NumEvents = 0;
	
	/* mean outcome overall, in c1, and in c0 */
	public double meanOutcome = 0;
	public double c1MeanOutcome = 0;
	public double c0MeanOutcome = 0;

	public double pc_e0 = 0;
	public double pc_e1 = 0;
	public double rrCe = 0;
	public double rrCd = 0;
	public double cdRegressionBeta = 0;
	
	public double bias = 0;

	public double biasRankingVariable = 0;
	public double expAssocRankingVariable = 0;
	public double outcomeAssocRankingVariable = 0;
	
	public double activeRankingVariable = 0;
	
	public int zBiasScore = 0;

	public boolean selectedForPs;
	public static final int valueOne = 1;
	public static final int valueMissing = -1;
	public static final int valueZero = 0;
	
//    public static final String kAnyVarType = "Any";
    public static final String kFrequentVarType = "Frequent";
    public static final String kSporadicVarType = "Spor";
    public static final String kOnceVarType = "Once";
    public static final String kSpecialVarType = "Special";

    public HdpsVariable()
	{
    	super();
	}
    
    // NOTE: if making a change here, also change in %hdps_InputVarsFile
    public static final String[] outputFieldNames = {
			"dimension", 				// 0
			"code_id", 					// 1
			"var_name", 				// 2
			"selected_for_ps",  		// 3
			"e1", 						// 4 
			"e0", 						// 5 
			"pt_e1", 					// 6 
			"pt_e0", 					// 7 
			"d1", 						// 8 
			"d0", 						// 9 
			"c1", 						// 10 
			"c0", 						// 11 
			"pt_c1", 					// 12 
			"pt_c0", 					// 13 
			"e1c1", 					// 14
			"e1c0",  					// 15
			"e0c1", 					// 16 
			"e0c0", 					// 17 
			"d1c1", 					// 18 
			"d1c0", 					// 19 
			"d0c1", 					// 20 
			"d0c0", 					// 21
			"pc_e1", 					// 22 
			"pc_e0", 					// 23 
			"num_events", 				// 24 
			"c1_num_events", 			// 25 
			"c0_num_events", 			// 26 
			"mean_outcome", 			// 27 
			"c1_mean_outcome", 			// 28
			"c0_mean_outcome",  		// 29
			"rr_ce", 					// 30
			"rr_cd", 					// 31 
			"bias", 					// 32
			"exp_assoc_ranking_var", 	// 33 
			"outcome_assoc_ranking_var",// 34
			"bias_assoc_ranking_var", 	// 35 
			"z_bias_score" 				// 36 
		};
    
    public HdpsVariable(HdpsCode code, String type)
	{
    	this();
    	
		this.code = code;
		this.biasRankingVariable = Math.abs(Math.log(bias));
		this.selectedForPs = false;
		this.varName = code.id + type;
		this.type = type;
	}

    public HdpsVariable(HdpsCode code, ResultSet r)
    throws Exception
	{
    	this();
    	
    	this.varName = r.getString("var_name");
    	this.type = r.getString("type");
    	this.N = r.getDouble("n");
    	this.pt = r.getDouble("pt");

    	this.e1 = r.getDouble("e1");
    	this.e0 = r.getDouble("e0");
    	
       	this.c1 = r.getDouble("c1");
    	this.c0 = r.getDouble("c0");

    	this.d1 = r.getDouble("d1");
    	this.d0 = r.getDouble("d0");
    	
    	this.pt_e1 = r.getDouble("pt_e1");
    	this.pt_e0 = r.getDouble("pt_e0");

    	this.pt_c1 = r.getDouble("pt_c1");
    	this.pt_c0 = r.getDouble("pt_c0");

    	this.e1c1 = r.getDouble("e1c1");
    	this.e1c0 = r.getDouble("e1c0");
    	this.e0c1 = r.getDouble("e0c1");
    	this.e0c0 = r.getDouble("e0c0");

    	this.d1c1 = r.getDouble("d1c1");
    	this.d1c0 = r.getDouble("d1c0");
    	this.d0c1 = r.getDouble("d0c1");
    	this.d0c0 = r.getDouble("d0c0");

    	this.numEvents = r.getDouble("num_events");
    	this.c1NumEvents = r.getDouble("c1_num_events");
    	this.c0NumEvents = r.getDouble("c0_num_events");

    	this.pc_e0 = r.getDouble("pc_e0");
    	this.pc_e1 = r.getDouble("pc_e1");
    	this.rrCe = r.getDouble("rr_ce");
    	this.rrCd = r.getDouble("rr_cd");
    	this.cdRegressionBeta = r.getDouble("cd_regression_beta");
    	
    	this.expAssocRankingVariable = r.getDouble("exp_assoc_ranking_var");
    	this.outcomeAssocRankingVariable = r.getDouble("outcome_assoc_ranking_var");
    	
    	this.bias = r.getDouble("bias");
    	this.biasRankingVariable = r.getDouble("bias_ranking_var");
	}

    public HdpsVariable(HdpsCode code, String[] s)
    throws Exception
	{
    	this();
    	
    	// hack, but a quick way to get a code where none exists
    	if (code == null)
    		this.code = new HdpsCode(s[1]);
    	this.code.codeString = s[1];
    	
    	this.varName = s[2];
    	this.selectedForPs = Boolean.parseBoolean(s[3]);
    	this.e1 = Utils.parseInputDouble(s[4]);
    	this.e0 = Utils.parseInputDouble(s[5]);
    	this.pt_e1 = Utils.parseInputDouble(s[6]);
    	this.pt_e0 = Utils.parseInputDouble(s[7]);
    	this.d1 = Utils.parseInputDouble(s[8]);
    	this.d0 = Utils.parseInputDouble(s[9]);
    	this.c1 = Utils.parseInputDouble(s[10]);
    	this.c0 = Utils.parseInputDouble(s[11]);
    	this.pt_c1 = Utils.parseInputDouble(s[12]);
    	this.pt_c0 = Utils.parseInputDouble(s[13]);
    	this.e1c1 = Utils.parseInputDouble(s[14]);
    	this.e1c0 = Utils.parseInputDouble(s[15]);
    	this.e0c1 = Utils.parseInputDouble(s[16]);
    	this.e0c0 = Utils.parseInputDouble(s[17]);
    	this.d1c1 = Utils.parseInputDouble(s[18]);
    	this.d1c0 = Utils.parseInputDouble(s[19]);
    	this.d0c1 = Utils.parseInputDouble(s[20]);
    	this.d0c0 = Utils.parseInputDouble(s[21]);
    	this.pc_e1 = Utils.parseInputDouble(s[22]);
    	this.pc_e0 = Utils.parseInputDouble(s[23]);
    	this.numEvents = Utils.parseInputDouble(s[24]);
    	this.c1NumEvents = Utils.parseInputDouble(s[25]);
    	this.c0NumEvents = Utils.parseInputDouble(s[26]);
    	this.meanOutcome = Utils.parseInputDouble(s[27]);
    	this.c1MeanOutcome = Utils.parseInputDouble(s[28]);
    	this.c0MeanOutcome = Utils.parseInputDouble(s[29]);
    	this.rrCe = Utils.parseInputDouble(s[30]);
    	this.rrCd = Utils.parseInputDouble(s[31]);
    	this.bias = Utils.parseInputDouble(s[32]);
    	this.expAssocRankingVariable = Utils.parseInputDouble(s[33]);
    	this.outcomeAssocRankingVariable = Utils.parseInputDouble(s[34]);
    	this.biasRankingVariable = Utils.parseInputDouble(s[35]);
    	this.zBiasScore = Integer.parseInt(s[36]);
	}
    
	public boolean isTypeFrequent()
	{
		return (this.type == kFrequentVarType);
	}

	public boolean isTypeSporadic()
	{
		return (this.type == kSporadicVarType);
	}

	public boolean isTypeOnce()
	{
		return (this.type == kOnceVarType);
	}
	
	public boolean isTypeSpecial()
	{
		return (this.type == kSpecialVarType);
	}
	
	public String[] toStringArray() {
		final String quoteStr = "\"";
		
		String[] s = {
			quoteStr + this.code.dimension.dimensionDescription + quoteStr,
			quoteStr + this.code.codeString + quoteStr,
			quoteStr + this.varName + quoteStr,
			Boolean.toString(this.selectedForPs),
			Utils.formatOutputDouble(this.e1),
			Utils.formatOutputDouble(this.e0),
			Utils.formatOutputDouble(this.pt_e1),
			Utils.formatOutputDouble(this.pt_e0),
			Utils.formatOutputDouble(this.d1),
			Utils.formatOutputDouble(this.d0),
			Utils.formatOutputDouble(this.c1),
			Utils.formatOutputDouble(this.c0),
			Utils.formatOutputDouble(this.pt_c1),
			Utils.formatOutputDouble(this.pt_c0),
			Utils.formatOutputDouble(this.e1c1),
			Utils.formatOutputDouble(this.e1c0),
			Utils.formatOutputDouble(this.e0c1),
			Utils.formatOutputDouble(this.e0c0),
			Utils.formatOutputDouble(this.d1c1),
			Utils.formatOutputDouble(this.d1c0),
			Utils.formatOutputDouble(this.d0c1),
			Utils.formatOutputDouble(this.d0c0),
			Utils.formatOutputDouble(this.pc_e1),
			Utils.formatOutputDouble(this.pc_e0),
			Utils.formatOutputDouble(this.numEvents),
			Utils.formatOutputDouble(this.c1NumEvents),
			Utils.formatOutputDouble(this.c0NumEvents),
			Utils.formatOutputDouble(this.meanOutcome),
			Utils.formatOutputDouble(this.c1MeanOutcome),
			Utils.formatOutputDouble(this.c0MeanOutcome),
			Utils.formatOutputDouble(this.rrCe),
			Utils.formatOutputDouble(this.rrCd),
			Utils.formatOutputDouble(this.bias),
			Utils.formatOutputDouble(this.expAssocRankingVariable),
			Utils.formatOutputDouble(this.outcomeAssocRankingVariable),
			Utils.formatOutputDouble(this.biasRankingVariable),
			Integer.toString(this.zBiasScore)
		};
		
		return s;
	}
}
