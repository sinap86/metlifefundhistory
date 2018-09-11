package hu.sinap86.metlifefundhistory.model;

import hu.sinap86.metlifefundhistory.util.CommonUtils;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Data
@Builder
@EqualsAndHashCode(of = { "fundName" })
public class FundHistory {

    @NonNull
    private String fundName;
    private String fundCode;
    private final List<HistoryElement> historyElements = new ArrayList<>();

    public BigDecimal getTotalUnits() {
        return historyElements.stream().map(HistoryElement::getSumOfUnits).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalBalance() {
        Collections.sort(historyElements);

        final BigDecimal amountSum = historyElements.stream().map(HistoryElement::getSumAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return amountSum.negate();
    }

    public BigDecimal getTotalBalance(final BigDecimal rate) {
        CommonUtils.checkNotNull(rate, "rate");
        final BigDecimal amountSum = historyElements.stream().map(HistoryElement::getSumAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return getTotalUnits().multiply(rate).subtract(amountSum);
    }

    public long getPeriodLengthInMonths() {
        Collections.sort(historyElements);

        final String fromDate = historyElements.get(0).getTransactionDate();
        final String toDate = historyElements.get(historyElements.size() - 1).getTransactionDate();

        return ChronoUnit.MONTHS.between(
                LocalDate.parse(fromDate).withDayOfMonth(1),
                LocalDate.parse(toDate).withDayOfMonth(1));
    }
}
