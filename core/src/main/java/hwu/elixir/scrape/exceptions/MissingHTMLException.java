package hwu.elixir.scrape.exceptions;

public class MissingHTMLException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String url;
	
	public MissingHTMLException(String url) {
		this.url = url;
	}
	
	@Override
	public String toString() {
		return "Cannot retrieve HTML for " + url;
	}

}
