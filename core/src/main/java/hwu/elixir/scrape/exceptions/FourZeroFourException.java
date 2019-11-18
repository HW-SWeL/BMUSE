package hwu.elixir.scrape.exceptions;

public class FourZeroFourException extends Exception {

	/**
	 * 404 returned by server when trying to scrape a URL.
	 * 
	 * As Selenium hides the HTML codes, this is not much use.
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public FourZeroFourException(String url) {
		super(url+" returns a 404");
	}
}
