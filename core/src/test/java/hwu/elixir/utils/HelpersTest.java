package hwu.elixir.utils;

import static org.junit.Assert.*;

import java.util.StringTokenizer;

import org.junit.Test;

public class HelpersTest {

	@Test
	public void test_getDateForName() {
		String date4Name = Helpers.getDateForName();
		
		StringTokenizer st = new StringTokenizer(date4Name, "-");
		String year = st.nextToken();
		String month = st.nextToken();
		String day = st.nextToken();
		
		assertTrue("year is 4 characters", (year.length() == 4));
		assertTrue("month is 2 characters", (month.length() == 2));
		assertTrue("day is 2 character", (day.length() == 2));
		
		Integer yearInt = Integer.valueOf(year);		
		Integer monthInt = Integer.valueOf(month);
		Integer dayInt = Integer.valueOf(day);
				
		assertTrue("year over 2018 ", (yearInt > 2018));
		assertTrue("month is between 1 and 12", (monthInt > 0 && monthInt < 13));
		assertTrue("day is between 1 and 31", (dayInt > 0 && dayInt < 32));
	}


}
