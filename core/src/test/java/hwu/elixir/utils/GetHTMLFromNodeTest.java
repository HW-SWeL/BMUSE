package hwu.elixir.utils;

import static org.junit.Assert.*;

import org.junit.Ignore;
import org.junit.Test;

import hwu.elixir.scrape.exceptions.HtmlExtractorServiceException;


/** 
 * Don't want to run this as part of build as it requires HtmlExtractor to work
 * 
 * @author kcm
 *
 */
public class GetHTMLFromNodeTest {

	@Ignore
	@Test (expected = HtmlExtractorServiceException.class)
	public void test_getHtml_shouldFail() throws HtmlExtractorServiceException {		
		GetHTMLFromNode.getHtml("http://www.hw.ac.uk/~kcm");
	}

	@Ignore
	@Test
	public void test_getHtml_shouldWork() throws HtmlExtractorServiceException {		
		String source = GetHTMLFromNode.getHtml("http://www.hw.ac.uk/").trim();
		assertTrue(source.startsWith("<!DOCTYPE html>"));
		assertTrue(source.contains("\"@context\": \"https://schema.org\","));
		assertTrue(source.endsWith("</body></html>"));
	}
	
	@Ignore
	@Test (expected = HtmlExtractorServiceException.class)
	public void test_getHtml_shouldFail_badUrl() throws HtmlExtractorServiceException {		
		GetHTMLFromNode.getHtml("http://www.......m");
	}	
}
