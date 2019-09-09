package hwu.elixir.utils;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

public class Helpers {

	/**
	 * Generates a date in the format yyyy-MM-dd.
	 * Time is not included
	 * 
	 * @return
	 */
	public static String getDateForName() {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		LocalDateTime now = LocalDateTime.now();
		return (dtf.format(now));
	}
	
	/**
	 * Generates a date in the format yyyy-MM-dd'T'HH:mm:ss.
	 * 
	 * 
	 * @return
	 */
	public static XMLGregorianCalendar getFullDateWithTime() {
		try {
			return DatatypeFactory.newInstance()
					.newXMLGregorianCalendar(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()));
		} catch (DatatypeConfigurationException e) {			
			e.printStackTrace();
		}
		return null;
	}
}
