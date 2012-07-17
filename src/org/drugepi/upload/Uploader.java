/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.upload;

import java.io.*;
import java.net.*;

public class Uploader {
	public static String version = "HD Pharmacoepi Uploader version 0.1.";

	public Uploader() {
		super();
	}
	
	public static void uploadFile(String analysisNum, String filePath) 
	throws MalformedURLException, IOException {
		// CONSTANTS
		String bndry = "AaB03x";
		String url = "";

		url = "http://www.hdpharmacoepi.org/studies/studytools/addresult_fromclient/"
				+ analysisNum + "/";
//		System.out.println("URL " + url + "...");

		// CREATE AN HTTP CONNECTION
		HttpURLConnection httpcon = (HttpURLConnection) ((new URL(url)
				.openConnection()));
		httpcon.setDoOutput(true);
		httpcon.setUseCaches(false);
		httpcon.setRequestMethod("POST");
		httpcon.setRequestProperty("Content-type",
				"enctype=multipart/form-data, boundary=" + bndry); // this is
																	// new line
		httpcon.connect();

		// OPEN THE READ AND WRITE STREAMS
		System.out.println("Posting " + filePath + "...");
		File file = new File(filePath);
		FileInputStream is = new FileInputStream(file);
		OutputStream os = httpcon.getOutputStream();

		// WRITE THE FIRST/START BOUNDARY
//		String disptn = "--" + bndry
//				+ "\r\ncontent-disposition: form-data; name=\"" + paramName
//				+ "\"; filename=\"" + fileName
//				+ "\"\r\nContent-Type: text/plain\r\n\r\n";
//		System.out.print(disptn);
		// os.write(disptn.getBytes());

		// WRITE THE FILE CONTENT
		byte[] buffer = new byte[4096];
		int bytes_read;
		while ((bytes_read = is.read(buffer)) != -1) {
			os.write(buffer, 0, bytes_read);
			// System.out.print(new String(buffer, 0, bytes_read));
		}

		// WRITE THE CLOSING BOUNDARY
//		String boundar = "\r\n--" + bndry + "--";
//		System.out.print(boundar);
		// os.write(boundar.getBytes()); // another 2 new lines

		// FLUSH / CLOSE THE STREAMS
		os.flush();
		os.close();
		is.close();

		// DEBUG
//		System.out.println("\n....Djanog Done!!!...\n\n");
		dump(httpcon);
	}

	public static void dump(HttpURLConnection httpcon) 
	throws IOException {
		int n = 0; // n=0 has no key, and the HTTP return status in the value
					// field
		String headerKey;
		String headerVal;

		while (true) {
			headerKey = httpcon.getHeaderFieldKey(n);
			headerVal = httpcon.getHeaderField(n);

			if (headerKey != null || headerVal != null) {
//				System.out.println(headerKey + ": " + headerVal);
			} else {
				break;
			}

			n++;
		}

//		System.out.println();
//		System.out.println("getRequestMethod : " + httpcon.getRequestMethod());
//		System.out.println("getResponseCode : " + httpcon.getResponseCode());
//		System.out.println("getResponseMessage : "
//				+ httpcon.getResponseMessage());

		BufferedReader in = new BufferedReader(new InputStreamReader(
				httpcon.getInputStream()));
		String inputLine;

		while ((inputLine = in.readLine()) != null)
			System.out.println(inputLine);
		in.close();
	}

	public static void main(String[] args) 
	throws MalformedURLException, IOException {
		
//		Uploader u = new Uploader("5904647451",
//		"/Users/jeremy/Desktop/hdps_upload_files/estimates/output_estimates.txt");
//		u.uploadFile();

//		Uploader u = new Uploader("9998397345",
//				"/Users/wh088/Desktop/hdps_upload_files/Z Bias Diagnostics/output_z_bias.txt");
//		u.uploadFile();
//
//		Uploader u1 = new Uploader("9998397345",
//				"/Users/wh088/Desktop/hdps_upload_files/Estimates/output_estimates.txt");
//		u1.uploadFile();
//
//		Uploader u2 = new Uploader("1913111255",
//				"/Users/wh088/Desktop/hdps_upload_files/PS Variables/output_all_vars.txt");
//		u2.uploadFile();
//
//		Uploader u3 = new Uploader("1913111255",
//				"/Users/wh088/Desktop/hdps_upload_files/Dimension Variables/output_dim_3.txt");
//		u3.uploadFile();

	}
}
