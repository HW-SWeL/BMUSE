package hwu.elixir.scrape.exceptions;

public class HtmlExtractorServiceException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	public HtmlExtractorServiceException(String url) {
		super("Problem with backup HtmlExtractor service; cannot obtain: " + url);
	}
}
