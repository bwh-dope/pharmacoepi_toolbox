/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

package org.drugepi.hdps;

import java.util.List;
import org.drugepi.util.RowWriter;

import org.drugepi.hdps.storage.HdpsCode;
import org.drugepi.util.RowReader;

public abstract class HdpsDimensionController {
	// VARIABLES WITH GETTERS AND SETTERS
	public Hdps hdps;
	public RowReader reader;
	public HdpsPatientController patientController;
	
	public int dimensionId;
	public String dimensionDescription;

	protected static final int patientColumn = 0;
	protected static final int codeColumn = 1;
	protected static final int dateColumn = 2;

	// INTERNAL VARIABLES
	public HdpsDimensionController(Hdps hdps) 
	{
		this.hdps = hdps;
	}
	
	public abstract void readDimension() throws Exception;
	
	public abstract void writeCodes(RowWriter writer) throws Exception; 

	// TODO: This method can likely be deleted.
	public abstract List<HdpsCode> getCodes() throws Exception; 
	
	public abstract void freeUselessCodes();

	public abstract void closeController() throws Exception;
	
	public void setHdps(Hdps hdps) {
		this.hdps = hdps;
	}
}
