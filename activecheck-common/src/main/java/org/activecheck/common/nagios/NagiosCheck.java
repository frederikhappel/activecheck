package org.activecheck.common.nagios;

public abstract class NagiosCheck {
	private String warning = null;
	private String critical = null;
	private boolean warningLessThan = false;
	private boolean criticalLessThan = false;

	protected final NagiosCheckResult checkResult = new NagiosCheckResult();

	public final NagiosServiceStatus compare(Object checkData) {
		NagiosServiceStatus status = NagiosServiceStatus.OK;
		if (compare(checkData, critical, criticalLessThan)) {
			status = NagiosServiceStatus.CRITICAL;
		} else if (compare(checkData, warning, warningLessThan)) {
			status = NagiosServiceStatus.WARNING;
		}
		return status;
	}

	private final boolean compare(Object checkData, String compareValue,
			boolean invert) throws IllegalArgumentException {
		if (compareValue == null) {
			return false;
		}
		if (checkData == null) {
			throw new IllegalArgumentException("Query '" + getQuery()
					+ "' did not return a result");
		} else if (checkData instanceof Number) {
			Number check = (Number) checkData;
			if (check.doubleValue() == Math.floor(check.doubleValue())) {
				return invert ? check.doubleValue() < Double
						.parseDouble(compareValue)
						: check.doubleValue() > Double
								.parseDouble(compareValue);
			} else {
				return invert ? check.longValue() < Long
						.parseLong(compareValue) : check.longValue() > Long
						.parseLong(compareValue);
			}
		} else if (checkData instanceof String) {
			boolean result = invert;
			String[] compareValue_parts = compareValue.split("\\|");
			if (compareValue_parts.length == 0) {
				compareValue_parts[0] = compareValue;
			}
			for (String level_part : compareValue_parts) {
				boolean comparedResult = ((String) checkData)
						.equalsIgnoreCase(level_part);
				if (invert) {
					result = result && !comparedResult;
				} else {
					result = result || comparedResult;
				}
			}
			return result;
		} else if (checkData instanceof Boolean) {
			return checkData.equals(Boolean.parseBoolean(compareValue));
		}
		throw new IllegalArgumentException("'" + checkData
				+ "' returned from query '" + getQuery()
				+ "' is not of type Number,String or Boolean");
	}

	protected final void addPerformanceData(String name, Object value) {
		try {
			NagiosPerformanceData nagiosPerfData = new NagiosPerformanceData(
					name, value, warning, critical, null, null);
			nagiosPerfData.nextWarning(warningLessThan);
			nagiosPerfData.nextCritical(criticalLessThan);
			checkResult.addPerformanceData(nagiosPerfData);
		} catch (NagiosPerformanceDataException e) {
			// nothing to be done here
		}
	}

	public final String getWarning() {
		return warning;
	}

	public final void setWarning(String warning) {
		// set warning threshold
		if (warning == null || warning.isEmpty()) {
			this.warning = null;
			warningLessThan = false;
		} else {
			this.warning = warning.replaceAll("[<>^]", "");
			warningLessThan = (warning.startsWith("<") || warning
					.startsWith("^")) ? true : false;
		}
	}

	public final String getCritical() {
		return critical;
	}

	public final void setCritical(String critical) {
		// set critical threshold
		if (critical == null || critical.isEmpty()) {
			this.critical = null;
			criticalLessThan = false;
		} else {
			this.critical = critical.replaceAll("[<>^]", "");
			criticalLessThan = (critical.startsWith("<") || critical
					.startsWith("^")) ? true : false;
			warningLessThan = (warning == null) ? criticalLessThan
					: warningLessThan;
		}
	}

	public final NagiosCheckResult getCheckResult() {
		return checkResult;
	}

	public abstract String getQuery();
}
