package hwu.elixir.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hwu.elixir.scrape.exceptions.HtmlExtractorServiceException;

public class GetHTMLFromNode {
	
	private static final String propertiesFile = "application.properties";	
	private static Logger logger = LoggerFactory.getLogger("hwu.elixir.utils.GetHTMLFromNode");	
	private static String nodeUrl = null;
	
	static	{
		ClassLoader classLoader = GetHTMLFromNode.class.getClassLoader();

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
		logger.info("     HtmlExtractor URL: " + nodeUrl);		
	}

	/**
	 * Gets HTML from given URL by using a service; location of service in properties file.
	 * All errors from the service (eg, not a 200 etc) are wrapped in a HtmlExtractorServiceException
	 * 
	 * @param url URL to fetch HTML from
	 * @return The HTML 
	 * @throws HtmlExtractorServiceException 
	 */
	public static String getHtml(String url) throws HtmlExtractorServiceException {
		BufferedReader reader = null;

		if(nodeUrl == null) {
			throw new HtmlExtractorServiceException("Cannot read extractor service URL");
		}
		
		try {			
			URL queryURL = new URL(nodeUrl + url);
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
	
	
}
