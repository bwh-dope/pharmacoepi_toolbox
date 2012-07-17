/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.hdps.local;

import java.io.*;

import org.apache.commons.lang.RandomStringUtils;
import org.drugepi.hdps.storage.*;

import com.sleepycat.je.*;
import com.sleepycat.persist.*;

public class HdpsLocalDatabase {
	private Environment env;
	private EntityStore store;
	
	private EnvironmentConfig envConfig;
	
	public PrimaryIndex<String, HdpsPatient> patientById;
	public PrimaryIndex<String, HdpsCodePatientLink> codePatientLinkById;
	
	public SecondaryIndex<String, String, HdpsCodePatientLink> codePatientLinkByCode;
	public SecondaryIndex<String, String, HdpsCodePatientLink> codePatientLinkByPatient;

	String homeDirectory;
	
	public HdpsLocalDatabase(String homeDirectory) {
		super();
		
		this.homeDirectory = homeDirectory;
		
		envConfig = new EnvironmentConfig();
		envConfig.setTransactional(false);
		envConfig.setAllowCreate(true);
		env = new Environment(new File(homeDirectory), envConfig);

		StoreConfig storeConfig = new StoreConfig();
//		storeConfig.setDeferredWrite(true);
		storeConfig.setAllowCreate(true);
		storeConfig.setTransactional(false);
		storeConfig.setTemporary(true);
		String storeName = String.format("HdpsStore_%s", RandomStringUtils.randomAlphabetic(8));
		store = new EntityStore(env, storeName, storeConfig);
		
		System.out.printf("NOTE: Opened database with cache size %8.2f MB (%d%% of available)\n", 
				((double) env.getMutableConfig().getCacheSize()) / (1024d * 1024d),
				env.getMutableConfig().getCachePercent());

		patientById = store.getPrimaryIndex(String.class, HdpsPatient.class);
		codePatientLinkById = store.getPrimaryIndex(String.class, HdpsCodePatientLink.class);
		
		codePatientLinkByCode = store.getSecondaryIndex(codePatientLinkById, String.class, "codeId");
		codePatientLinkByPatient = store.getSecondaryIndex(codePatientLinkById, String.class, "patientId");
	}

	public void close()
	throws Exception {
		// close the store 
		store.close();
//		
//		// remove databases 
//		String prefix = "persist#" + store.getStoreName() + "#";
//		for (String dbName : env.getDatabaseNames()) {
//			if (dbName.startsWith(prefix)) {
//				env.removeDatabase(null, dbName);
//			}
//		}
		
		// close the environment
		env.close();

		// delete all temp files
		File tempDir = new File(this.homeDirectory);
		File[] dbFiles = tempDir.listFiles(new DatabaseFileFilter());
		for (File f: dbFiles) {
			f.delete();
		}
	}
	
	private class DatabaseFileFilter implements FileFilter {
		public boolean accept(File pathname) {
			if (! pathname.isFile())
				return false;
			
			if (pathname.getName().toLowerCase().endsWith(".jdb"))
				return true;
			
			if (pathname.getName().equalsIgnoreCase("je.info.0"))
				return true;

			if (pathname.getName().equalsIgnoreCase("je.lck"))
				return true;
			
			return false;
		}
	}
}
