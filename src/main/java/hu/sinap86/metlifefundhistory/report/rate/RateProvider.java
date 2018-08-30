package hu.sinap86.metlifefundhistory.report.rate;

import java.math.BigDecimal;

public interface RateProvider {

    String getRateDate();

    BigDecimal getExchangeRate(final String fundName);

}
