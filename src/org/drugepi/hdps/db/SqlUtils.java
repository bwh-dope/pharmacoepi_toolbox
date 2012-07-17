package org.drugepi.hdps.db;

import java.sql.*;

import org.apache.commons.lang.RandomStringUtils;

public class SqlUtils {
	static int queryCounter = 0;
	
	public static String generateRandomName(int numChars)
	{
		return RandomStringUtils.randomAlphabetic(numChars);
	}
	
	public static String generateRandomName()
	{
		return (SqlUtils.generateRandomName(8));
	}
	
	public static String getTableName(String fixedName, String randomName)
	{
		return "t_" + fixedName + "_" + randomName;
	}
	
	public static String getTableName(String fixedName)
	{
		return (SqlUtils.getTableName(fixedName, SqlUtils.generateRandomName()));
	}
	
	private static long logQueryStart(int queryCounter, String sql)
	{
		long startTime = System.currentTimeMillis();
//		System.out.printf("Starting query %4d: \n%s\n", queryCounter, sql);

		return startTime;
	}
	
	private static void logQueryQueued(int queryCounter, String sql)
	{
//		System.out.printf("Queueing query %4d: \n%s\n", queryCounter, sql);
	}
	
	@SuppressWarnings("unused")
	private static void logQueryEnd(int queryCounter, long startTime)
	{
		long eTime = System.currentTimeMillis() - startTime;
        double minutes = Math.floor(eTime / (60 * 1000F));
        eTime -= minutes * 60 * 1000F;
        double seconds = eTime / 1000F;
        
//        System.out.printf("Query %4d finished.  Run time: %02d:%02.3f.\n\n", 
//        		queryCounter, (int) minutes, seconds);
	}
	
	public static void executeSql(Statement s, String sql)
	throws Exception
	{
		queryCounter++;

		long startTime = logQueryStart(queryCounter, sql);
		s.execute(sql);
		logQueryEnd(queryCounter, startTime);
	}
	
	public static ResultSet executeSqlQuery(Statement s, String sql)
	throws Exception
	{
		queryCounter++;
		
		long startTime = logQueryStart(queryCounter, sql);
		ResultSet r = s.executeQuery(sql);
		logQueryEnd(queryCounter, startTime);

        return r;
	}
	
	public static void addToSqlBatch(Statement s, String sql)
	throws Exception
	{
		logQueryQueued(queryCounter, sql);
		s.addBatch(sql);
	}
	
	public static void executeSqlBatch(Statement s)
	throws Exception 
	{
		queryCounter++;
		
		long startTime = logQueryStart(queryCounter, "SQL Batch");
		s.executeBatch();
		logQueryEnd(queryCounter, startTime);
	}
}
