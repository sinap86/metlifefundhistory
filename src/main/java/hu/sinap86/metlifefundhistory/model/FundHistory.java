package hu.sinap86.metlifefundhistory.model;

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

        // add the sumAmount of elements except the last one
        BigDecimal amountSum = BigDecimal.ZERO;
        for (int i = 0; i < historyElements.size() - 1; i++) {
            amountSum = amountSum.add(historyElements.get(i).getSumAmount());
        }
        // negate the last element's sumAmount and subtract the previous elements' sumAmount from it
        final HistoryElement lastElement = historyElements.get(historyElements.size() - 1);
        return lastElement.getSumAmount().negate().subtract(amountSum);
    }

    public BigDecimal getTotalBalance(final BigDecimal rate) {
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
