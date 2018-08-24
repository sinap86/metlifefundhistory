package hu.sinap86.metlifefundhistory.report.rate;

import java.math.BigDecimal;

public interface RateProvider {

    String getRateDate();

    BigDecimal getExchangeRateOrZero(final String fundName);

}
