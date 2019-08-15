package hwu.elixir.scrape.exceptions;

public class MissingContextException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String url;
	
	public MissingContextException(String url) {
		this.url = url;
	}
	
	@Override
	public String toString() {
		return url + " appears to not use rdfa, but no context is found. So no markup?";
	}

}
