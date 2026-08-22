package io.github.djordjije11.homeloancalculator.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class HomeLoanCalculatorTest {

    private final HomeLoanCalculator homeLoanCalculator = new HomeLoanCalculator();

    @Test
    void calculateMonthlyPayment() {
        BigDecimal monthlyPaymentAmount = homeLoanCalculator.calculateMonthlyPayment(
                new BigDecimal("100000"), new BigDecimal("1.5"), 120);

        assertThat(monthlyPaymentAmount).isCloseTo(new BigDecimal("897.914998"), org.assertj.core.data.Offset.offset(new BigDecimal("0.000001")));
    }

    @Test
    void calculateRemainingPrincipal() {
        BigDecimal principalAmount = homeLoanCalculator.calculateRemainingPrincipal(
                new BigDecimal("100000"), new BigDecimal("1.5"), 120, 72);

        assertThat(principalAmount).isCloseTo(new BigDecimal("41807.0509"), org.assertj.core.data.Offset.offset(new BigDecimal("0.0001")));
    }

    @Test
    void calculatePaidInterest() {
        BigDecimal interestAmount = homeLoanCalculator.calculatePaidInterest(
                new BigDecimal("100000"), new BigDecimal("1.5"), 120, 72);

        assertThat(interestAmount).isCloseTo(new BigDecimal("6456.93"), org.assertj.core.data.Offset.offset(new BigDecimal("0.01")));
    }

    @Test
    void calculateRepaymentPeriod_shouldRoundUp() {
        int repaymentPeriod = homeLoanCalculator.calculateRepaymentPeriod(
                new BigDecimal("50000"), new BigDecimal("1.5"), new BigDecimal("897.915"));

        assertThat(repaymentPeriod).isEqualTo(58);
    }

    @Test
    void calculatePartialRepayment() {
        BigDecimal monthlyPaymentAmount = homeLoanCalculator.calculateMonthlyPayment(
                new BigDecimal("70000"), new BigDecimal("1.5"), 84);

        PartialRepaymentCalculation calculation = homeLoanCalculator.calculatePartialRepayment(
                new BigDecimal("70000"), new BigDecimal("1.5"), 84, new BigDecimal("20000"), monthlyPaymentAmount);

        assertThat(calculation.principalAmount()).isEqualByComparingTo("50000");
        assertThat(calculation.monthlyPaymentAmount()).isLessThan(monthlyPaymentAmount);
        assertThat(calculation.repaymentPeriod()).isLessThan(84);
    }

    @Test
    void calculateRepaymentPeriod_whenMonthlyPaymentDoesNotAmortisePrincipal_shouldThrowException() {
        assertThatIllegalArgumentException().isThrownBy(() -> homeLoanCalculator.calculateRepaymentPeriod(
                new BigDecimal("100000"), new BigDecimal("1.5"), new BigDecimal("125")));
    }
}
