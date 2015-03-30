package org.activecheck.common.nagios;

import static org.testng.Assert.assertEquals;

import org.activecheck.common.nagios.NagiosServiceStatus;
import org.testng.annotations.Test;

public class NagiosServiceStatusTest {
	@Test(description = "Compare CRITICAL to OK and expect CRITICAL")
	public void testCRITICALMoreSevereThanOK() {
		assertEquals(
				NagiosServiceStatus.CRITICAL.moreSevere(NagiosServiceStatus.OK),
				NagiosServiceStatus.CRITICAL);
	}

	@Test(description = "Compare OK to WARNING and expect WARNING")
	public void testOKLessSevereThanWARNING() {
		assertEquals(
				NagiosServiceStatus.OK.moreSevere(NagiosServiceStatus.WARNING),
				NagiosServiceStatus.WARNING);
	}

	@Test(description = "Compare OK to UNKNOWN and expect UNKNOWN")
	public void testOKLessSevereThanUKNOWN() {
		assertEquals(
				NagiosServiceStatus.OK.moreSevere(NagiosServiceStatus.UNKNOWN),
				NagiosServiceStatus.UNKNOWN);
	}

	@Test(description = "Compare OK to OK and expect OK")
	public void testOKequalsOK() {
		assertEquals(NagiosServiceStatus.OK.moreSevere(NagiosServiceStatus.OK),
				NagiosServiceStatus.OK);
	}
}
