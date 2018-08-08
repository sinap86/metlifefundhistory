package hu.sinap86.metlifefundhistory.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

import java.math.BigDecimal;

@Data
@Builder
@EqualsAndHashCode(of = { "transactionName", "transactionDate", "rate", "priceDate" })
public class HistoryElement implements Comparable<HistoryElement> {

    @NonNull
    private String transactionName;
    private String transactionCode;
    @NonNull
    private String transactionDate;
    private BigDecimal numberOfInitialUnits;
    private BigDecimal numberOfAccumulationUnits;
    @NonNull
    private BigDecimal rate;
    @NonNull
    private String priceDate;

    private volatile BigDecimal sumOfUnits;

    public BigDecimal getSumOfUnits() {
        if (sumOfUnits == null) {
            sumOfUnits = calculateSumOfUnits();
        }
        return sumOfUnits;
    }

    private BigDecimal calculateSumOfUnits() {
        if (numberOfInitialUnits == null && numberOfAccumulationUnits == null) {
            return BigDecimal.ZERO;
        }
        if (numberOfInitialUnits == null) {
            return numberOfAccumulationUnits;
        }
        if (numberOfAccumulationUnits == null) {
            return numberOfInitialUnits;
        }
        return numberOfInitialUnits.add(numberOfAccumulationUnits);
    }

    public BigDecimal getSumAmount() {
        final BigDecimal sumOfUnits = getSumOfUnits();
        if (sumOfUnits == null) {
            return null;
        }
        return sumOfUnits.multiply(rate);
    }

    @Override
    public int compareTo(final HistoryElement other) {
        if (other == null) {
            return 1;
        }
        return this.transactionDate.compareTo(other.transactionDate);
    }
}
