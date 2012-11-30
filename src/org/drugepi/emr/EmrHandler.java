/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

package org.drugepi.emr;

import java.io.FileInputStream;
import java.util.*;
import java.util.concurrent.*;

import org.apache.commons.lang.StringUtils;
import org.drugepi.emr.filters.*;
import org.drugepi.emr.transformers.*;
import org.drugepi.hdps.HdpsController;
import org.drugepi.hdps.storage.HdpsVariable;
import org.drugepi.util.*;
import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.parser.Parser;
import org.jsoup.parser.XmlTreeBuilder;
import org.jsoup.select.Elements;

/**
 * Main EMR class, including main() method.
 * 
 * @author Elaine Angelino
 * @author Jeremy A. Rassen
 */

public class EmrHandler {
	// current filter and transform lists
	private ArrayList<EmrTokenFilter> filterList; 
	private ArrayList<EmrTokenTransformer> transformList; 
	
	public EmrHandler() {
		filterList = new ArrayList<EmrTokenFilter>();
		transformList = new ArrayList<EmrTokenTransformer>();
	}
	
	
	/**
	 * Class for storing output row data.  Each thread will re-use a single instance.
	 * 
	 * Base table SQL:
	 * 
	 	CREATE TABLE T_VYT6MO_EMR_NGRAMS (
		IDCODE 	VARCHAR(20),
		SCRUB_DATE VARCHAR(19),
		REPORT_TYPE VARCHAR(33),
		NGRAM_TYPE VARCHAR(35),
		NGRAM_LEVEL INTEGER,
		NGRAM VARCHAR(512)
		);
	 */
	private static class EmrOutputRow {
		private String rowData[];
		
		public EmrOutputRow() {
			rowData = new String[6];
		}
		
		public void setPatientId(String s) { this.rowData[0] = s; }
		public void setDate(String s) { this.rowData[1] = s; }
		public void setReportType(String s) { this.rowData[2] = s; }
		public void setTypeTag(String s) { this.rowData[3] = s; }
		public void setLevel(int l) { this.rowData[4] = Integer.toString(l); } 
		public void setNgram(String s) {
			// hack to fix database delimiter problem
			this.rowData[5] = s.replace("|",  "[pipe]"); 
		}
		
		public String[] toArray() {
			return rowData;
		}
	}

	private List<EmrTextBlock> processText(String s) {
		ArrayList<EmrTextBlock> list = new ArrayList<EmrTextBlock>();
		
		Document d = Jsoup.parse(s, "", Parser.xmlParser());
		
		// Get text reports
		Elements textReports = d.getElementsByTag("text_report");
		for (Element textReport: textReports) {
			// Get text items
			Elements textReportBlocks = textReport.getElementsByTag("text");
			for (Element textReportBlock: textReportBlocks) {
				EmrTextBlock textBlock = new EmrTextBlock();

				// Get title, if it exists
				Element title = textReportBlock.getElementsByTag("title").first();
				if (title != null) 
					textBlock.title = title.text();

				Elements possibleTextItems = textReportBlock.children();
				// keep anything that's not the title and that has text
				for (Element possibleTextItem: possibleTextItems) {
					if ((! possibleTextItem.tagName().equalsIgnoreCase("title")) &&
						(possibleTextItem.hasText())) {	

						textBlock.addTextItem(possibleTextItem.text());
					}
				}

				list.add(textBlock);
			}
		}
		
		return list;
	}
	
	private void writeNgrams(RowWriter writer, EmrTextBlock textBlock, int minLevel, int maxLevel, 
			String ngramTypeTag, EmrOutputRow outputRow)
	throws Exception {
		outputRow.setTypeTag(ngramTypeTag);
		
		Hashtable<Integer, List<String>> ngrams = textBlock.toNgrams(minLevel, maxLevel, this.filterList, this.transformList);
		for (int level: ngrams.keySet()) {
			outputRow.setLevel(level);
			List<String> ngramsAtLevel = ngrams.get(level);
			for (String ngram: ngramsAtLevel) {
				outputRow.setNgram(ngram);
				writer.writeRow(outputRow.toArray());
			}
		}
	}
	
	
	private static void executeThread(Properties properties, int threadNum)
	throws Exception
	{
		System.out.printf("Thread %d started\n", threadNum);
		
		String sql = String.format(
				properties.getProperty("EMR_INPUT_QUERY"),
				Integer.parseInt(properties.getProperty("EMR_NUM_THREADS")),
				threadNum);

		RowReader reader = new DatabaseRowReader(
				properties.getProperty("EMR_DB_DRIVER"),
				properties.getProperty("EMR_DB_URL"),
				properties.getProperty("EMR_DB_USER"),
				properties.getProperty("EMR_DB_PASSWORD"),
				sql);
		System.out.println("Got database reader for thread " + threadNum);
		
		NetezzaDatabaseRowWriter writer = 
				new NetezzaDatabaseRowWriter(
						properties.getProperty("EMR_DB_DRIVER"),
						properties.getProperty("EMR_DB_URL"),
						properties.getProperty("EMR_DB_USER"),
						properties.getProperty("EMR_DB_PASSWORD"),
						properties.getProperty("EMR_OUTPUT_TABLE"),
						properties.getProperty("EMR_TEMP_DIR"));

		EmrHandler handler = new EmrHandler();
		EmrOutputRow outputRow = new EmrOutputRow();
		// useful filters and transformers
		EmrTokenFilter stopWordFilter = new EmrStopWordFilter();
		EmrTokenTransformer porterStemmer = new PorterStemmerTransformer();

		long rowNum = 0;
		while (true) { // (rowNum <= 250) {
			String[] r = reader.getNextRow();
			if (r == null)
				break;

			rowNum += 1;
			if (rowNum % 100 == 1) 
				System.out.printf("Processing Thread %d Record %d\n", threadNum, rowNum);

			outputRow.setPatientId(r[0]);
			outputRow.setDate(r[1]);
			outputRow.setReportType(r[2]);
			
			List<EmrTextBlock> textBlocks = handler.processText(r[3]);
			for (EmrTextBlock textBlock: textBlocks) {
				handler.filterList.clear();
				handler.transformList.clear();

				handler.filterList.add(stopWordFilter);
				handler.writeNgrams(writer, textBlock, 1, 7, "UNSTEMMED", outputRow);
				
				handler.transformList.add(porterStemmer);
				handler.writeNgrams(writer, textBlock, 1, 7, "STEMMED", outputRow);
			}
		};
		writer.close();
    }
	
	public static void main(String[] argv)
	throws Exception {
		System.out.println("Starting");

	    final Properties properties = new Properties();
	    properties.load(new FileInputStream("testing.properties"));
	    int numThreads = Integer.parseInt(properties.getProperty("EMR_NUM_THREADS"));

	    ExecutorService executor;

        executor = Executors.newFixedThreadPool(numThreads);
        for (int i = 0; i < numThreads; i++) {
            final int threadNum = i;

            Runnable task = new Runnable() {
                public void run() {
                    try {
                    	executeThread(properties, threadNum);
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            executor.submit(task);
        }

        try {
            executor.shutdown();
            executor.awaitTermination(100 * 60 * 60, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
}
