package io.github.djordjije11.homeloancalculator.domain;

import java.math.BigDecimal;

public record PartialRepaymentCalculation(
        BigDecimal principalAmount, BigDecimal monthlyPaymentAmount, int repaymentPeriod) {
}
