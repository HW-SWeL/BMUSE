package hwu.elixir.scrape.exceptions;

public class FourZeroFourException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public FourZeroFourException(String url) {
		super(url+" returns a 404");
	}
}
