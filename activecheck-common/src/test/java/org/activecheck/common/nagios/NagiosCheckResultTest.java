package org.activecheck.common.nagios;

import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;

import org.testng.annotations.Test;
import org.testng.reporters.Files;

public class NagiosCheckResultTest {
	@Test(description = "Test check_linux_procstat.pl output")
	public void testCheckLinuxProcstat() throws IOException {
		InputStream is = ClassLoader
				.getSystemResourceAsStream("check_linux_procstat.pl.txt");
		String message = Files.readFile(is);

		NagiosCheckResult checkResult = NagiosCheckResult.fromMessage(message);
		assertEquals(checkResult.getPerfData().size(), 11);
	}

	@Test(description = "Test check_load output")
	public void testCheckLoad() throws IOException {
		InputStream is = ClassLoader
				.getSystemResourceAsStream("check_load.txt");
		String message = Files.readFile(is);

		NagiosCheckResult checkResult = NagiosCheckResult.fromMessage(message);
		Collection<NagiosPerformanceData> perfDataCollection = checkResult
				.getPerfData();
		assertEquals(perfDataCollection.size(), 3);
		Iterator<NagiosPerformanceData> it = perfDataCollection.iterator();

		// test perfdata for load1=0.200;4.000;8.000;0;
		NagiosPerformanceData perfData = it.next();
		assertEquals(perfData.getName(), "load1");
		assertEquals(perfData.getCurrent(), 0.2);
		assertEquals(perfData.getWarning(), 4.0);
		assertEquals(perfData.getCritical(), 8.0);
		assertEquals(perfData.getMinimum(), 0.0);

		// test perfdata for load5=0.170;4.000;8.000;0;
		perfData = it.next();
		assertEquals(perfData.getName(), "load5");
		assertEquals(perfData.getCurrent(), 0.17);
		assertEquals(perfData.getWarning(), 4.0);
		assertEquals(perfData.getCritical(), 8.0);
		assertEquals(perfData.getMinimum(), 0.0);

		// test perfdata for load15=0.270;4.000;8.000;0;
		perfData = it.next();
		assertEquals(perfData.getName(), "load15");
		assertEquals(perfData.getCurrent(), 0.27);
		assertEquals(perfData.getWarning(), 4.0);
		assertEquals(perfData.getCritical(), 8.0);
		assertEquals(perfData.getMinimum(), 0.0);
	}

	@Test(description = "Test check_cpu_stats.sh output")
	public void testCheckCpuStats() throws IOException {
		InputStream is = ClassLoader
				.getSystemResourceAsStream("check_cpu_stats.sh.txt");
		String message = Files.readFile(is);

		NagiosCheckResult checkResult = NagiosCheckResult.fromMessage(message);
		Collection<NagiosPerformanceData> perfDataCollection = checkResult
				.getPerfData();
		assertEquals(perfDataCollection.size(), 2);

		Iterator<NagiosPerformanceData> it = perfDataCollection.iterator();

		// test perfdata for CpuSTEAL=0.17;30;100
		NagiosPerformanceData perfData = it.next();
		assertEquals(perfData.getName(), "CpuSTEAL");
		assertEquals(perfData.getCurrent(), 0.17);
		assertEquals(perfData.getWarning(), 30.0);
		assertEquals(perfData.getCritical(), 100.0);

		// test perfdata for CpuIOWAIT=0.00;30;100
		perfData = it.next();
		assertEquals(perfData.getName(), "CpuIOWAIT");
		assertEquals(perfData.getCurrent(), 0.0);
		assertEquals(perfData.getWarning(), 30.0);
		assertEquals(perfData.getCritical(), 100.0);
	}

	@Test(description = "Test check_disk output")
	public void testCheckDisk() throws IOException {
		InputStream is = ClassLoader
				.getSystemResourceAsStream("check_disk.txt");
		String message = Files.readFile(is);

		NagiosCheckResult checkResult = NagiosCheckResult.fromMessage(message);
		Collection<NagiosPerformanceData> perfDataCollection = checkResult
				.getPerfData();
		assertEquals(perfDataCollection.size(), 1);

		Iterator<NagiosPerformanceData> it = perfDataCollection.iterator();

		// test perfdata for /=2016MB;9832;9822;0;9842
		NagiosPerformanceData perfData = it.next();
		assertEquals(perfData.getName(), "/");
		assertEquals(perfData.getCurrent(), 2016.0);
		assertEquals(perfData.getWarning(), 9832.0);
		assertEquals(perfData.getCritical(), 9822.0);
		assertEquals(perfData.getMinimum(), 0.0);
		assertEquals(perfData.getMaximum(), 9842.0);
	}

	@Test(description = "Test check_cassandra_cluster.sh output")
	public void testCheckCassandraCluster() throws IOException {
		InputStream is = ClassLoader
				.getSystemResourceAsStream("check_cassandra_cluster.sh.txt");
		String message = Files.readFile(is);

		NagiosCheckResult checkResult = NagiosCheckResult.fromMessage(message);
		Collection<NagiosPerformanceData> perfDataCollection = checkResult
				.getPerfData();
		assertEquals(perfDataCollection.size(), 18);

		Iterator<NagiosPerformanceData> it = perfDataCollection.iterator();

		// Load_172.17.45.155=20.78GB
		NagiosPerformanceData perfData = it.next();
		assertEquals(perfData.getName(), "Load_172.17.45.155");
		assertEquals(perfData.getCurrent(), 20.78);
		// Tokens_172.17.45.155=256
		perfData = it.next();
		assertEquals(perfData.getName(), "Tokens_172.17.45.155");
		assertEquals(perfData.getCurrent(), 256.0);
		// Owns_172.17.45.155=15.1%
		perfData = it.next();
		assertEquals(perfData.getName(), "Owns_172.17.45.155");
		assertEquals(perfData.getCurrent(), 15.1);

		// Load_172.17.45.154=18.06GB
		perfData = it.next();
		assertEquals(perfData.getName(), "Load_172.17.45.154");
		assertEquals(perfData.getCurrent(), 18.06);
		// Tokens_172.17.45.154=256
		perfData = it.next();
		assertEquals(perfData.getName(), "Tokens_172.17.45.154");
		assertEquals(perfData.getCurrent(), 256.0);
		// Owns_172.17.45.154=16.6%
		perfData = it.next();
		assertEquals(perfData.getName(), "Owns_172.17.45.154");
		assertEquals(perfData.getCurrent(), 16.6);
	}

	@Test(description = "Test Case1 output")
	public void testParsingSingleLine() throws IOException {
		InputStream is = ClassLoader
				.getSystemResourceAsStream("output_case1.txt");
		String message = Files.readFile(is);

		NagiosCheckResult checkResult = NagiosCheckResult.fromMessage(message);
		assertEquals(checkResult.getMessage(), "message_line1");
		assertEquals(checkResult.getPerfData().size(), 0);
	}

	@Test(description = "Test Case2 output")
	public void testParsingSingleLineWithPerfdata() throws IOException {
		InputStream is = ClassLoader
				.getSystemResourceAsStream("output_case2.txt");
		String message = Files.readFile(is);

		NagiosCheckResult checkResult = NagiosCheckResult.fromMessage(message);
		assertEquals(checkResult.getMessage(), "message_line1");
		assertEquals(checkResult.getPerfData().size(), 1);
	}

	@Test(description = "Test Case3 output")
	public void testParsingMultipleLinesWithPerfdata() throws IOException {
		InputStream is = ClassLoader
				.getSystemResourceAsStream("output_case3.txt");
		String message = Files.readFile(is);

		NagiosCheckResult checkResult = NagiosCheckResult.fromMessage(message);
		assertEquals(checkResult.getMessage(),
				"message_line1\nmessage_line2\nmessage_line3\nmessage_line4");
		assertEquals(checkResult.getPerfData().size(), 4);
	}

	@Test(description = "Test replacing")
	public void testReplacing() throws IOException {
		InputStream is = ClassLoader
				.getSystemResourceAsStream("output_case3.txt");
		String message = Files.readFile(is);

		NagiosCheckResult checkResult = new NagiosCheckResult();
		checkResult.addPerfDataReplacement("_", ".");
		checkResult.parseMessage(message);
		assertEquals(checkResult.getPerfData().get(0).getName(),
				"perfdata.line1");
		assertEquals(checkResult.getPerfData().get(1).getName(),
				"perfdata.line2");
		assertEquals(checkResult.getPerfData().get(2).getName(),
				"perfdata.line3");
		assertEquals(checkResult.getPerfData().get(3).getName(),
				"perfdata.line4");
	}
}
