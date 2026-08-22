package io.github.djordjije11.homeloancalculator.infra.web.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record RepaymentPeriodRequest(
        @Schema(description = "Principal at the start of this calculation, in EUR", example = "50000.00")
        @NotNull @Positive @Max(9999999) BigDecimal principalAmount,
        @Schema(description = "Positive nominal annual interest rate expressed as a percentage", example = "4.5")
        @NotNull @Positive BigDecimal annualInterestRatePercentage,
        @Schema(description = "Monthly payment in EUR", example = "897.91500")
        @NotNull @Positive @Max(9999999) BigDecimal monthlyPaymentAmount) {
}
