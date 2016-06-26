package org.activecheck.plugin.reporter.jmx;

import org.activecheck.common.nagios.NagiosPerformanceData;
import org.activecheck.common.nagios.NagiosServiceStatus;
import org.activecheck.plugin.reporter.jmx.common.JMXProvider;
import org.activecheck.plugin.reporter.jmx.query.JMXQuery;
import org.activecheck.plugin.reporter.jmx.query.JMXQueryExecutor;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;
import java.util.Map;

import static javax.management.remote.JMXConnector.CREDENTIALS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
 * Tests for the JMXQuery class.
 */
@Test
public class JMXQueryExecutorTest {
    /**
     * The subject under test
     */
    private JMXQueryExecutor jmxQueryExecutor;

    /**
     * Mocked JMXProvider
     */
    @Mock
    private JMXProvider jmxProvider;

    /**
     * Mocked JMXConnector
     */
    @Mock
    private JMXConnector jmxConnector;

    /**
     * Mocked MBeanServerConnection
     */
    @Mock
    private MBeanServerConnection mBeanServerConnection;

    /**
     * Prepares the mocks, buffers and test subject.
     */
    @SuppressWarnings("unchecked")
    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(jmxProvider.getConnector(any(JMXServiceURL.class), any(Map.class)))
                .thenReturn(jmxConnector);
        when(jmxConnector.getMBeanServerConnection()).thenReturn(
                mBeanServerConnection);

        jmxQueryExecutor = new JMXQueryExecutor(jmxProvider,
                "service:jmx:some://domain.com");
        jmxQueryExecutor.authenticate("duke", "java_rulez");
    }

    @Test(description = "Test credentials used.")
    @SuppressWarnings("unchecked")
    public void testCredentialsUsed() throws Exception {
        ArgumentCaptor<Map<String, ?>> envCaptor = (ArgumentCaptor<Map<String, ?>>) (ArgumentCaptor<?>) ArgumentCaptor
                .forClass(Map.class);
        verify(jmxProvider).getConnector(any(JMXServiceURL.class),
                envCaptor.capture());
        assertEquals(envCaptor.getValue().get(CREDENTIALS), new String[]{
                "duke", "java_rulez"});
    }

    @Test(description = "Test positive AND OR  query. (true || false) && (true || false)")
    public void testPositiveAndOrQuery() throws Exception {
        when( // first evaluates to true
                mBeanServerConnection.getAttribute(
                        eq(ObjectName.getInstance("foo:bar=x")), eq("baz")))
                .thenReturn("OK");
        when( // second evaluates to true
                mBeanServerConnection.getAttribute(
                        eq(ObjectName.getInstance("foo:bar=y")), eq("bay")))
                .thenReturn("WARNING");

        // expect true : true && true
        JMXQuery jmxQuery = new JMXQuery(
                "(&(|(foo:bar=x!baz!WARNING!CRITICAL)(foo:bar=y!bay!WARNING!CRITICAL))(|(foo:bar=x!baz!WARNING!CRITICAL)(foo:bar=y!bay!WARNING!CRITICAL)))",
                false);
        assertEquals(jmxQuery.execute(jmxQueryExecutor), true);
        assertEquals(jmxQuery.getCheckResult().getStatus(),
                NagiosServiceStatus.OK);
    }

    @Test(description = "Test positive AND query. (true && true)")
    public void testPositiveAndTrueTrueQuery() throws Exception {
        when( // first evaluates to true
                mBeanServerConnection.getAttribute(
                        eq(ObjectName.getInstance("foo:bar=x")), eq("baz")))
                .thenReturn("OK");
        when( // second evaluates to true
                mBeanServerConnection.getAttribute(
                        eq(ObjectName.getInstance("foo:bar=y")), eq("bay")))
                .thenReturn("OK");

        // expect true : true && true
        JMXQuery jmxQuery = new JMXQuery(
                "(&(foo:bar=x!baz!WARNING!CRITICAL)(foo:bar=y!bay!WARNING!CRITICAL))",
                false);
        assertEquals(jmxQuery.execute(jmxQueryExecutor), true);
        assertEquals(jmxQuery.getCheckResult().getStatus(),
                NagiosServiceStatus.OK);
    }

    @Test(description = "Test negative AND query. (false && true)")
    public void testNegativeAndFalseTrueQuery() throws Exception {
        when( // first evaluates to false
                mBeanServerConnection.getAttribute(
                        eq(ObjectName.getInstance("foo:bar=x")), eq("baz")))
                .thenReturn("WARNING");
        when( // second evaluates to true
                mBeanServerConnection.getAttribute(
                        eq(ObjectName.getInstance("foo:bar=y")), eq("bay")))
                .thenReturn("OK");

        // expect false : false && true
        JMXQuery jmxQuery = new JMXQuery(
                "(&(foo:bar=x!baz!WARNING!CRITICAL)(foo:bar=y!bay!WARNING!CRITICAL))",
                false);
        assertEquals(jmxQuery.execute(jmxQueryExecutor), false);
        assertEquals(jmxQuery.getCheckResult().getStatus(),
                NagiosServiceStatus.WARNING);
    }

    @Test(description = "Test positive OR query. (false || true)")
    public void testPositiveOrFalseTrueQuery() throws Exception {
        when( // first evaluates to false
                mBeanServerConnection.getAttribute(
                        eq(ObjectName.getInstance("foo:bar=x")), eq("baz")))
                .thenReturn("WARNING");
        when( // second evaluates to true
                mBeanServerConnection.getAttribute(
                        eq(ObjectName.getInstance("foo:bar=y")), eq("bay")))
                .thenReturn("OK");

        // expect true : false || true
        JMXQuery jmxQuery = new JMXQuery(
                "(|(foo:bar=x!baz!WARNING!CRITICAL)(foo:bar=y!bay!WARNING!CRITICAL))",
                false);
        assertEquals(jmxQuery.execute(jmxQueryExecutor), true);
        assertEquals(jmxQuery.getCheckResult().getStatus(),
                NagiosServiceStatus.OK);
    }

    @Test(description = "Test negative OR query. (false || false)")
    public void testNegativeOrFalseFalseQuery() throws Exception {
        when( // first evaluates to false
                mBeanServerConnection.getAttribute(
                        eq(ObjectName.getInstance("foo:bar=x")), eq("baz")))
                .thenReturn("WARNING");
        when( // second evaluates to false
                mBeanServerConnection.getAttribute(
                        eq(ObjectName.getInstance("foo:bar=y")), eq("bay")))
                .thenReturn("WARNING");

        // expect false : false || false
        JMXQuery jmxQuery = new JMXQuery(
                "(|(foo:bar=x!baz!WARNING!CRITICAL)(foo:bar=y!bay!WARNING!CRITICAL))",
                false);
        assertEquals(jmxQuery.execute(jmxQueryExecutor), false);
        assertEquals(jmxQuery.getCheckResult().getStatus(),
                NagiosServiceStatus.WARNING);
    }

    @Test(description = "Test positive IMPLIES query. (false => false)")
    public void testPositiveImpliesFalseFalseQuery() throws Exception {
        when( // first evaluates to false
                mBeanServerConnection.getAttribute(
                        eq(ObjectName.getInstance("foo:bar=x")), eq("baz")))
                .thenReturn("WARNING");
        when( // second evaluates to false
                mBeanServerConnection.getAttribute(
                        eq(ObjectName.getInstance("foo:bar=y")), eq("bay")))
                .thenReturn("CRITICAL");

        // expect true : false => false
        JMXQuery jmxQuery = new JMXQuery(
                "(=>(foo:bar=x!baz!WARNING!CRITICAL)(foo:bar=y!bay!WARNING!CRITICAL))",
                false);
        assertEquals(jmxQuery.execute(jmxQueryExecutor), true);
        assertEquals(jmxQuery.getCheckResult().getStatus(),
                NagiosServiceStatus.OK);
    }

    @Test(description = "Test positive IMPLIES query. (false => true)")
    public void testPositiveImpliesFalseTrueQuery() throws Exception {
        when( // first evaluates to false
                mBeanServerConnection.getAttribute(
                        eq(ObjectName.getInstance("foo:bar=x")), eq("baz")))
                .thenReturn("WARNING");
        when( // second evaluates to true
                mBeanServerConnection.getAttribute(
                        eq(ObjectName.getInstance("foo:bar=y")), eq("bay")))
                .thenReturn("OK");

        // expect true : false => true
        JMXQuery jmxQuery = new JMXQuery(
                "(=>(foo:bar=x!baz!WARNING!CRITICAL)(foo:bar=y!bay!WARNING!CRITICAL))",
                false);
        assertEquals(jmxQuery.execute(jmxQueryExecutor), true);
        assertEquals(jmxQuery.getCheckResult().getStatus(),
                NagiosServiceStatus.OK);
    }

    @Test(description = "Test positive IMPLIES query. (true => true)")
    public void testPositiveImpliesTrueTrueQuery() throws Exception {
        when( // first evaluates to false
                mBeanServerConnection.getAttribute(
                        eq(ObjectName.getInstance("foo:bar=x")), eq("baz")))
                .thenReturn("OK");
        when( // second evaluates to true
                mBeanServerConnection.getAttribute(
                        eq(ObjectName.getInstance("foo:bar=y")), eq("bay")))
                .thenReturn("OK");

        // expect true : false => true
        JMXQuery jmxQuery = new JMXQuery(
                "(=>(foo:bar=x!baz!WARNING!CRITICAL)(foo:bar=y!bay!WARNING!CRITICAL))",
                false);
        assertEquals(jmxQuery.execute(jmxQueryExecutor), true);
        assertEquals(jmxQuery.getCheckResult().getStatus(),
                NagiosServiceStatus.OK);
    }

    @Test(description = "Test negative IMPLIES query. (true => false)")
    public void testPositiveImpliesTrueFalseQuery() throws Exception {
        when( // first evaluates to false
                mBeanServerConnection.getAttribute(
                        eq(ObjectName.getInstance("foo:bar=x")), eq("baz")))
                .thenReturn("OK");
        when( // second evaluates to true
                mBeanServerConnection.getAttribute(
                        eq(ObjectName.getInstance("foo:bar=y")), eq("bay")))
                .thenReturn("WARNING");

        // expect true : false => true
        JMXQuery jmxQuery = new JMXQuery(
                "(=>(foo:bar=x!baz!WARNING!CRITICAL)(foo:bar=y!bay!WARNING!CRITICAL))",
                false);
        assertEquals(jmxQuery.execute(jmxQueryExecutor), false);
        assertEquals(jmxQuery.getCheckResult().getStatus(),
                NagiosServiceStatus.WARNING);
        assertEquals(jmxQuery.getCheckResult().getMessage(),
                "x:baz=OK => y:bay=WARNING");
    }

    @Test(description = "Test true IMPLIES exception. (true => false) == false")
    public void testImpliesTrueExceptionQuery() throws Exception {
        when( // first evaluates to false
                mBeanServerConnection.getAttribute(
                        eq(ObjectName.getInstance("foo:bar=x")), eq("baz")))
                .thenReturn(true);
        when( // second evaluates to false
                mBeanServerConnection.getAttribute(
                        eq(ObjectName.getInstance("foo:bar=y")), eq("bay")))
                .thenThrow(new ArrayIndexOutOfBoundsException());

        // expect true : false => true
        JMXQuery jmxQuery = new JMXQuery("(=>(foo:bar=x!baz)(foo:bar=y!bay))",
                false);
        assertEquals(jmxQuery.execute(jmxQueryExecutor), false);
        assertEquals(jmxQuery.getCheckResult().getStatus(),
                NagiosServiceStatus.CRITICAL);
    }

    @Test(description = "Test false IMPLIES exception. (false => false) == true")
    public void testImpliesFalseExceptionQuery() throws Exception {
        when( // first evaluates to false
                mBeanServerConnection.getAttribute(eq(ObjectName.getInstance("foo:bar=x")), eq("baz")))
                .thenReturn(false);
        when( // second evaluates to false
                mBeanServerConnection.getAttribute(eq(ObjectName.getInstance("foo:bar=y")), eq("bay")))
                .thenThrow(new ArrayIndexOutOfBoundsException());

        // expect true : false => true
        JMXQuery jmxQuery = new JMXQuery("(=>(foo:bar=x!baz!false)(foo:bar=y!bay))", false);
        assertEquals(jmxQuery.execute(jmxQueryExecutor), true);
        assertEquals(jmxQuery.getCheckResult().getStatus(), NagiosServiceStatus.OK);
    }

    @Test
    public void testReadSimpleBooleanAttribute() throws Exception {
        when(
                mBeanServerConnection.getAttribute(
                        eq(ObjectName.getInstance("foo:bar=x")), eq("baz")))
                .thenReturn(true);
        JMXQuery jmxQuery = new JMXQuery("foo:bar=x!baz", false);
        jmxQuery.execute(jmxQueryExecutor);
        assertEquals(jmxQuery.getCheckResult().getStatus(),
                NagiosServiceStatus.OK);
        assertEquals(jmxQuery.getCheckResult().getMessage(), "x:baz=true");
    }

    @Test
    public void testReadSimpleStringAttribute() throws Exception {
        when(
                mBeanServerConnection.getAttribute(
                        eq(ObjectName.getInstance("foo:bar=x")), eq("baz")))
                .thenReturn("STRING");
        JMXQuery jmxQuery = new JMXQuery("foo:bar=x!baz", false);
        jmxQuery.execute(jmxQueryExecutor);
        assertEquals(jmxQuery.getCheckResult().getStatus(),
                NagiosServiceStatus.OK);
        assertEquals(jmxQuery.getCheckResult().getMessage(), "x:baz=STRING");
    }

    @Test
    public void testWarnOnSimpleStringAttribute() throws Exception {
        when(
                mBeanServerConnection.getAttribute(
                        eq(ObjectName.getInstance("foo:bar=x")), eq("baz")))
                .thenReturn("STRING");
        JMXQuery jmxQuery = new JMXQuery("foo:bar=x!baz!STRING", false);
        jmxQuery.execute(jmxQueryExecutor);
        assertEquals(jmxQuery.getCheckResult().getStatus(),
                NagiosServiceStatus.WARNING);
        assertEquals(jmxQuery.getCheckResult().getMessage(), "x:baz=STRING");
    }

    @Test
    public void testWarnOnSimpleInvertedStringAttribute() throws Exception {
        when(
                mBeanServerConnection.getAttribute(
                        eq(ObjectName.getInstance("foo:bar=x")), eq("baz")))
                .thenReturn("STRING");
        JMXQuery jmxQuery = new JMXQuery("foo:bar=x!baz!^asdasd", false);
        jmxQuery.execute(jmxQueryExecutor);
        assertEquals(jmxQuery.getCheckResult().getStatus(),
                NagiosServiceStatus.WARNING);
        assertEquals(jmxQuery.getCheckResult().getMessage(), "x:baz=STRING");
    }

    @Test
    public void testReadSimpleNumericAttribute() throws Exception {
        when(
                mBeanServerConnection.getAttribute(
                        eq(ObjectName.getInstance("foo:bar=x")), eq("baz")))
                .thenReturn(42);
        JMXQuery jmxQuery = new JMXQuery("foo:bar=x!baz", false);
        jmxQuery.execute(jmxQueryExecutor);
        assertEquals(jmxQuery.getCheckResult().getStatus(),
                NagiosServiceStatus.OK);
        assertEquals(jmxQuery.getCheckResult().getMessage(), "x:baz=42");
    }

    @Test(description = "Test reading a simple numeric attribute on warn level.")
    public void testWarnOnSimpleNumericAttribute() throws Exception {
        when(
                mBeanServerConnection.getAttribute(
                        eq(ObjectName.getInstance("foo:bar=x")), eq("baz")))
                .thenReturn(42);
        JMXQuery jmxQuery = new JMXQuery("foo:bar=x!baz!25!50", false);
        jmxQuery.execute(jmxQueryExecutor);
        assertEquals(jmxQuery.getCheckResult().getStatus(),
                NagiosServiceStatus.WARNING);
        assertEquals(jmxQuery.getCheckResult().getMessage(), "x:baz=42");
    }

    @Test(description = "Test performance data with integers.")
    public void testPerformanceDataInteger() throws Exception {
        int returnValue = 42;
        when(
                mBeanServerConnection.getAttribute(
                        eq(ObjectName.getInstance("foo:bar=x")), eq("baz")))
                .thenReturn(returnValue);
        JMXQuery jmxQuery = new JMXQuery("foo:bar=x!baz!25!50", false);
        jmxQuery.execute(jmxQueryExecutor);
        assertEquals(jmxQuery.getCheckResult().getStatus(),
                NagiosServiceStatus.WARNING);
        assertEquals(jmxQuery.getCheckResult().getMessage(), "x:baz="
                + returnValue);
        assertEquals(jmxQuery.getCheckResult().getPerfData().get(0).toString(),
                "baz=" + returnValue + ";26;51");
    }

    @Test(description = "Test performance data with float.")
    public void testPerformanceDataFloat() throws Exception {
        double returnValue = 42.02;
        when(
                mBeanServerConnection.getAttribute(
                        eq(ObjectName.getInstance("foo:bar=x")), eq("baz")))
                .thenReturn(returnValue);
        JMXQuery jmxQuery = new JMXQuery("foo:bar=x!baz!!<50", false);
        jmxQuery.execute(jmxQueryExecutor);
        String expect = String.format("baz=%f;%f;%f", returnValue,
                50 - NagiosPerformanceData.PRECISION,
                50 - NagiosPerformanceData.PRECISION);
        assertEquals(jmxQuery.getCheckResult().getStatus(),
                NagiosServiceStatus.CRITICAL);
        assertEquals(jmxQuery.getCheckResult().getMessage(), "x:baz="
                + returnValue);
        assertEquals(jmxQuery.getCheckResult().getPerfData().get(0).toString(),
                expect);
    }

    @Test(description = "Default value if attribute doesn't exist.")
    public void testDefaultValue() throws Exception {
        when(
                mBeanServerConnection.getAttribute(any(ObjectName.class),
                        anyString()))
                .thenThrow(new InstanceNotFoundException());
        JMXQuery jmxQuery = new JMXQuery("foo:bar=x!baz!!!-1", false);
        jmxQuery.execute(jmxQueryExecutor);
        assertEquals(jmxQuery.getCheckResult().getStatus(),
                NagiosServiceStatus.OK);
        assertEquals(jmxQuery.getCheckResult().getMessage(), "x:baz=-1");
    }

    @Test(description = "Default value not needed if attribute exist.")
    public void testDefaultValueNotNeeded() throws Exception {
        when(
                mBeanServerConnection.getAttribute(any(ObjectName.class),
                        anyString())).thenReturn(2);
        JMXQuery jmxQuery = new JMXQuery("foo:bar=x!baz!!!-1", false);
        jmxQuery.execute(jmxQueryExecutor);
        assertEquals(jmxQuery.getCheckResult().getStatus(),
                NagiosServiceStatus.OK);
        assertEquals(jmxQuery.getCheckResult().getMessage(), "x:baz=2");
    }
}
