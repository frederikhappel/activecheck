package org.activecheck.common.nagios;

import static org.testng.Assert.assertEquals;

import org.activecheck.common.nagios.NagiosPerformanceData;
import org.testng.annotations.Test;

public class NagiosPerformanceDataTest {
	@Test(description = "Test NAN", expectedExceptions = NumberFormatException.class)
	public void testNAN() {
		new NagiosPerformanceData("nan", "234sf23", null, null, null, null);
	}

	@Test(description = "Test number with units")
	public void testNumberWithUnits() {
		NagiosPerformanceData perfData = new NagiosPerformanceData("nan",
				"123GB", null, null, null, null);
		assertEquals(perfData.getCurrent(), (double) 123);
	}

	@Test(description = "Test Double")
	public void testDouble() {
		NagiosPerformanceData perfData = new NagiosPerformanceData("nan",
				(double) 8762347862348723l, null, null, null, null);
		assertEquals(perfData.getCurrent(), (double) 8762347862348723l);
	}

	@Test(description = "Test decimal with dot")
	public void testDecimalWithDot() {
		NagiosPerformanceData perfData = new NagiosPerformanceData("nan",
				"8.2342", null, null, null, null);
		assertEquals(perfData.getCurrent(), 8.2342);
	}

	@Test(description = "Test decimal with comma", expectedExceptions = NumberFormatException.class)
	public void testDecimalWithComma() {
		new NagiosPerformanceData("nan", "8,2342", null, null, null, null);
	}
}
