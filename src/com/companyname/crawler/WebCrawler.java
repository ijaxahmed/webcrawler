package com.companyname.crawler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

public class WebCrawler {

	// Log initialization
	private static org.apache.log4j.Logger logger = Logger
			.getLogger(WebCrawler.class.getName());

	// instance variables
	private static HashMap<String, String> crawledResultHash = new HashMap<String, String>();
	private static BufferedWriter writer = null;
	private static File file = new File("./output/output.txt");
	private static int defaulCrawlingtLimit = 1000;

	// Main
	public static void main(String args[]) {
		logger.info("Crawler STARTED " + System.currentTimeMillis());
		// console input
		Scanner reader = new Scanner(System.in);
		String input = null;
		String newLimit = null;
		while (true) {
			System.out.println("\"y\" for Exit | Enter URL to Crawl: ");
			input = reader.next();
			if ("y".equalsIgnoreCase(input)) { // Exit Application
				System.out.println("Thank you for using IJAZ-WebCrawler.");
				System.exit(0);
			}
			if (validateURL(input)) {
				while (true) {
					System.out
							.println("\"y\" for Exit | \"d\" for Default Crawling Limit (1000) |Enter New Crawling Limit: ");
					newLimit = reader.next();
					if ("d".equalsIgnoreCase(newLimit)) {
						break;
					} else if ("y".equalsIgnoreCase(newLimit)) { // Exit
																	// Application
						System.out
								.println("Thank you for using IJAZ-WebCrawler.");
						System.exit(0);
					} else {
						try {
							defaulCrawlingtLimit = Integer.parseInt(newLimit);
							break;
						} catch (NumberFormatException e) {
							logger.error("Wrong limit entered.", e);
							System.out
									.println("Please enter correct Limit e.g. 10, 67, 1000");
							continue;
						}
					}
				}
				break;
			} else
				System.out
						.println("Please enter correct URL e.g. http(s)://www.url.com");

		}

		try { // Creating output file
			if (!file.exists()) {
				file.getParentFile().mkdirs();
			}
			writer = new BufferedWriter(new FileWriter(file));
			readUrlAsHtml(input);
		} catch (IOException e) {
			logger.error("Fail Creating OutPut File.", e);
		} finally {
			try {
				if (writer != null) { // Close writer
					writer.close();
				}
				if (reader != null)
					reader.close();
			} catch (IOException e) {
				logger.error("Fail Closing File output writer.", e);

			}
		}

		System.out
				.println("\n\nFind output file at: {PROJ_DIR}/output/output.txt");
		logger.info("Crawler END " + System.currentTimeMillis());
	}

	/**
	 * Read URL source code line by line
	 * 
	 * @param urlToRead
	 *            URL to read
	 */

	public static void readUrlAsHtml(String urlToRead) {
		URL url = null; // The URL to read
		HttpURLConnection conn = null; // The actual connection to the web page
		BufferedReader br = null; // Used to read results from the web page
		String line; // An individual line of the web page HTML
		try {
			url = new URL(urlToRead); // URL object
			conn = (HttpURLConnection) url.openConnection();// Open URL
															// connection
			conn.setRequestMethod("GET");
			br = new BufferedReader(
					new InputStreamReader(conn.getInputStream()));
			while ((line = br.readLine()) != null) { // Read HTML line by line
				if (crawledResultHash.size() < defaulCrawlingtLimit) {
					extractURLS(line); // Extract URL from HTML
				} else
					break;

			}
		} catch (Exception e) {
			if (crawledResultHash.size() < defaulCrawlingtLimit) { // Crowled
																	// list not
																	// more then
																	// Given
																	// Limit
				if (url != null && !crawledResultHash.containsValue(url)) {
					crawledResultHash.put(url.toString(), url.toString());
				}

			}
			logger.error("Cannot Open URL connection", e);
		} finally {
			if (br != null) {
				try {
					br.close(); // close buffer reader
				} catch (IOException e) {
					logger.error("Error Closing Buffer Reader", e);
				}
			}
			if (conn != null) { // close URL connection
				conn.disconnect();
			}
		}
	}

	/**
	 * Extract URLs form HTML text
	 * 
	 * @param text
	 *            HTML text which may contain URLs
	 */
	public static void extractURLS(String text) {
		// RegExp for matching URL from HTML
		String regex = "\\(?\\b(http://|https://|www[.])[-A-Za-z0-9+&@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&@#/%=~_()|]";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(text);
		while (m.find()) {
			String urlStr = m.group(); // found URL String
			if (urlStr.startsWith("(") && urlStr.endsWith(")")) // Eliminating
																// any brackets
																// around
			{
				urlStr = urlStr.substring(1, urlStr.length() - 1);
			}
			System.out.println(urlStr);
			if (crawledResultHash.size() < defaulCrawlingtLimit
					&& !crawledResultHash.containsValue(urlStr)) {

				crawledResultHash.put(urlStr, urlStr);

				try {
					writer.write(urlStr + "\n"); // Write URL to Output File
				} catch (IOException e) {
					logger.error("Error Writing to output file", e);
				}
				if (!isImageURL(urlStr)
						&& !crawledResultHash.containsValue(urlStr))
					readUrlAsHtml(urlStr); // Recursively call HTML reader for
											// URL
			} else { // if Crawler list size has reached
				break;
			}

		}

	}

	/**
	 * Validate if provided URL is a correct URL by syntax
	 * 
	 * @param url
	 *            URL to validate
	 * @return True if valid URL Syntax False if wrong URL Syntax
	 */
	public static boolean validateURL(String url) {
		boolean isCorrectURL = false;
		// RegExp to check URL
		Pattern pattern = Pattern
				.compile("^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
		Matcher matcher = pattern.matcher(url);
		if (matcher.find()) {
			isCorrectURL = true;
		}
		return isCorrectURL;

	}

	/**
	 * Verify if URL is NON-HTML URL e.g. Image, DTD etc
	 * 
	 * @param url
	 *            URL to verify
	 * @return True if NON-HTML URL False if HTML URL
	 */
	public static boolean isImageURL(String url) {
		// RegExp to check image URL
		String regex = "http(s?)://([\\w-]+\\.)+[\\w-]+(/[\\w- ./]*)+\\.(?:[gG][iI][fF]|[jJ][pP][gG]|[jJ][pP][eE][gG]|[pP][nN][gG]|[bB][mM][pP]|[sS][wW][fF]|[dD][tT][dD])";
		Matcher m = Pattern.compile(regex).matcher(url);

		if (m.find()) {
			logger.debug(m.group(0) + " is a NON-HTML URL");
			return true;
		} else
			return false;

	}

}
