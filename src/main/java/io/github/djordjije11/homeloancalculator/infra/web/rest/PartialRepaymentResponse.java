package io.github.djordjije11.homeloancalculator.infra.web.rest;

import java.math.BigDecimal;

public record PartialRepaymentResponse(
        BigDecimal principalAmount,
        ReduceMonthlyPaymentResponse reduceMonthlyPayment,
        ReduceRepaymentPeriodResponse reduceRepaymentPeriod) {
}
