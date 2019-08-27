package hwu.elixir.scrape.scraper;

import java.util.StringTokenizer;

import org.apache.any23.source.DocumentSource;
import org.apache.any23.source.StringDocumentSource;
import org.json.simple.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hwu.elixir.scrape.exceptions.FourZeroFourException;
import hwu.elixir.scrape.exceptions.HtmlExtractorServiceException;
import hwu.elixir.scrape.exceptions.JsonLDInspectionException;
import hwu.elixir.scrape.exceptions.MissingContextException;
import hwu.elixir.scrape.exceptions.MissingHTMLException;
import hwu.elixir.utils.GetHTMLFromNode;

public class WebScraper extends ScraperCore {

	private static final Logger logger = LoggerFactory.getLogger(System.class.getName());
	private GetHTMLFromNode getHtmlFromNode = new GetHTMLFromNode();

	public JSONArray scrape(String url)
			throws HtmlExtractorServiceException, FourZeroFourException, JsonLDInspectionException, MissingContextException, MissingHTMLException {

		if (url.endsWith("/") || url.endsWith("#"))
			url = url.substring(0, url.length() - 1);

		
		String html = getHtml(url);		
		
		try {
			html = injectId(html, url);
		} catch (MissingContextException | MissingHTMLException e) {			
			logger.info("using puppeteer");
			return scrapeWithPupeteer(url);	
		}

		DocumentSource source = new StringDocumentSource(html, url);

		String n3 = getTriplesInN3(source);
		if (n3 == null)
			return scrapeWithPupeteer(url);
			

		return writeN3ToJSONArray(n3);
	}

	private JSONArray scrapeWithPupeteer(String url) throws HtmlExtractorServiceException, JsonLDInspectionException, MissingContextException, MissingHTMLException {
		String html = getHtmlFromNode.getHtml(url);

		html = injectId(html, url);
		
		DocumentSource source = new StringDocumentSource(html, url);

		String n3 = getTriplesInN3(source);
		if (n3 == null)
			return null;	

		return writeN3ToJSONArray(n3);
	}

	
	private JSONArray writeN3ToJSONArray(String n3) {
		JSONArray array = new JSONArray();
		StringTokenizer st = new StringTokenizer(n3, "\n");
		while(st.hasMoreTokens()) {
			array.add(st.nextToken());
		}
		return array;
	}
}
