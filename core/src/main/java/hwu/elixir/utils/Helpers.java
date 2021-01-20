package hwu.elixir.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.zip.GZIPInputStream;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

public class Helpers {

	/**
	 * Method that takes a byte array compressed gz file and returns the decompressed contents of the file
	 * in a Jsoup Document format
	 * @param compressedSourceFile
	 * @return output
	 */
	public static Document gzipFileDecompression(byte[] compressedSourceFile) {


		ByteArrayOutputStream output = new ByteArrayOutputStream();

		try{

			InputStream compressedSourceFileLocal = new ByteArrayInputStream(compressedSourceFile);
			// Create a file input stream to read the source file.
			// Create a gzip input stream to decompress the source
			// file defined by the file input stream.
			GZIPInputStream gzis = new GZIPInputStream(compressedSourceFileLocal);

			// Create a buffer and temporary variable used during the
			// file decompress process.
			byte[] buffer = new byte[1024];
			int length;

			// Read from the compressed source file and write the
			// decompress file.
			while ((length = gzis.read(buffer)) > 0) {
				output.write(buffer, 0, length);
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return Jsoup.parse(output.toString());
	}

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
