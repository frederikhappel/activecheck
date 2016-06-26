package org.activecheck.plugin.reporter.jmx.query;

import org.activecheck.common.Encoding;
import org.activecheck.common.nagios.NagiosCheck;
import org.activecheck.common.nagios.NagiosServiceStatus;
import org.apache.commons.lang.Validate;

import javax.management.openmbean.CompositeDataSupport;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JMXQuery extends NagiosCheck {
    private final JMXQuery parent;
    private final List<JMXQuery> children = new ArrayList<>();
    private final boolean isOperation;
    private final Object[] arguments;

    private String query = null;
    private JMXQueryOperator queryOperator = JMXQueryOperator.NONE;
    private String object = null;
    private String attributeName = null;
    private String attributeKey = null;
    private String defaultValue = null;

    public JMXQuery(String query, boolean isOperation) {
        this(query, isOperation, null, null);
        checkResult.setStatus(NagiosServiceStatus.UNKNOWN);
    }

    public JMXQuery(String query, boolean isOperation, Object[] arguments) {
        this(query, isOperation, arguments, null);
        checkResult.setStatus(NagiosServiceStatus.UNKNOWN);
    }

    public JMXQuery(String query, boolean isOperation, Object[] arguments, JMXQuery parent) {
        checkResult.setStatus(NagiosServiceStatus.UNKNOWN);
        validateQuery(query);
        query = query.trim();
        this.parent = parent;
        this.isOperation = isOperation;
        if (arguments != null) {
            this.arguments = arguments.clone();
        } else {
            this.arguments = null;
        }

        // query seems valid
        Pattern p = Pattern.compile("^\\(([^\\(\\)]*?)\\(.*\\)\\)$");
        Matcher operatorMatcher = p.matcher(query);

        if (operatorMatcher.find()) {
            queryOperator = JMXQueryOperator.fromString(operatorMatcher.group(1));
            this.query = null;
            checkResult.setConcatenator(" " + queryOperator + " ");

            // find complete statements
            query = query.replaceAll("^\\s*\\([^\\(\\)]*", "").replaceAll("\\)\\s*$", "");

            // run through string
            int countOpeningBrackets = 0;
            int countClosingBrackets = 0;
            int beginIndex = 0;
            int i = 0;
            for (byte character : query.getBytes(Encoding.UTF8)) {
                if (character == '(') {
                    countOpeningBrackets++;
                } else if (character == ')') {
                    countClosingBrackets++;
                }
                i++;
                if ((countOpeningBrackets > 0 || i == query.length()) && countOpeningBrackets == countClosingBrackets) {
                    String subquery = query.substring(beginIndex, i);
                    children.add(new JMXQuery(subquery, isOperation, arguments, this));
                    beginIndex = i;
                    countOpeningBrackets = 0;
                    countClosingBrackets = 0;
                }
            }
        } else {
            this.query = query.replaceAll("[()]", "");
        }

        parseQuery();
    }

    private void validateQuery(String query) {
        Validate.notNull(query, "query string cannot be NULL");
        // seems to be a ldap filter like expression
        int countOpeningBrackets = query.replaceAll("[^\\(]", "").length();
        int countClosingBrackets = query.replaceAll("[^\\)]", "").length();

        Validate.isTrue(countOpeningBrackets == countClosingBrackets,
                "invalid query: mismatched number of brackets : "
                        + countOpeningBrackets + "*'(' and "
                        + countClosingBrackets + "*')'");

        if (query.matches("\\(.*\\)")) {
            // test if surrounded with ()
            String tempQuery = query.replaceFirst("^\\(", "");
            int firstClosingBracket = tempQuery.indexOf(")");
            int countOpeningBracketsBeforeFirstClosingBracket = tempQuery
                    .substring(0, firstClosingBracket).replaceAll("[^\\(]", "")
                    .length();
            Validate.isTrue(
                    firstClosingBracket == tempQuery.length() - 1
                            || (firstClosingBracket < tempQuery.length() - 1 && countOpeningBracketsBeforeFirstClosingBracket > 0),
                    "invalid query: needs to be surrounded with brackets");
        }
    }

    private void parseQuery() {
        if (query != null) {
            // parse jmx query
            String[] parts = query.split("!");
            Validate.isTrue(parts.length >= 2, "object or attributeName cannot be NULL. Malformed query string");

            // set object
            Validate.notEmpty(parts[0], "object cannot be empty. Malformed query string");
            object = parts[0];

            // set attributeName and attributeKey
            Validate.notEmpty(parts[1], "attributeName cannot be empty. Malformed query string");
            String[] attributeParts = parts[1].split(":");
            attributeName = attributeParts[0];
            if (attributeParts.length > 1) {
                attributeKey = parts[1].split(":")[1];
            } else {
                attributeKey = null;
            }

            // set warning threshold
            if (parts.length >= 3) {
                setWarning(parts[2]);
            }

            // set critical threshold
            if (parts.length >= 4) {
                setCritical(parts[3]);
            }

            // set default value
            if (parts.length >= 5 && !parts[4].isEmpty()) {
                defaultValue = parts[4];
            } else {
                defaultValue = null;
            }
        }
    }

    public boolean execute(JMXQueryExecutor executor) throws IllegalArgumentException, JMXQueryExecutorException, IOException {
        Validate.notNull(executor, "jmx executor cannot be NULL");

        checkResult.clear(NagiosServiceStatus.OK);
        Optional<Boolean> result = Optional.empty();

        if (query != null) {
            Object checkData;
            Object value;
            try {
                if (isOperation) {
                    value = executor.invoke(object, attributeName, arguments);
                } else {
                    value = executor.getAttribute(object, attributeName);
                }
            } catch (JMXQueryExecutorException e) {
                if (defaultValue == null) {
                    throw e;
                }
                value = defaultValue;
            }

            // set report message
            String message = object.substring(object.indexOf("=") + 1) + ":" + attributeName;
            if (value instanceof CompositeDataSupport) {
                Validate.notNull(attributeKey, "attributeKey is missing");
                checkData = ((CompositeDataSupport) value).get(attributeKey);
                message += '.' + attributeKey + "=" + checkData;
            } else {
                // set status message and perfdata
                checkData = value;
                message += "=" + checkData;
                addPerformanceData(attributeName, checkData);
            }
            checkResult.addMessage(message);
            checkResult.setStatus(compare(checkData));
            result = Optional.of(checkResult.getStatus() == NagiosServiceStatus.OK);
        } else {
            for (JMXQuery child : children) {
                try {
                    result = Optional.of(queryOperator.calculate(result, child.execute(executor)));
                    checkResult.merge(child.getCheckResult());
                } catch (Exception e) {
                    result = Optional.of(queryOperator.calculate(result, false));
                    if (!result.get()) {
                        checkResult.setStatusMoreSevere(NagiosServiceStatus.CRITICAL);
                    }
                    checkResult.addMessage(e.getMessage());
                }
            }
            if (result.isPresent() && result.get()) {
                checkResult.setStatus(NagiosServiceStatus.OK);
            }
        }

        return result.isPresent() && result.get();
    }

    @Override
    public String getQuery() {
        return query;
    }

    public JMXQuery getParent() {
        return parent;
    }

    public JMXQueryOperator getQueryOperator() {
        return queryOperator;
    }

    public String getObject() {
        return object;
    }

    public List<JMXQuery> getChildren() {
        return children;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public Object getAttributeKey() {
        return attributeKey;
    }

    public String getDefaultValue() {
        return defaultValue;
    }
}
