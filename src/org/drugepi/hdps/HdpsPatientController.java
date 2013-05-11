/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.hdps;

import org.drugepi.util.RowReader;

public abstract class HdpsPatientController
{
    // VARIABLES WITH GETTERS AND SETTERS
	public Hdps hdps;
    protected static final int KEY_COLUMN_NUM = 0;
    protected static final int EXPOSED_COLUMN_NUM = 1;
    protected static final int OUTCOME_COLUMN_NUM = 2;
    protected static final int TIME_COLUMN_NUM = 3;
    
    public int nExposed;
    public int nOutcome;
	public int ptTotal = 0;
	public int ptExposed = 0;
	public double sumOfOutcomes = 0;
	public int numEvents = 0;
	
    private int numPatients;

	public RowReader reader;

    public HdpsPatientController(Hdps hdps) {
    	this.hdps = hdps;
    	this.numPatients = -1;
    }
    
	public abstract void readPatients() throws Exception;
	
	public abstract void closeController() throws Exception;
	
	public void setHdps(Hdps hdps) {
		this.hdps = hdps;
	}

	public void setNumPatients(int numPatients) {
		this.numPatients = numPatients;
	}


	public int getNumPatients() {
		return numPatients;
	}
	
 }

