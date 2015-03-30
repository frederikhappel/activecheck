package org.activecheck;

import static org.testng.Assert.assertEquals;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class PropertiesParseTest {
	private PropertiesConfiguration properties;

	/**
	 * Prepares the mocks, buffers and test subject.
	 */
	@BeforeMethod
	public void setUp() throws Exception {
		properties = new PropertiesConfiguration();
		properties.load("resources/conf.d/jmx_pidfile.conf");
	}

	@Test(description = "Test reading a simple string attribute.")
	public void testReadSimpleStringAttribute() throws Exception {
		String result = properties.getString("jmx.username");
		assertEquals(result, "monitor");
	}
}
