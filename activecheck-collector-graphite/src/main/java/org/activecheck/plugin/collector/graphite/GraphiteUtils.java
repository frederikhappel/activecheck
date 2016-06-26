package org.activecheck.plugin.collector.graphite;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GraphiteUtils {
	private static final Pattern IPV4_PATTERN = Pattern
			.compile("(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9][0-9]|[0-9])\\."
					+ "(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9][0-9]|[0-9])\\."
					+ "(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9][0-9]|[0-9])\\."
					+ "(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9][0-9]|[0-9])");

	public static String makeGraphitePath(String... names) {
		final StringBuilder graphitePath = new StringBuilder();
		for (String part : names) {
			if (part != null) {
				part = part.trim();
				if (!part.isEmpty()) {
					// prepare string
					part = replaceCharacters(part);

					// find intersection and append parts
					if (graphitePath.length() == 0) {
						graphitePath.append(part);
					} else {
						String currentPath = graphitePath.toString();
						StringBuilder lookfor = new StringBuilder();
						boolean intersecting = false;
						for (String cleanedPart : part.split("\\.")) {
							lookfor.append(cleanedPart);
							if (!intersecting) {
								if (currentPath.endsWith(lookfor.toString())) {
									intersecting = true;
								} else {
									lookfor.append(".");
								}
							} else {
								graphitePath.append('.');
								graphitePath.append(cleanedPart);
							}
						}
						if (!intersecting) {
							graphitePath.append('.');
							graphitePath.append(part);
						}
					}
				}
			}
		}
		String path = graphitePath.toString();

		// do a final clean and return
		return replaceCharacters(path);
	}

	private static String replaceUrlAndAddress(String part) {
		// url identifiers with escaped underscore
		part = part.replaceAll("://+", "\\\\_");

		// replace dots in IPv4 addresses with escaped underscores
		Matcher matcher = IPV4_PATTERN.matcher(part);
		while (matcher.find()) {
			String replacement = String.format(
					"%03d\\\\_%03d\\\\_%03d\\\\_%03d",
					Integer.parseInt(matcher.group(1)),
					Integer.parseInt(matcher.group(2)),
					Integer.parseInt(matcher.group(3)),
					Integer.parseInt(matcher.group(4)));
			part = matcher.replaceAll(replacement);
		}

		return part;
	}

	private static String replaceCharacters(String part) {
		// url and IP address handling
		part = replaceUrlAndAddress(part);

		// replace filesystem delimiters with dots
		part = part.replaceAll("/+", "."); // *nix
		part = part.replaceAll("\\\\+([^_])", ".$1"); // Windows

		// trim whitespaces before and after dots
		part = part.replaceAll("\\s+\\.+", ".");
		part = part.replaceAll("\\.+\\s+", ".");

		// merge and trim dots
		part = part.replaceAll("\\.+", ".");
		part = part.replaceAll("^\\.+", "");
		part = part.replaceAll("\\.+$", "");

		// clean quotes
		part = part.replaceAll("'+", "");
		part = part.replaceAll("\"+", "");

		// replace whitespaces and colons with escaped underscores
		part = part.replaceAll("\\s+", "\\\\_");
		part = part.replaceAll(":+", "\\\\_");

		// trim and merge underscores
		part = part.replaceAll("_+(\\\\*)_*", "$1_");
		part = part.replaceAll("\\.+_+", ".");

		return part;
	}

	public static String sanitizeServiceName(String serviceName) {
		// url and IP address handling
		serviceName = replaceUrlAndAddress(serviceName);

		// // replace all dots with escaped underscores
		// serviceName = serviceName.replaceAll("\\.+", "\\\\_");

		// replace unescaped underscores with dots
		serviceName = serviceName.replaceAll("([^\\\\])_+", "$1.");

		// merge and trim dots
		serviceName = serviceName.replaceAll("\\.+", ".");

		return serviceName;
	}

	public static String finalizeGraphitePath(String graphitePath) {
		return graphitePath.replaceAll("\\\\+", "");
	}
}
