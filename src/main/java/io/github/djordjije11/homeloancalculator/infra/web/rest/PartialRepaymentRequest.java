package io.github.djordjije11.homeloancalculator.infra.web.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record PartialRepaymentRequest(
        @Schema(description = "Current principal before the additional payment, in EUR", example = "41807.05090")
        @NotNull @Positive @Max(9999999) BigDecimal principalAmount,
        @Schema(description = "Positive nominal annual interest rate expressed as a percentage", example = "4.5")
        @NotNull @Positive BigDecimal annualInterestRate,
        @Schema(description = "Remaining repayment period in whole months", example = "48")
        @NotNull @Positive Integer repaymentPeriod,
        @Schema(description = "Immediate additional payment applied to principal, in EUR", example = "10000.00")
        @NotNull @Positive @Max(9999999) BigDecimal additionalPaymentAmount,
        @Schema(description = "Current monthly payment to preserve for the shorter-period alternative, in EUR", example = "897.91500")
        @NotNull @Positive @Max(9999999) BigDecimal monthlyPaymentAmount) {

    @AssertTrue(message = "additionalPaymentAmount must be smaller than principalAmount")
    public boolean isAdditionalPaymentSmallerThanPrincipal() {
        return principalAmount == null || additionalPaymentAmount == null
                || additionalPaymentAmount.compareTo(principalAmount) < 0;
    }
}
