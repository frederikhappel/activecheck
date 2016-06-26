package org.activecheck.plugin.reporter.jmx;

import static org.testng.Assert.assertEquals;

import org.activecheck.plugin.reporter.jmx.query.JMXQuery;
import org.activecheck.plugin.reporter.jmx.query.JMXQueryOperator;
import org.testng.annotations.Test;
import org.activecheck.common.nagios.NagiosServiceStatus;

public class JMXQueryTest {
	@Test(description = "Test initialization with null", expectedExceptions = IllegalArgumentException.class)
	public void testInitializationWithNull() {
		new JMXQuery("", false);
	}

	@Test(description = "Test initialization without object", expectedExceptions = IllegalArgumentException.class)
	public void testInitializationWithoutObject() {
		new JMXQuery("!baz", false);
	}

	@Test(description = "Test initialization without attributeName", expectedExceptions = IllegalArgumentException.class)
	public void testInitializationWithoutAttributeName() {
		new JMXQuery("foo:bar=x!!20", false);
	}

	@Test(description = "Test stringValue with complex path")
	public void testStringValueWithComplexPath() {
		JMXQuery jmxQuery = new JMXQuery(
				"org.apache.camel:context=sf-autoinput-qa-vm1.fra1.framework/camelAutoinput,type=context,name=\"camelAutoinput\"!State!!^Started",
				false);
		assertEquals(
				jmxQuery.getObject(),
				"org.apache.camel:context=sf-autoinput-qa-vm1.fra1.framework/camelAutoinput,type=context,name=\"camelAutoinput\"");
		assertEquals(jmxQuery.getAttributeName(), "State");
		assertEquals(jmxQuery.getAttributeKey(), null);
		assertEquals(jmxQuery.getWarning(), null);
		assertEquals(jmxQuery.getCritical(), "Started");
		assertEquals(jmxQuery.getDefaultValue(), null);
		assertEquals(jmxQuery.compare("Stopped"), NagiosServiceStatus.CRITICAL);
	}

	@Test(description = "Test combined IMPLIES query.")
	public void testCombinedImpliesQuery() {
		JMXQuery jmxQuery = new JMXQuery(
				"(=>(foo:bar=x!baz!WARNING!CRITICAL)(foo:bar=y!bay!true))",
				false);
		assertEquals(jmxQuery.getObject(), null);
		assertEquals(JMXQueryOperator.IMPLIES, jmxQuery.getQueryOperator());

		// test child1
		JMXQuery child1 = jmxQuery.getChildren().get(0);
		assertEquals(child1.getObject(), "foo:bar=x");
		assertEquals(child1.getAttributeName(), "baz");
		assertEquals(child1.getAttributeKey(), null);
		assertEquals(child1.getWarning(), "WARNING");
		assertEquals(child1.getCritical(), "CRITICAL");
		assertEquals(child1.getDefaultValue(), null);
		assertEquals(child1.compare("WARNING"), NagiosServiceStatus.WARNING);
		assertEquals(child1.compare("CRITICAL"), NagiosServiceStatus.CRITICAL);

		// test child2
		JMXQuery child2 = jmxQuery.getChildren().get(1);
		assertEquals(child2.getObject(), "foo:bar=y");
		assertEquals(child2.getAttributeName(), "bay");
		assertEquals(child2.getAttributeKey(), null);
		assertEquals(child2.getWarning(), "true");
		assertEquals(child2.getDefaultValue(), null);
		assertEquals(child2.compare("true"), NagiosServiceStatus.WARNING);
	}

	@Test(description = "Test complex valid query.")
	public void testComplexValidQuery() {
		JMXQuery jmxQuery = new JMXQuery(
				"(&(|(foo:bar=x!baz!WARNING!CRITICAL)(foo:bar=y!bay!WARNING!CRITICAL))(|(foo:bar=x!baz!WARNING!CRITICAL)(foo:bar=y!bay!WARNING!CRITICAL)))",
				false);

		assertEquals(2, jmxQuery.getChildren().size());
		JMXQuery child1 = jmxQuery.getChildren().get(0);
		assertEquals(2, child1.getChildren().size());
		JMXQuery child2 = jmxQuery.getChildren().get(1);
		assertEquals(2, child2.getChildren().size());
	}

	@Test(description = "Test complex invalid query.", expectedExceptions = IllegalArgumentException.class)
	public void testComplexInvalidQuery() {
		new JMXQuery(
				"(&(foo:bar=x!baz)(foo:bas=x!bay))(|(goo:bar=x!baz)(goo:bas=x!bay))",
				false);
	}

	@Test(description = "Test simple invalid query.", expectedExceptions = IllegalArgumentException.class)
	public void testSimpleInvalidQuery() {
		new JMXQuery("(foo:bar=x!baz)(foo:bas=x!bay)", false);
	}

	@Test(description = "Test invalid syntax1.", expectedExceptions = IllegalArgumentException.class)
	public void testInvalidSyntax1Query() {
		new JMXQuery("foo:bar=x!baz)", false);
	}

	@Test(description = "Test invalid syntax2.", expectedExceptions = IllegalArgumentException.class)
	public void testInvalidSyntax2Query() {
		new JMXQuery("(foo:bar=x!baz", false);
	}

	@Test(description = "Test invalid operator.", expectedExceptions = IllegalArgumentException.class)
	public void testInvalidOperatorQuery() {
		new JMXQuery("(>(foo:bar=x!baz)(foo:bas=x!bay))", false);
	}

	@Test(description = "Test valid query.")
	public void testValidQuery() {
		JMXQuery jmxQuery = new JMXQuery("foo:bar=x!baz", false);
		assertEquals(jmxQuery.getObject(), "foo:bar=x");
		assertEquals(jmxQuery.getAttributeName(), "baz");
		assertEquals(jmxQuery.getAttributeKey(), null);
		assertEquals(jmxQuery.getWarning(), null);
		assertEquals(jmxQuery.getCritical(), null);
		assertEquals(jmxQuery.getDefaultValue(), null);
	}

	@Test(description = "Test attributeKey.")
	public void testAttributeKey() {
		JMXQuery jmxQuery = new JMXQuery("foo:bar=x!baz:bay", false);
		assertEquals(jmxQuery.getObject(), "foo:bar=x");
		assertEquals(jmxQuery.getAttributeName(), "baz");
		assertEquals(jmxQuery.getAttributeKey(), "bay");
		assertEquals(jmxQuery.getWarning(), null);
		assertEquals(jmxQuery.getCritical(), null);
		assertEquals(jmxQuery.getDefaultValue(), null);
	}

	@Test(description = "Test stringValue.")
	public void testStringValue() {
		JMXQuery jmxQuery = new JMXQuery("foo:bar=x!baz!WARNING!CRITICAL",
				false);
		assertEquals(jmxQuery.getObject(), "foo:bar=x");
		assertEquals(jmxQuery.getAttributeName(), "baz");
		assertEquals(jmxQuery.getAttributeKey(), null);
		assertEquals(jmxQuery.getWarning(), "WARNING");
		assertEquals(jmxQuery.getCritical(), "CRITICAL");
		assertEquals(jmxQuery.getDefaultValue(), null);
		assertEquals(jmxQuery.compare("WARNING"), NagiosServiceStatus.WARNING);
		assertEquals(jmxQuery.compare("CRITICAL"), NagiosServiceStatus.CRITICAL);
	}

	@Test(description = "Test multipleStringValue.")
	public void testMultipleStringValue() {
		JMXQuery jmxQuery = new JMXQuery(
				"foo:bar=x!baz!WARNING|ATTENTION!CRITICAL|ERROR", false);
		assertEquals(jmxQuery.getDefaultValue(), null);
		assertEquals(jmxQuery.compare("WARNING"), NagiosServiceStatus.WARNING);
		assertEquals(jmxQuery.compare("ATTENTION"), NagiosServiceStatus.WARNING);
		assertEquals(jmxQuery.compare("CRITICAL"), NagiosServiceStatus.CRITICAL);
		assertEquals(jmxQuery.compare("ERROR"), NagiosServiceStatus.CRITICAL);
		assertEquals(jmxQuery.compare("OK"), NagiosServiceStatus.OK);
	}

	@Test(description = "Test multipleStringValueInverted.")
	public void testMultipleStringValueInverted() {
		JMXQuery jmxQuery = new JMXQuery("foo:bar=x!baz!^a|b!^a|b|c", false);
		assertEquals(jmxQuery.getDefaultValue(), null);
		assertEquals(jmxQuery.compare("c"), NagiosServiceStatus.WARNING);
		assertEquals(jmxQuery.compare("a"), NagiosServiceStatus.OK);
		assertEquals(jmxQuery.compare("b"), NagiosServiceStatus.OK);
		assertEquals(jmxQuery.compare("d"), NagiosServiceStatus.CRITICAL);
	}

	@Test(description = "Test valid query with warning inverted.")
	public void testValidQueryWarningInverted() {
		JMXQuery jmxQuery = new JMXQuery("foo:bar=x!baz!<30!<20", false);
		assertEquals(jmxQuery.getWarning(), "30");
		assertEquals(jmxQuery.getCritical(), "20");
		assertEquals(jmxQuery.compare(40), NagiosServiceStatus.OK);
		assertEquals(jmxQuery.compare(21), NagiosServiceStatus.WARNING);
		assertEquals(jmxQuery.compare(10), NagiosServiceStatus.CRITICAL);
	}

	@Test(description = "Test valid query with warning >0.")
	public void testValidQueryIntegerNull() {
		JMXQuery jmxQuery = new JMXQuery("foo:bar=x!baz!!>0", false);
		assertEquals(jmxQuery.getWarning(), null);
		assertEquals(jmxQuery.getCritical(), "0");
		assertEquals(jmxQuery.compare(0), NagiosServiceStatus.OK);
		assertEquals(jmxQuery.compare(-10), NagiosServiceStatus.OK);
		assertEquals(jmxQuery.compare(1), NagiosServiceStatus.CRITICAL);
	}

	@Test(description = "Test defaultValue.")
	public void testDefaultValue() {
		JMXQuery jmxQuery = new JMXQuery("foo:bar=x!baz!!!def", false);
		assertEquals(jmxQuery.getObject(), "foo:bar=x");
		assertEquals(jmxQuery.getAttributeName(), "baz");
		assertEquals(jmxQuery.getAttributeKey(), null);
		assertEquals(jmxQuery.getWarning(), null);
		assertEquals(jmxQuery.getCritical(), null);
		assertEquals(jmxQuery.getDefaultValue(), "def");
	}
}
