package org.activecheck.plugin.reporter.jmx.query;

import java.util.Optional;

public enum JMXQueryOperator {
    NONE(""), OR("|"), AND("&"), IMPLIES("=>");

    private String operatorString;

    private JMXQueryOperator(String operatorString) {
        this.operatorString = operatorString;
    }

    @Override
    public String toString() {
        return operatorString;
    }

    public boolean calculate(Optional<Boolean> previousResult, boolean bool) {
        boolean result;
        if (!previousResult.isPresent()) {
            result = bool;
        } else {
            result = previousResult.get();
            switch (this) {
                case OR:
                    result = (result || bool);
                    break;
                case AND:
                    result = (result && bool);
                    break;
                case IMPLIES:
                    result = (!result || bool);
                    break;
                default:
                    result = bool;
                    break;
            }
        }

        return result;
    }

    public static JMXQueryOperator fromString(String operatorString) {
        for (JMXQueryOperator operator : JMXQueryOperator.values()) {
            if (operator.operatorString.contentEquals(operatorString)) {
                return operator;
            }
        }
        throw new IllegalArgumentException("Invalid operator '"
                + operatorString + "'");
    }
}
