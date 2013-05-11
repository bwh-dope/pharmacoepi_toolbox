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

package org.drugepi.hdps.local;

import java.util.*;

import org.drugepi.hdps.*;
import org.drugepi.hdps.storage.*;
import org.drugepi.hdps.storage.comparators.*;
import org.drugepi.util.*;

import com.sleepycat.persist.EntityCursor;

public class HdpsLocalDimensionController extends HdpsDimensionController {
	public Map<String, HdpsCode> codeMap;

	// INTERNAL VARIABLES
	private int varGenerationSequence = 0;
	
	private HdpsLocalController hdpsController;

	private class NumPatientCodesStore {
		public int numPatientCodes = 0;
		public int numUniquePatientCodes = 0;
	}
	
	private HashMap<String, NumPatientCodesStore> numPatientCodes;
	
	public HdpsLocalDimensionController(Hdps hdps, HdpsLocalController hdpsController) 
	{
		super(hdps);
		this.hdpsController = hdpsController;

		this.codeMap = new HashMap<String, HdpsCode>();
		this.numPatientCodes = new HashMap<String, NumPatientCodesStore>();
	}

	private void filterCodesForPrevalence() throws Exception
	{
		for (HdpsCode code : this.codeMap.values()) {
			code.prevalence = (float) code.numUniqueOccurrences
					/ (float) this.hdpsController.getNumPatients();
			if (code.prevalence > 0.5)
				code.prevalence = 1.0 - code.prevalence;
		}

		// sort by descending prevalence
		List<HdpsCode> sortedCodes = new ArrayList<HdpsCode>(this.codeMap.values());
		Collections.sort(sortedCodes, new HdpsCodeReversePrevalenceComparator());

		int numIncluded = 0;
		for (HdpsCode code : sortedCodes) {
			code.considerForPs = false;
			if (code.numUniqueOccurrences > this.hdps.frequencyMin) {
				if (numIncluded < this.hdps.topN) {
					code.considerForPs = true;
					numIncluded++;
				} 
			}
		}
	}
	
	public void buildCodePatientDatabase() throws Exception {
		String[] row;
		int rowsRead = 0;
		HdpsCode code = null;
		HdpsPatient patient = null;
		HdpsCodePatientLink codePatientLink = null;
		
		while ((row = this.reader.getNextRow()) != null) {
			String codeString = row[codeColumn];
			String patientId = row[patientColumn];

			if ((patient == null) || (! patient.id.equals(patientId)))
				patient = this.hdpsController.getPatientDatabase().get(patientId);

			code = codeMap.get(codeString);
			if (code == null) {
				code = new HdpsCode(this.generateVariableName());
				code.codeString = codeString;
				code.dimension = this;
				code.numUniqueOccurrences = 0;
				codeMap.put(code.codeString, code);
			}
		
			if (patient != null) {
				String cplId = HdpsCodePatientLink.generateId(code, patient);

				NumPatientCodesStore store = this.numPatientCodes.get(patient.id);
				if (store == null) {
					store = new NumPatientCodesStore();
					this.numPatientCodes.put(patient.id, store);
				}

				// do a lookup with a cursor in order to do a possible update below
				EntityCursor<HdpsCodePatientLink> cursor = 
					this.hdpsController.getCodePatientLinkDatabase().entities(cplId, true, cplId, true);
				codePatientLink = cursor.first();
				if (codePatientLink == null) {
					codePatientLink = new HdpsCodePatientLink();
					codePatientLink.id = cplId;
					codePatientLink.patientId = patient.id;
					codePatientLink.codeId = code.id;
					codePatientLink.numOccurrences = 1;
					code.putInRecurrenceBin(codePatientLink.numOccurrences);
					code.numUniqueOccurrences++;
					store.numUniquePatientCodes++;
					this.hdpsController.getCodePatientLinkDatabase().put(codePatientLink);
				} else {
					codePatientLink.numOccurrences++;
					code.switchRecurrenceBin(codePatientLink.numOccurrences - 1, codePatientLink.numOccurrences);
					cursor.update(codePatientLink);
				}
				
				store.numPatientCodes++;
				
				cursor.close();
			}
			rowsRead++;
		}
		
		// one last time, just in case
		this.hdpsController.getCodePatientLinkDatabase().put(codePatientLink);

		System.out.printf(
				"NOTE: hd-PS dimension %s read finished.  %d input rows processed.",
				this.dimensionDescription, rowsRead);
		System.out.println("");
	}

	public void readDimension() throws Exception {
		this.buildCodePatientDatabase();
		this.filterCodesForPrevalence();
		this.calculateMediansAndBias();
		
		System.out.printf(
				"NOTE: hd-PS dimension %s building finished.\n",
				this.dimensionDescription);
	}

	protected void createServiceIntensityVariables(boolean uniqueOnly) {
		Integer freq[] = new Integer[this.numPatientCodes.size()];

		int i = 0;
		for (NumPatientCodesStore s: this.numPatientCodes.values()) {
			if (uniqueOnly == true)
				freq[i] = s.numUniquePatientCodes;
			else 
				freq[i] = s.numPatientCodes;
			i++;
		}
		
		HdpsCode[] quartileCodes = new HdpsCode[4];
		int[] quartiles = new int[4];
		for (i = 0; i < 4 ; i++) {
			Integer q = (Integer) Quickselect.getQuartile(freq, i + 1);
			quartiles[i] = q;
			
			//RTRIM('D%d_INT_%s_Q%d') AS var_name,
			
			// Make a new code for this quartile
			String codeString = null;
			codeString = this.generateServiceIntensityVariableName(i + 1, uniqueOnly);
			quartileCodes[i] = new HdpsCode(codeString, HdpsCode.CODE_TYPE_INTENSITY);
			quartileCodes[i].codeString = codeString;
			quartileCodes[i].dimension = this;
			quartileCodes[i].considerForPs = true;
			codeMap.put(quartileCodes[i].codeString, quartileCodes[i]);			
		}
		
		for (Map.Entry<String,NumPatientCodesStore> e: this.numPatientCodes.entrySet()) {
			HdpsPatient patient = this.hdpsController.getPatientDatabase().get(e.getKey());
			int f = 0;
			if (uniqueOnly == true)
				f = e.getValue().numUniquePatientCodes;
			else
				f = e.getValue().numPatientCodes;
			
			for (int j = 0; j < 4; j++) {
				int quartileMin = (j > 0 ? quartiles[j - 1] : 0);
				int quartileMax = (j <= 3 ? quartiles[j] : Integer.MAX_VALUE);
				
				if ((f > quartileMin) && (f <= quartileMax)) {
					// Make a code patient link
					String cplId = HdpsCodePatientLink.generateId(quartileCodes[j], patient);
					
					HdpsCodePatientLink codePatientLink = new HdpsCodePatientLink();
					codePatientLink.id = cplId;
					codePatientLink.patientId = patient.id;
					codePatientLink.codeId = quartileCodes[j].id;
					codePatientLink.intensityVarValue = 1;
					this.hdpsController.getCodePatientLinkDatabase().put(codePatientLink);
					
					HdpsVariable var = quartileCodes[j].getVariableByType(HdpsVariable.VAR_TYPE_SERVICE_INTENSITY);
					this.updateVarCounts(var, codePatientLink.intensityVarValue, patient);
				}
			}
		}

//		System.out.printf("Quartiles: %d %d %d %d\n", quartiles[0], quartiles[1], quartiles[2], quartiles[3]);
	}
	
	protected void calculateMediansAndBias() throws Exception {
		if (this.hdps.inferServiceIntensityVars == 1) {
			createServiceIntensityVariables(true);
			createServiceIntensityVariables(false);
		}
		
		for (HdpsCode code: this.codeMap.values()) {
			if (code.considerForPs) {
				code.calcMedian();
				markOccurrenceType(code);
				calculateBias(code);
			}
		}
	}
	
	private void markOccurrenceType(HdpsCode code) {
		if (! code.isStandardCode())
			return;
	
		// use primary keys in order to call update() rather than put() below.
		// a bit of a hack.
		EntityCursor<HdpsCodePatientLink> cursor = 
			HdpsCodePatientLink.getCursorForCodeId(this.hdpsController, code.id);
		
		for (HdpsCodePatientLink codePatientLink: cursor) {
			HdpsPatient patient = this.hdpsController.getPatientDatabase().get(codePatientLink.patientId);

			boolean setOnceVar = (codePatientLink.numOccurrences >= 1);
			boolean setSporadicVar = (codePatientLink.numOccurrences >= code.median);
			boolean setFrequentVar = (codePatientLink.numOccurrences >= code.q3);

			boolean sporadicVarMissing = false;
			boolean frequentVarMissing = false;

			if (code.q3 == code.median)
				frequentVarMissing = true;
			if (code.median == 1)
				sporadicVarMissing = true;

			codePatientLink.anyVarValue = HdpsVariable.valueOne;
			if (setFrequentVar)
				codePatientLink.frequentVarValue = 
					(frequentVarMissing ? HdpsVariable.valueMissing : HdpsVariable.valueOne);
			if (setSporadicVar) 
				codePatientLink.sporadicVarValue = 
					(sporadicVarMissing ? HdpsVariable.valueMissing : HdpsVariable.valueOne);
			if (setOnceVar) 
				codePatientLink.onceVarValue = HdpsVariable.valueOne;
			
			this.updateVarCounts(code.vars[HdpsCode.kOnceVarIndex], codePatientLink.onceVarValue, patient);
			this.updateVarCounts(code.vars[HdpsCode.kSporadicVarIndex], codePatientLink.sporadicVarValue, patient);
			this.updateVarCounts(code.vars[HdpsCode.kFrequentVarIndex], codePatientLink.frequentVarValue, patient);
			
			cursor.update(codePatientLink);
		}
		cursor.close();
	}
	
	private void updateVarCounts(HdpsVariable var, int value, HdpsPatient patient) 
	{
		if (value == HdpsVariable.valueZero)
			return;
		
		// this is a running total until the end, when it's
		// divided by c1
		var.c1MeanOutcome += patient.outcomeContinuous;
		
		var.c1NumEvents += patient.outcomeCount;
		
		if (value == HdpsVariable.valueOne)
			var.pt_c1 += patient.followUpTime;
		
		if (patient.exposed) {
			if (value == HdpsVariable.valueMissing)
				var.e1Missing++;
			else if (value == HdpsVariable.valueOne) 
				var.e1c1++;
		} else {
			if (value == HdpsVariable.valueMissing)
				var.e0Missing++;
			else if (value == HdpsVariable.valueOne)
				var.e0c1++;
		}

		if (patient.outcomeDichotomous) {
			if (value == HdpsVariable.valueMissing)
				var.d1Missing++;
			else if (value == HdpsVariable.valueOne)
				var.d1c1++;
		} else {
			if (value == HdpsVariable.valueMissing)
				var.d0Missing++;
			else if (value == HdpsVariable.valueOne)
				var.d0c1++;
		}
	}

	protected void calculateBias(HdpsCode code) throws ClassNotFoundException {
		double nTotal = (double) this.hdpsController.getNumPatients();
		double ptTotal = (double) this.patientController.ptTotal;
		double e1Total = (double) this.patientController.nExposed;
		double ptExposed = (double) this.patientController.ptExposed;
		double e0Total = nTotal - e1Total;
		double d1Total = (double) this.patientController.nOutcome;
		double d0Total = nTotal - d1Total;
		double sumOfOutcomes = this.patientController.sumOfOutcomes;
		double numEvents = this.patientController.numEvents;

		for (HdpsVariable var: code.vars) { 
			var.bias = 0;
			var.nMissing = var.e1Missing + var.e0Missing;
			var.N = nTotal - var.nMissing;

			var.e1 = e1Total - var.e1Missing;
			var.e1c0 = var.e1 - var.e1c1;

			var.e0 = e0Total - var.e0Missing;
			var.e0c0 = var.e0 - var.e0c1;

			var.d1 = d1Total - var.d1Missing;
			var.d1c0 = var.d1 - var.d1c1;

			var.d0 = d0Total - var.d0Missing;
			var.d0c0 = var.d0 - var.d0c1;

			var.c1 = var.e1c1 + var.e0c1;
			var.c0 = var.N - var.c1;
			
			var.pt = ptTotal;
			var.pt_e1 = ptExposed;
			var.pt_e0 = ptTotal - ptExposed;
			var.pt_c0 = ptTotal - var.pt_c1;
			
			var.numEvents = numEvents;
			var.c0NumEvents = numEvents - var.c1NumEvents;
			
			// so far, these are running totals.  divide to get means.
			var.meanOutcome = sumOfOutcomes / nTotal;
			if (var.c1 > 0)
				var.c1MeanOutcome = var.c1MeanOutcome / var.c1;
			else
				var.c1MeanOutcome = -1;
			if (var.c0 > 0)
				var.c0MeanOutcome = (((var.meanOutcome * nTotal) - (var.c1MeanOutcome * var.c1)) / var.c0);
			else
				var.c0MeanOutcome = -1;

			try {
				if (var.e1 > 0)
					var.pc_e1 = var.e1c1 / var.e1;
				else
					var.pc_e1 = 0;
				
				if (var.e0 > 0)
					var.pc_e0 = var.e0c1 / var.e0;
				else
					var.pc_e0 = 0;
				
				if (var.pc_e1 > 0.5)
					var.pc_e1 = 1.0 - var.pc_e1;
				if (var.pc_e0 > 0.5)
					var.pc_e0 = 1.0 - var.pc_e0;
				
				// set default values, in case anything in here fails
				// !!! better to do this in HdpsVariable?
				var.rrCe = HdpsVariable.INVALID;
				var.expAssocRankingVariable = HdpsVariable.INVALID;
				var.rrCd = HdpsVariable.INVALID;
				var.bias = HdpsVariable.INVALID;
				var.biasRankingVariable = HdpsVariable.INVALID;
				var.outcomeAssocRankingVariable = HdpsVariable.INVALID;
				
				if ((var.pc_e0 > 0) && (var.pc_e1 > 0)) {
					var.rrCe = var.pc_e1 / var.pc_e0;
					var.expAssocRankingVariable = Math.abs(Math.log(var.rrCe));
				} 

				if ((this.hdpsController.outcomeIsDichotomous()) ||
						(this.hdpsController.outcomeIsCount()))
				{
					if (this.hdps.useOutcomeZeroCellCorrection == 1)
						var.rrCd = (((var.c1NumEvents + 0.1) / (var.pt_c1 + 0.1)) / ((var.c0NumEvents + 0.1) / (var.pt_c0 + 0.1)));
					else if ((var.c1NumEvents > 0) && (var.pt_c1 > 0) && (var.c0NumEvents > 0) &&
							 (var.pt_c0 > 0))
						var.rrCd = ((var.c1NumEvents / var.pt_c1) / (var.c0NumEvents / var.pt_c0));
					
					if ((var.rrCd > 0) && (var.rrCd < 1))
						var.rrCd = 1 / var.rrCd;
					
					if (var.rrCd > 0) 
						var.outcomeAssocRankingVariable = Math.abs(Math.log(var.rrCd));
					
				} else if (this.hdpsController.outcomeIsContinuous()) {
					var.rrCd = var.c1MeanOutcome - var.c0MeanOutcome;
					var.outcomeAssocRankingVariable = Math.abs(var.rrCd);
				}
				
				if ((var.pc_e0 > 0) && (var.pc_e1 > 0) && (var.rrCd > 0)) {
					double biasA = var.pc_e1 * (var.rrCd - 1.0) + 1.0;
					double biasB = var.pc_e0 * (var.rrCd - 1.0) + 1.0;
					var.bias = biasA / biasB;
					var.biasRankingVariable = Math.abs(Math.log(var.bias));
				}
			} catch (ArithmeticException e) {
				// already set defaults to INVALID
			}
		}
	}

	public void writeCodes(RowWriter writer)
	throws Exception
	{
		// TODO: This is inefficient.  Should be re-done.
		List<HdpsCode> codes = new ArrayList<HdpsCode>(this.codeMap.values());
		Collections.sort(codes, new HdpsCodeIdComparator());

		for (HdpsCode code : codes) 
			writer.writeRow(code.toStringArray());
	}
	
	public List<HdpsCode> getCodes()
	throws Exception
	{
		List<HdpsCode> codes = new ArrayList<HdpsCode>(this.codeMap.values());
		return codes;
	}
	
	public void freeUselessCodes() {
		// not yet implemented -- necessary? 
	}

	public Map<String, HdpsVariable> getVariablesToConsider()
	throws Exception 
	{
		Map<String, HdpsVariable> varsToConsider = new HashMap<String, HdpsVariable>();

		for (HdpsCode code : this.codeMap.values()) {
			if (code.considerForPs) {
				for (HdpsVariable var : code.vars) {
					varsToConsider.put(var.varName, var);
				}
			}
		}

		return varsToConsider;
	}

	public synchronized String generateVariableName() {
		String s = String.format("D%02dV%03d", this.dimensionId,
				this.varGenerationSequence);

		this.varGenerationSequence++;
		return s;
	}
	
	public synchronized String generateServiceIntensityVariableName(int quartile, 
			boolean uniqueOnly) {
		
		String s = String.format("D%02d_INT_%s_Q%d", 
				this.dimensionId,
				(uniqueOnly ? "UNIQ" : "ALL"),
				quartile);

		return s;
	}
	
	
	public void closeController() 
	throws Exception
	{
		this.reader.close();
	}
	
	public void setHdps(Hdps hdps) {
		this.hdps = hdps;
	}
}
