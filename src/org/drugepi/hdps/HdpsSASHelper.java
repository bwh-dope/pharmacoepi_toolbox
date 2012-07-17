/*
	The contents of this file are subject to the Mozilla Public License Version 
	1.1 (the "License"); you may not use this file except in compliance with
	the License.  You may obtain a copy of the License at http://www.mozilla.org/MPL.
	
	The Original Code is the DoPE Pharmacoepidemiology Toolbox.
	
	The Initial Developer of the Original Code is the Brigham and Women's Hospital 
	Division of Pharmacoepidemiology.
	
	Contributor(s):
	   Jeremy A. Rassen <jrassen@post.harvard.edu>
 */

package org.drugepi.hdps;

import java.util.ArrayList;

import org.drugepi.PharmacoepiTool;


/**
 * Helper classes for SAS hd-PS implementation.
 *  
 * @author Jeremy A. Rassen
 * @version 2.1.0
 * 
 */
public class HdpsSASHelper extends PharmacoepiTool 
{
	/**
	 * Version string.
	 */
	public static final String version = "hd-PS SAS Helper Version 2.1.0";

	public String inputVarsAll;
	public String inputVarPatId;
	public String inputVarExposure;
	public String inputVarOutcome;
	public String inputVarTime;
	public String inputVarsDemographic;
	public String inputVarsPredefined;
	public String inputVarsForceCategorical;
	public String inputVarsIgnore;

	public String outputVarsDemographicContinuous;
	public String outputVarsDemographicCategorical;
	public String outputVarsPredefContinuous;
	public String outputVarsPredefCategorical;
	
	public ArrayList<String> listVarsAll;
	public ArrayList<String> listVarsCategorical;
	public ArrayList<String> listVarsContinuous;

	public ArrayList<String> listVarsDemographic;
	public ArrayList<String> listVarsPredefined;
	public ArrayList<String> listVarsForceCategorical;
	public ArrayList<String> listVarsIgnore;

	public ArrayList<String> listVarsDemogCategorical;
	public ArrayList<String> listVarsDemogContinuous;
	public ArrayList<String> listVarsPredefContinuous;
	public ArrayList<String> listVarsPredefCategorical;

	/**
	 * Constructor for the hd-PS class using default values for all parameters.
	 */
	public HdpsSASHelper()
	{
		super();
	}
	
	private static ArrayList<String> stringToList(String s)
	{
		ArrayList<String> l = new ArrayList<String>();

		if ((s != null) && (s.length() > 0)) {
			String[] stringTokens = s.split("\\s");
			for (int i = 0; i < stringTokens.length; i++)
				l.add(stringTokens[i].toUpperCase());
		}
		
		return l;
	}
	
	private static String join(ArrayList<String> list)
	{
		if ((list == null) || (list.size() == 0))
			return "";
		
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String item : list) {
			if (first)
				first = false;
			else
				sb.append(" ");
			sb.append(item.toUpperCase());
		}
		return sb.toString();
	}
	
	private static boolean isCategorical(String s)
	{
		if ((s == null) || (s.length() == 0))
			return false;
		
		String[] stringTokens = s.split("\\|");
		return (stringTokens[1].equalsIgnoreCase("C"));
	}
	
	private static String truncateType(String s)
	{
		if ((s == null) || (s.length() == 0))
			return "";
		
		String[] stringTokens = s.split("\\|");
		return (stringTokens[0].toUpperCase());
	}
	
	public void computeVariableLists()
	{
		// listVarsAll is a list of variables and types.  
		// format: var|type
		// type is one of: C[har], I[nt], D[ate]
		// at this time, only type C is meaningful
		listVarsAll = stringToList(inputVarsAll);
		listVarsDemographic = stringToList(inputVarsDemographic);
		listVarsPredefined = stringToList(inputVarsPredefined);
		listVarsForceCategorical = stringToList(inputVarsForceCategorical);
		listVarsIgnore = stringToList(inputVarsIgnore);

		// create master list of categorical and continuous vars
		listVarsCategorical = new ArrayList<String>();
		listVarsContinuous = new ArrayList<String>();
		for (String var: listVarsAll) {
			if (isCategorical(var))
				listVarsCategorical.add(truncateType(var));
			else
				listVarsContinuous.add(truncateType(var));
		}
		listVarsCategorical.addAll(listVarsForceCategorical);
		listVarsContinuous.removeAll(listVarsForceCategorical);
		
		// remake the "all" list without the type tags
		listVarsAll.clear();
		listVarsAll.addAll(listVarsCategorical);
		listVarsAll.addAll(listVarsContinuous);
		
		listVarsDemogContinuous = new ArrayList<String>();
		listVarsDemogCategorical = new ArrayList<String>();
		listVarsPredefContinuous = new ArrayList<String>();
		listVarsPredefCategorical = new ArrayList<String>();

		if ((inputVarPatId != null) && (inputVarPatId.length() > 0))
			listVarsIgnore.add(inputVarPatId.toUpperCase());
		if ((inputVarExposure != null)  && (inputVarExposure.length() > 0))
			listVarsIgnore.add(inputVarExposure.toUpperCase());
		if ((inputVarOutcome != null)  && (inputVarOutcome.length() > 0))
			listVarsIgnore.add(inputVarOutcome.toUpperCase());
		if ((inputVarTime != null) && (inputVarTime.length() > 0))
			listVarsIgnore.add(inputVarTime.toUpperCase());
		
		// set up demographic variables
		listVarsDemographic.removeAll(listVarsIgnore);
		
		listVarsDemogContinuous.addAll(listVarsDemographic);
		listVarsDemogContinuous.removeAll(listVarsCategorical);
		
		listVarsDemogCategorical.addAll(listVarsDemographic);
		listVarsDemogCategorical.removeAll(listVarsDemogContinuous);
				
		
		// set up other predefined variables
		if (listVarsPredefined.size() == 0) 
			listVarsPredefined.addAll(listVarsAll);

		listVarsPredefined.removeAll(listVarsIgnore);
		listVarsPredefined.removeAll(listVarsDemographic);

		listVarsPredefContinuous.addAll(listVarsPredefined);
		listVarsPredefContinuous.removeAll(listVarsCategorical);
		
		listVarsPredefCategorical.addAll(listVarsPredefined);
		listVarsPredefCategorical.removeAll(listVarsPredefContinuous);
		
		// output
		outputVarsDemographicCategorical = join(listVarsDemogCategorical);
		outputVarsDemographicContinuous = join(listVarsDemogContinuous);
		outputVarsPredefCategorical = join(listVarsPredefCategorical);
		outputVarsPredefContinuous = join(listVarsPredefContinuous);
	}
	
	public static void main(String args[]) {
		HdpsSASHelper sh = new HdpsSASHelper();

		String all;
		String demo;
		
//		all = "AGE85|I AGE75_84|I COVAR_ACS_2005|I COVAR_ACS_2M|I COVAR_AF_2005|I COVAR_AF_2M|I COVAR_CAD_2005|I COVAR_CAD_2M|I COVAR_CHF_2005|I COVAR_CHF_2M|I COVAR_COPD_2005|I COVAR_COPD_2M|I COVAR_CANCER_2005|I COVAR_CANCER_2M|I COVAR_CHOLESTERO_2005|I COVAR_CHOLESTERO_2M|I COVAR_DEMENTIA_2005|I COVAR_DEMENTIA_2M|I COVAR_DEPRESSION_2005|I COVAR_DEPRESSION_2M|I COVAR_DIAB_2005|I COVAR_DIAB_2M|I COVAR_ENDSTAGERENAL_2005|I COVAR_ENDSTAGERENAL_2M|I COVAR_HOSPICE_2005|I COVAR_HOSPICE_2M|I COVAR_HYPERTENSION_2005|I COVAR_HYPERTENSION_2M|I COVAR_MIMED_2M|I COVAR_MI_2005|I COVAR_MI_2M|I COVAR_NH_2005|I COVAR_NH_2M|I COVAR_RENAL_2005|I COVAR_RENAL_2M|I COVAR_REVAS_2005|I COVAR_REVAS_2M|I COVAR_STROKEMED_2M|I COVAR_STROKE_2005|I COVAR_STROKE_2M|I COVAR_VTEMED_2M|I COVAR_VTE_2005|I COVAR_VTE_2M|I COVERAGEGAP_EXP|I DEATH|I GAP60PRIOR_DRUGSPEND|I GAP60PRIOR_N_DRUG|I GAP_CHARLSONS_2005|I GAP_CHARLSONS_2M|I GAP_HOSP_2005|I GAP_HOSP_2M|I GAP_JCODENUM_2005|I GAP_JCODENUM_2M|I GAP_MDVISIT_2005|I GAP_MDVISIT_2M|I ID|C RACE_BLACK|I RACE_OTHER|I REGION|C SPENDING_PARTAB|I TIME_REACH_GAP|I GENDER|C INCOME|I RURAL|I";
//		demo = "Age75_84 Age85 Gender Race_Black Race_Other Rural Income Region";
//		sh.inputVarPatId = "ID";
//		sh.inputVarExposure = "CoverageGap_Exp";
//		sh.inputVarOutcome = "Death";
//		sh.inputVarsAll = all;
//		sh.inputVarsDemographic = demo;
//		sh.inputVarsIgnore = "";
//		sh.inputVarsPredefined = "";
//		sh.inputVarsForceCategorical = "gender region";
	
		all = "AGE_CAT|I APM|I DEATH_30|I DEATH_60|I death_90|I DEATH_180|I ID|I IDCHAR|C IndexDt|I Race|C SEX|I";
		demo = "RACE SEX AGE_CAT";
			
		sh.inputVarPatId = "idchar";
		sh.inputVarExposure = "APM";
		sh.inputVarOutcome = "DEATH_180";
		sh.inputVarsAll = all;
		sh.inputVarsDemographic = demo;
		sh.inputVarsIgnore = "indexdt DEATH_30 DEATH_90 id";
		sh.inputVarsPredefined = "";
		sh.inputVarsForceCategorical = "RACE SEX AGE_CAT";
		
		sh.computeVariableLists();
		
		System.out.printf("outputVarsDemographicCategorical:\n%s\n", sh.outputVarsDemographicCategorical);
		System.out.printf("outputVarsDemographicContinuous:\n%s\n", sh.outputVarsDemographicContinuous);
		System.out.printf("outputVarsPredefCategorical:\n%s\n", sh.outputVarsPredefCategorical);
		System.out.printf("outputVarsPredefContinuous:\n%s\n", sh.outputVarsPredefContinuous);
	}
}

