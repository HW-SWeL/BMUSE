package hwu.elixir.scrape.exceptions;

public class FourZeroFourException extends Exception {

	/**
	 * 404 returned by server when trying to scrape a URL.
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public FourZeroFourException(String url) {
		super(url+" returns a 404");
	}
}
