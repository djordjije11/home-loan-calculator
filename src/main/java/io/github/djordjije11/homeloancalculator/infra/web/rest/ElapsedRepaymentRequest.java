package io.github.djordjije11.homeloancalculator.infra.web.rest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ElapsedRepaymentRequest(@Schema(description = "Principal at the start of this calculation, in EUR",
        example = "100000.00") @NotNull @Positive @Max(9999999) BigDecimal principalAmount,
                                      @Schema(description = "Positive nominal annual interest rate expressed as a percentage",
                                              example = "4.5") @NotNull @Positive BigDecimal annualInterestRatePercentage,
                                      @Schema(description = "Repayment period in whole months", example = "120") @NotNull @Positive Integer repaymentPeriod,
                                      @Schema(description = "Completed repayment period in whole months",
                                              example = "24") @NotNull @PositiveOrZero Integer elapsedRepaymentPeriod) {

    @JsonIgnore
    @AssertTrue(message = "elapsedRepaymentPeriod must not exceed repaymentPeriod")
    public boolean isElapsedRepaymentPeriodWithinRepaymentPeriod() {
        return repaymentPeriod == null || elapsedRepaymentPeriod == null || elapsedRepaymentPeriod <= repaymentPeriod;
    }
}
