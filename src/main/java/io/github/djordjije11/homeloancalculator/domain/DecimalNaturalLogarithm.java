package io.github.djordjije11.homeloancalculator.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import org.springframework.util.Assert;

final class DecimalNaturalLogarithm {

    private DecimalNaturalLogarithm() {
    }

    static BigDecimal calculate(BigDecimal value, MathContext mathContext) {
        Assert.isTrue(value.signum() > 0, "value must be positive");

        BigDecimal transformedValue = value.subtract(BigDecimal.ONE, mathContext)
                .divide(value.add(BigDecimal.ONE, mathContext), mathContext);
        BigDecimal squaredTransformedValue = transformedValue.multiply(transformedValue, mathContext);
        BigDecimal term = transformedValue;
        BigDecimal result = BigDecimal.ZERO;
        BigDecimal tolerance = BigDecimal.ONE.movePointLeft(mathContext.getPrecision());

        for (int index = 1; ; index += 2) {
            BigDecimal contribution = term.divide(BigDecimal.valueOf(index), mathContext);
            result = result.add(contribution, mathContext);
            if (contribution.abs().compareTo(tolerance) <= 0) {
                return result.multiply(BigDecimal.TWO, mathContext);
            }
            term = term.multiply(squaredTransformedValue, mathContext);
        }
    }
}
