package org.activecheck.plugin.collector.graphite;

import static org.testng.Assert.assertEquals;

import java.io.IOException;

import org.activecheck.plugin.collector.graphite.GraphiteUtils;
import org.testng.annotations.Test;

public class GraphiteUtilsTest {
	@Test(description = "Test whitespaces and trimming")
	public void testWhitespacesAndTrimming() throws IOException {
		String serviceName = "sendmail_connect tcp://localhost:25";
		String perfDataName = "";
		String expect = "sendmail.connect\\_tcp\\_localhost\\_25";

		String serviceNameCleaned = GraphiteUtils
				.sanitizeServiceName(serviceName);
		String graphitePath = GraphiteUtils.makeGraphitePath(
				serviceNameCleaned, perfDataName);
		assertEquals(graphitePath, expect);

		// test idempotency
		graphitePath = GraphiteUtils.makeGraphitePath(graphitePath);
		assertEquals(graphitePath, expect);
	}

	@Test(description = "Test IPv4 address recognition")
	public void testIpv4AddressRecognition() throws IOException {
		String serviceName = "sendmail_connect tcp://127.0.0.1:25";
		String perfDataName = "";
		String expect = "sendmail.connect\\_tcp\\_127\\_000\\_000\\_001\\_25";

		String serviceNameCleaned = GraphiteUtils
				.sanitizeServiceName(serviceName);
		String graphitePath = GraphiteUtils.makeGraphitePath(
				serviceNameCleaned, perfDataName);
		assertEquals(graphitePath, expect);

		// test idempotency
		graphitePath = GraphiteUtils.makeGraphitePath(graphitePath);
		assertEquals(graphitePath, expect);
	}

	@Test(description = "Test multiple IPv4 address recognition")
	public void testMultipleIpv4AddressRecognition() throws IOException {
		String serviceName = "127.0.0.1:25";
		String perfDataName = "127.0.0.1:26";
		String expect = "127\\_000\\_000\\_001\\_25.127\\_000\\_000\\_001\\_26";

		String serviceNameCleaned = GraphiteUtils
				.sanitizeServiceName(serviceName);
		String graphitePath = GraphiteUtils.makeGraphitePath(
				serviceNameCleaned, perfDataName);
		assertEquals(graphitePath, expect);

		// test idempotency
		graphitePath = GraphiteUtils.makeGraphitePath(graphitePath);
		assertEquals(graphitePath, expect);
	}

	@Test(description = "Test check_disk /")
	public void testCheckDiskRoot() throws IOException {
		String serviceName = "disk_free /";
		String perfDataName = "/";
		String expect = "disk.free";

		String serviceNameCleaned = GraphiteUtils
				.sanitizeServiceName(serviceName);
		String graphitePath = GraphiteUtils.makeGraphitePath(
				serviceNameCleaned, perfDataName);
		assertEquals(graphitePath, expect);

		// test idempotency
		graphitePath = GraphiteUtils.makeGraphitePath(graphitePath);
		assertEquals(graphitePath, expect);
	}

	@Test(description = "Test check_disk /var")
	public void testCheckDiskVar() throws IOException {
		String serviceName = "disk_free /var";
		String perfDataName = "/var";
		String expect = "disk.free.var";

		String serviceNameCleaned = GraphiteUtils
				.sanitizeServiceName(serviceName);
		String graphitePath = GraphiteUtils.makeGraphitePath(
				serviceNameCleaned, perfDataName);
		assertEquals(graphitePath, expect);

		// test idempotency
		graphitePath = GraphiteUtils.makeGraphitePath(graphitePath);
		assertEquals(graphitePath, expect);
	}

	@Test(description = "Test reccuring pattern")
	public void testReccuringPattern() throws IOException {
		String serviceName = "disk_free /var/tmp";
		String perfDataName = "/var";
		String expect = "disk.free.var.tmp.var";

		String serviceNameCleaned = GraphiteUtils
				.sanitizeServiceName(serviceName);
		String graphitePath = GraphiteUtils.makeGraphitePath(
				serviceNameCleaned, perfDataName);
		assertEquals(graphitePath, expect);

		// test idempotency
		graphitePath = GraphiteUtils.makeGraphitePath(graphitePath);
		assertEquals(graphitePath, expect);
	}

	@Test(description = "Test multiple reccuring patterns")
	public void testMultipleReccuringPatterns() throws IOException {
		String serviceName = "network_traffic_bond0.10";
		String perfDataName = "bond0.10.in_bps";
		String expect = "network.traffic.bond0.10.in_bps";

		String serviceNameCleaned = GraphiteUtils
				.sanitizeServiceName(serviceName);
		assertEquals(serviceNameCleaned, "network.traffic.bond0.10");
		String graphitePath = GraphiteUtils.makeGraphitePath(
				serviceNameCleaned, perfDataName);
		assertEquals(graphitePath, expect);

		// test idempotency
		graphitePath = GraphiteUtils.makeGraphitePath(graphitePath);
		assertEquals(graphitePath, expect);
	}

	@Test(description = "Test network_traffic_eth0")
	public void testNetworkTraffic() throws IOException {
		String graphitePath = "eth0.in.bps";
		String expect = "eth0.in.bps";

		graphitePath = GraphiteUtils.makeGraphitePath(graphitePath);
		assertEquals(graphitePath, expect);

		// test idempotency
		graphitePath = GraphiteUtils.makeGraphitePath(graphitePath);
		assertEquals(graphitePath, expect);
	}

	@Test(description = "Test single quoted perfData name")
	public void testSingleQuotedPerfDataName() throws IOException {
		String graphitePath = "'eth0.in.bps'";
		String expect = "eth0.in.bps";

		graphitePath = GraphiteUtils.makeGraphitePath(graphitePath);
		assertEquals(graphitePath, expect);

		// test idempotency
		graphitePath = GraphiteUtils.makeGraphitePath(graphitePath);
		assertEquals(graphitePath, expect);
	}

	@Test(description = "Test double quoted perfData name")
	public void testDoubleQuotedPerfDataName() throws IOException {
		String graphitePath = "\"eth0.in.bps\"";
		String expect = "eth0.in.bps";

		graphitePath = GraphiteUtils.makeGraphitePath(graphitePath);
		assertEquals(graphitePath, expect);

		// test idempotency
		graphitePath = GraphiteUtils.makeGraphitePath(graphitePath);
		assertEquals(graphitePath, expect);
	}

	@Test(description = "Test escaped underscore perfData name")
	public void testEscapedUnderscorePerfDataName() throws IOException {
		String serviceName = "mongod_replset\\_name_stats";
		String perfDataName = "database\\_name.storageSize.current";
		String expect = "mongod.replset\\_name.stats.database\\_name.storageSize.current";

		String serviceNameCleaned = GraphiteUtils
				.sanitizeServiceName(serviceName);
		String graphitePath = GraphiteUtils.makeGraphitePath(
				serviceNameCleaned, perfDataName);
		assertEquals(graphitePath, expect);

		// test idempotency
		graphitePath = GraphiteUtils.makeGraphitePath(graphitePath);
		assertEquals(graphitePath, expect);
	}

	@Test(description = "Test colon replacement")
	public void testColonReplacement() throws IOException {
		String graphitePath = "mongod:27017";
		String expect = "mongod\\_27017";

		graphitePath = GraphiteUtils.makeGraphitePath(graphitePath);
		assertEquals(graphitePath, expect);

		// test idempotency
		graphitePath = GraphiteUtils.makeGraphitePath(graphitePath);
		assertEquals(graphitePath, expect);
	}

	@Test(description = "Test multiple underscore replacement")
	public void testMultipleUnderscoreReplacement() throws IOException {
		String graphitePath = "this...__has\\___bad_\\_underscores";
		String expect = "this.has\\_bad\\_underscores";

		graphitePath = GraphiteUtils.makeGraphitePath(graphitePath);
		assertEquals(graphitePath, expect);

		// test idempotency
		graphitePath = GraphiteUtils.makeGraphitePath(graphitePath);
		assertEquals(graphitePath, expect);
	}

	@Test(description = "Test sanitizing serviceName with single dots")
	public void testSanitizingServiceNameWithSingleDots() throws IOException {
		String serviceName = "a.service.with.dots";

		String serviceNameCleaned = GraphiteUtils
				.sanitizeServiceName(serviceName);
		assertEquals(serviceNameCleaned, serviceName);

		// test idempotency
		serviceNameCleaned = GraphiteUtils
				.sanitizeServiceName(serviceNameCleaned);
		assertEquals(serviceNameCleaned, serviceName);
	}

	@Test(description = "Test sanitizing serviceName with multiple dots")
	public void testSanitizingServiceNameWithMultipleDots() throws IOException {
		String serviceName = "a..service...with....dots";
		String expect = "a.service.with.dots";

		String serviceNameCleaned = GraphiteUtils
				.sanitizeServiceName(serviceName);
		assertEquals(serviceNameCleaned, expect);

		// test idempotency
		serviceNameCleaned = GraphiteUtils
				.sanitizeServiceName(serviceNameCleaned);
		assertEquals(serviceNameCleaned, expect);
	}

	@Test(description = "Test finalization")
	public void testFinalization() throws IOException {
		String graphitePath = "mongod.replset\\_name.stats.database\\_name.storageSize.current";
		String expect = "mongod.replset_name.stats.database_name.storageSize.current";

		graphitePath = GraphiteUtils.finalizeGraphitePath(graphitePath);
		assertEquals(graphitePath, expect);
	}
}
