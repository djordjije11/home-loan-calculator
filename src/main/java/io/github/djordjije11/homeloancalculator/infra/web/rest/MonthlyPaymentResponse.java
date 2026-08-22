package io.github.djordjije11.homeloancalculator.infra.web.rest;

import java.math.BigDecimal;

public record MonthlyPaymentResponse(BigDecimal monthlyPaymentAmount) {
}
