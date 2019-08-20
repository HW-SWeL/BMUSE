package hwu.elixir.scrape.exceptions;

public class HtmlExtractorServiceException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String url;
	
	public HtmlExtractorServiceException(String url) {
		this.url = url;
	}
	
	@Override
	public String toString() {
		return "Problem with HtmlExtractor service; cannot obtain: " + url;
	}

}
