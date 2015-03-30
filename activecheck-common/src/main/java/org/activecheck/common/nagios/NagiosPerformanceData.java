package org.activecheck.common.nagios;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.lang.Validate;

public class NagiosPerformanceData implements Serializable {
	private static final long serialVersionUID = 5564177737050845064L;
	private static final double DEFAULT_VALUE = Double.MAX_VALUE;
	public static final double PRECISION = 0.000001;

	private String name;
	private final Double current;
	private double warning = DEFAULT_VALUE;
	private double critical = DEFAULT_VALUE;
	private double minimum = DEFAULT_VALUE;
	private double maximum = DEFAULT_VALUE;
	private boolean isInteger = false;

	public NagiosPerformanceData(String line)
			throws NagiosPerformanceDataException, NumberFormatException {
		Validate.notNull(line);
		// parse string
		// <name>=<current>;<warning>;<critical>;<minimum>;<maximum>
		if (line.isEmpty()) {
			throw new NagiosPerformanceDataException(
					"received invalid performance data line: empty line");
		}
		String[] perfDataFields = line.split(";");
		if (perfDataFields.length < 1) {
			throw new NagiosPerformanceDataException(
					"received invalid performance data line: " + line);
		}
		String[] nameAndValue = perfDataFields[0].split("=");
		if (nameAndValue.length != 2) {
			throw new NagiosPerformanceDataException(
					"received invalid performance data line: " + line);
		}
		name = nameAndValue[0];
		current = parseValue(nameAndValue[1]);
		if (perfDataFields.length > 1) {
			warning = parseValue(perfDataFields[1]);
		}
		if (perfDataFields.length > 2) {
			critical = parseValue(perfDataFields[2]);
		}
		if (perfDataFields.length > 3) {
			minimum = parseValue(perfDataFields[3]);
		}
		if (perfDataFields.length > 4) {
			maximum = parseValue(perfDataFields[4]);
		}
	}

	public NagiosPerformanceData(String name, Object current, String warning,
			String critical, String minimum, String maximum)
			throws NumberFormatException {
		Validate.notNull(name);
		Validate.notNull(current);
		this.name = name;
		this.current = parseValue(current);
		this.critical = parseValue(critical);
		this.warning = (warning == null) ? this.critical : parseValue(warning);
		this.minimum = parseValue(minimum);
		this.maximum = parseValue(maximum);
		isInteger = current instanceof Integer;
	}

	public String getLine() {
		// generate valid performance data line
		String line = name + "=" + value2String(current);
		if (hasWarning()) {
			line += ";" + value2String(warning);
			if (hasCritical()) {
				line += ";" + value2String(critical);
				if (hasMinimum()) {
					line += ";" + value2String(minimum);
					if (hasMaximum()) {
						line += ";" + value2String(maximum);
					}
				}
			}
		}
		return line;
	}

	@Override
	public String toString() {
		return getLine();
	}

	private String value2String(Double value) {
		if (isInteger) {
			return String.format("%d", value.longValue());
		} else {
			return String.format("%f", value);
		}
	}

	private Double parseValue(Object value) throws NumberFormatException {
		if (value == null) {
			return DEFAULT_VALUE;
		} else if (value instanceof Number) {
			return ((Number) value).doubleValue();
		} else {
			return Double.parseDouble(value.toString().replaceAll("[^\\d,]*$",
					""));
		}
	}

	public String getName() {
		return name;
	}

	public Double getCurrent() {
		return current;
	}

	public Double getWarning() {
		return warning;
	}

	public boolean hasWarning() {
		return warning != DEFAULT_VALUE;
	}

	public void nextWarning(boolean less) {
		if (hasWarning()) {
			if (isInteger) {
				warning = less ? warning - 1 : warning + 1;
			} else {
				warning = less ? warning - PRECISION : warning + PRECISION;
			}
		}
	}

	public Double getCritical() {
		return critical;
	}

	public boolean hasCritical() {
		return critical != DEFAULT_VALUE;
	}

	public void nextCritical(boolean less) {
		if (hasCritical()) {
			if (isInteger) {
				critical = less ? critical - 1 : critical + 1;
			} else {
				critical = less ? critical - PRECISION : critical + PRECISION;
			}
		}
	}

	public Double getMinimum() {
		return minimum;
	}

	public boolean hasMinimum() {
		return minimum != DEFAULT_VALUE;
	}

	public Double getMaximum() {
		return maximum;
	}

	public boolean hasMaximum() {
		return maximum != DEFAULT_VALUE;
	}

	public void replace(Map<String, String> perfDataReplacements)
			throws NagiosPerformanceDataException {
		for (Map.Entry<String, String> entry : perfDataReplacements.entrySet()) {
			String newName = name.replaceAll(entry.getKey(), entry.getValue());
			if (newName.isEmpty()) {
				throw new NagiosPerformanceDataException(
						"running replace on performance data name '" + name
								+ "' failed. Name must not become empty");
			}
			name = newName;
		}
	}
}
