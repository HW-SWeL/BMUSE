package hwu.elixir.scrape.exceptions;

public class JsonLDInspectionException extends Exception {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String url;
	
	public JsonLDInspectionException(String url) {
		this.url = url;
	}
	
	@Override
	public String toString() {
		return url + " needs human inspection";
	}	
}
