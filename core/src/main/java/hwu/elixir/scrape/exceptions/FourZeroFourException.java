package hwu.elixir.scrape.exceptions;

public class FourZeroFourException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String url;
	
	public FourZeroFourException(String url) {
		this.url = url;
	}
	
	@Override
	public String toString() {
		return url+" returns a 404";
	}

}
