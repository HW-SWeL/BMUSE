package hwu.elixir.utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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
	 * Method that takes a gz file and returns the decompressed contents of the file
	 * @param sourceFile
	 * @return decomressedFile
	 */
	public static String gzipFileDecompression(String sourceFile) {


		String decompressedFile = sourceFile.substring(0, sourceFile.length() - 3);

		//String content = new String(new GZIP().decompresGzipToBytes(FileUtils.readFileToByteArray(fileName)), "UTF-8");

		try{
			// Create a file input stream to read the source file.
			// Create a gzip input stream to decompress the source
			// file defined by the file input stream.
			GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(sourceFile));

			// Create file output stream where the decompress result
			// will be stored.
			FileOutputStream fos = new FileOutputStream(decompressedFile);

			// Create a buffer and temporary variable used during the
			// file decompress process.
			byte[] buffer = new byte[1024];
			int length;

			// Read from the compressed source file and write the
			// decompress file.
			while ((length = gzis.read(buffer)) > 0) {
				fos.write(buffer, 0, length);
			}

			//gzis.close();
			//fos.close();

		} catch(IOException e){
			e.printStackTrace();
		}

		return decompressedFile;
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
