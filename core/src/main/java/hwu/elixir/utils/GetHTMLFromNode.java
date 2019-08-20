package hwu.elixir.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hwu.elixir.scrape.ThreadedScrapeDriver;
import hwu.elixir.scrape.exceptions.HtmlExtractorServiceException;

public class GetHTMLFromNode {
	
	private static final String propertiesFile = "application.properties";	
	private static Logger logger = LoggerFactory.getLogger("hwu.elixir.utils.GetHTMLFromNode");	
	
	private String nodeUrl = "";
	
	public GetHTMLFromNode() {
		processProperties();
	}

	public String getHtml(String url) throws HtmlExtractorServiceException {
		BufferedReader reader = null;

		try {
			String myUrl = URLEncoder.encode(nodeUrl + url, "UTF-8");
			myUrl = nodeUrl + url;
			URL queryURL = new URL(myUrl);
			HttpURLConnection connection = (HttpURLConnection) queryURL.openConnection();
			connection.setRequestMethod("GET");
			connection.setReadTimeout(10 * 1000);
			connection.connect();

			int code = connection.getResponseCode();
			if (code == 200) {
				reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				StringBuilder stringBuilder = new StringBuilder();
				String line = null;
				while ((line = reader.readLine()) != null) {
					stringBuilder.append(line + "\n");
				}
				return stringBuilder.toString();
			} else {
				logger.error("HTMLExtractor returned a "+ code +" status code for "+url);
				throw new HtmlExtractorServiceException(url);
			}

		} catch (UnsupportedEncodingException e) {
			logger.error("Unsupported encoding for "+url);
			throw new HtmlExtractorServiceException(url);
		} catch (MalformedURLException e) {
			logger.error("Malformed url for "+url);
			throw new HtmlExtractorServiceException(url);
		} catch (IOException e) {
			logger.error("IO exception for "+url);
			throw new HtmlExtractorServiceException(url);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	
	private void processProperties() {
		ClassLoader classLoader = ThreadedScrapeDriver.class.getClassLoader();

		URL resource = classLoader.getResource(propertiesFile);
		if (resource == null) {
			logger.error("     Cannot find " + propertiesFile + " file");
			throw new IllegalArgumentException(propertiesFile + "file is not found!");
		}

		FileInputStream in = null;
		try {
			in = new FileInputStream(new File(resource.getFile()));
		} catch (FileNotFoundException e) {
			logger.error("     Cannot read application.properties file", e);
			System.exit(0);
		}
		Properties prop = new Properties();

		try {
			prop.load(in);
		} catch (IOException e) {
			logger.error("     Cannot load application.properties", e);
			System.exit(0);
		}

		nodeUrl = prop.getProperty("htmlExtractorServiceURI").trim();
		logger.info("     backup HtmlExtractor URL: " + nodeUrl);		
	}	
}
