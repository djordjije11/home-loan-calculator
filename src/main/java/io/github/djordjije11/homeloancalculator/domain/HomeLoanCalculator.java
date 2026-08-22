package io.github.djordjije11.homeloancalculator.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public final class HomeLoanCalculator {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);
    private static final MathContext MATH_CONTEXT = new MathContext(34, RoundingMode.HALF_UP);

    public BigDecimal calculateMonthlyPayment(BigDecimal principalAmount, BigDecimal annualInterestRatePercentage, int repaymentPeriod) {
        validateCalculationInputs(principalAmount, annualInterestRatePercentage, repaymentPeriod);

        BigDecimal monthlyInterestRate = calculateMonthlyInterestRate(annualInterestRatePercentage);
        BigDecimal growthFactor = BigDecimal.ONE.add(monthlyInterestRate).pow(repaymentPeriod, MATH_CONTEXT);
        return principalAmount.multiply(monthlyInterestRate, MATH_CONTEXT)
                .multiply(growthFactor, MATH_CONTEXT)
                .divide(growthFactor.subtract(BigDecimal.ONE), MATH_CONTEXT);
    }

    public BigDecimal calculateRemainingPrincipal(
            BigDecimal principalAmount, BigDecimal annualInterestRatePercentage, int repaymentPeriod, int elapsedRepaymentPeriod) {
        validateCalculationInputs(principalAmount, annualInterestRatePercentage, repaymentPeriod);
        Assert.isTrue(elapsedRepaymentPeriod >= 0 && elapsedRepaymentPeriod <= repaymentPeriod,
                "elapsedRepaymentPeriod must be between zero and repaymentPeriod");

        BigDecimal monthlyInterestRate = calculateMonthlyInterestRate(annualInterestRatePercentage);
        BigDecimal growthFactor = BigDecimal.ONE.add(monthlyInterestRate).pow(elapsedRepaymentPeriod, MATH_CONTEXT);
        BigDecimal monthlyPaymentAmount = calculateMonthlyPayment(principalAmount, annualInterestRatePercentage, repaymentPeriod);
        BigDecimal paymentEffect = monthlyPaymentAmount.multiply(growthFactor.subtract(BigDecimal.ONE), MATH_CONTEXT)
                .divide(monthlyInterestRate, MATH_CONTEXT);
        return principalAmount.multiply(growthFactor, MATH_CONTEXT).subtract(paymentEffect, MATH_CONTEXT).max(BigDecimal.ZERO);
    }

    public BigDecimal calculatePaidInterest(
            BigDecimal principalAmount, BigDecimal annualInterestRatePercentage, int repaymentPeriod, int elapsedRepaymentPeriod) {
        BigDecimal monthlyPaymentAmount = calculateMonthlyPayment(principalAmount, annualInterestRatePercentage, repaymentPeriod);
        BigDecimal remainingPrincipalAmount = calculateRemainingPrincipal(
                principalAmount, annualInterestRatePercentage, repaymentPeriod, elapsedRepaymentPeriod);
        return monthlyPaymentAmount.multiply(BigDecimal.valueOf(elapsedRepaymentPeriod), MATH_CONTEXT)
                .subtract(principalAmount.subtract(remainingPrincipalAmount), MATH_CONTEXT)
                .max(BigDecimal.ZERO);
    }

    public int calculateRepaymentPeriod(BigDecimal principalAmount, BigDecimal annualInterestRatePercentage, BigDecimal monthlyPaymentAmount) {
        Assert.notNull(principalAmount, "principalAmount must not be null");
        Assert.notNull(annualInterestRatePercentage, "annualInterestRatePercentage must not be null");
        Assert.notNull(monthlyPaymentAmount, "monthlyPaymentAmount must not be null");
        Assert.isTrue(principalAmount.signum() > 0, "principalAmount must be positive");
        Assert.isTrue(annualInterestRatePercentage.signum() > 0, "annualInterestRatePercentage must be positive");
        Assert.isTrue(monthlyPaymentAmount.signum() > 0, "monthlyPaymentAmount must be positive");

        BigDecimal monthlyInterestRate = calculateMonthlyInterestRate(annualInterestRatePercentage);
        BigDecimal firstMonthInterest = principalAmount.multiply(monthlyInterestRate, MATH_CONTEXT);
        Assert.isTrue(monthlyPaymentAmount.compareTo(firstMonthInterest) > 0,
                "monthlyPaymentAmount must be greater than the first month's interest");

        BigDecimal logarithmArgument = BigDecimal.ONE.subtract(
                principalAmount.multiply(monthlyInterestRate, MATH_CONTEXT).divide(monthlyPaymentAmount, MATH_CONTEXT));
        BigDecimal repaymentPeriod = DecimalNaturalLogarithm.calculate(logarithmArgument, MATH_CONTEXT).negate()
                .divide(DecimalNaturalLogarithm.calculate(BigDecimal.ONE.add(monthlyInterestRate), MATH_CONTEXT), MATH_CONTEXT);
        return repaymentPeriod.setScale(0, RoundingMode.CEILING).intValueExact();
    }

    public PartialRepaymentCalculation calculatePartialRepayment(
            BigDecimal principalAmount,
            BigDecimal annualInterestRatePercentage,
            int repaymentPeriod,
            BigDecimal additionalPaymentAmount,
            BigDecimal monthlyPaymentAmount) {
        validateCalculationInputs(principalAmount, annualInterestRatePercentage, repaymentPeriod);
        Assert.notNull(additionalPaymentAmount, "additionalPaymentAmount must not be null");
        Assert.isTrue(additionalPaymentAmount.signum() > 0, "additionalPaymentAmount must be positive");
        Assert.isTrue(additionalPaymentAmount.compareTo(principalAmount) < 0,
                "additionalPaymentAmount must be smaller than principalAmount");

        BigDecimal newPrincipalAmount = principalAmount.subtract(additionalPaymentAmount);
        return new PartialRepaymentCalculation(
                newPrincipalAmount,
                calculateMonthlyPayment(newPrincipalAmount, annualInterestRatePercentage, repaymentPeriod),
                calculateRepaymentPeriod(newPrincipalAmount, annualInterestRatePercentage, monthlyPaymentAmount));
    }

    private BigDecimal calculateMonthlyInterestRate(BigDecimal annualInterestRatePercentage) {
        return annualInterestRatePercentage.divide(ONE_HUNDRED, MATH_CONTEXT).divide(TWELVE, MATH_CONTEXT);
    }

    private void validateCalculationInputs(BigDecimal principalAmount, BigDecimal annualInterestRatePercentage, int repaymentPeriod) {
        Assert.notNull(principalAmount, "principalAmount must not be null");
        Assert.notNull(annualInterestRatePercentage, "annualInterestRatePercentage must not be null");
        Assert.isTrue(principalAmount.signum() > 0, "principalAmount must be positive");
        Assert.isTrue(annualInterestRatePercentage.signum() > 0, "annualInterestRatePercentage must be positive");
        Assert.isTrue(repaymentPeriod > 0, "repaymentPeriod must be positive");
    }
}
