package org.activecheck.common.nagios;

import static org.testng.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.activecheck.common.nagios.NagiosServiceReport;
import org.activecheck.common.nagios.NagiosServiceStatus;
import org.testng.annotations.Test;

public class NagiosServiceReportTest {
	@Test(description = "Sort a list with NagiosServiceReports")
	public void testSortOrder() {
		List<NagiosServiceReport> reports = new ArrayList<NagiosServiceReport>();
		reports.add(new NagiosServiceReport("test", "testhost",
				NagiosServiceStatus.UNKNOWN));
		reports.add(new NagiosServiceReport("test", "testhost",
				NagiosServiceStatus.OK));
		reports.add(new NagiosServiceReport("test", "testhost",
				NagiosServiceStatus.CRITICAL));
		reports.add(new NagiosServiceReport("test", "testhost",
				NagiosServiceStatus.WARNING));
		reports.add(new NagiosServiceReport("test", "testhost",
				NagiosServiceStatus.OK));
		reports.add(new NagiosServiceReport("test", "testhost",
				NagiosServiceStatus.WARNING));
		reports.add(new NagiosServiceReport("test", "testhost",
				NagiosServiceStatus.CRITICAL));
		reports.add(new NagiosServiceReport("test", "testhost",
				NagiosServiceStatus.UNKNOWN));
		Collections.sort(reports);
		assertEquals(reports.get(0).getStatus(), NagiosServiceStatus.CRITICAL);
		assertEquals(reports.get(1).getStatus(), NagiosServiceStatus.CRITICAL);
		assertEquals(reports.get(2).getStatus(), NagiosServiceStatus.WARNING);
		assertEquals(reports.get(3).getStatus(), NagiosServiceStatus.WARNING);
		assertEquals(reports.get(4).getStatus(), NagiosServiceStatus.UNKNOWN);
		assertEquals(reports.get(5).getStatus(), NagiosServiceStatus.UNKNOWN);
		assertEquals(reports.get(6).getStatus(), NagiosServiceStatus.OK);
		assertEquals(reports.get(7).getStatus(), NagiosServiceStatus.OK);
	}
}
