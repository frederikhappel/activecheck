package org.activecheck.common.nagios;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

public class NagiosPerformanceDataTest {
	@Test(description = "Test NAN", expectedExceptions = NagiosPerformanceDataException.class)
	public void testNAN() throws NagiosPerformanceDataException {
		new NagiosPerformanceData("nan", "234sf23", null, null, null, null);
	}

	@Test(description = "Test number with units")
	public void testNumberWithUnits() throws NagiosPerformanceDataException {
		NagiosPerformanceData perfData = new NagiosPerformanceData("nan",
				"123GB", null, null, null, null);
		assertEquals(perfData.getCurrent(), (double) 123);
	}

	@Test(description = "Test Double")
	public void testDouble() throws NagiosPerformanceDataException {
		NagiosPerformanceData perfData = new NagiosPerformanceData("nan",
				(double) 8762347862348723l, null, null, null, null);
		assertEquals(perfData.getCurrent(), (double) 8762347862348723l);
	}

	@Test(description = "Test decimal with dot")
	public void testDecimalWithDot() throws NagiosPerformanceDataException {
		NagiosPerformanceData perfData = new NagiosPerformanceData("nan",
				"8.2342", null, null, null, null);
		assertEquals(perfData.getCurrent(), 8.2342);
	}

	@Test(description = "Test decimal with comma", expectedExceptions = NagiosPerformanceDataException.class)
	public void testDecimalWithComma() throws NagiosPerformanceDataException {
		new NagiosPerformanceData("nan", "8,2342", null, null, null, null);
	}
}
