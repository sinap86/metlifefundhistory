package hu.sinap86.metlifefundhistory.report.rate;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Properties;

@Slf4j
public class FileRateProvider implements RateProvider {

    private final Properties rateProperties = new Properties();

    public FileRateProvider(final File ratesFile) {
        if (ratesFile == null) {
            throw new NullPointerException();
        }
        if (!ratesFile.canRead()) {
            throw new IllegalArgumentException(String.format("Exchange rates file (%s) not readable!", ratesFile.getAbsolutePath()));
        }

        try {
            rateProperties.loadFromXML(new FileInputStream(ratesFile));
            log.debug("Using exchange rates for active funds from file: {}", ratesFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Cannot load exchange rates from file: " + ratesFile.getAbsolutePath(), e);
        }
    }

    @Override
    public String getRateDate() {
        return rateProperties.getProperty("RATES_DATE");
    }

    @Override
    public BigDecimal getExchangeRate(final String fundName) {
        final String fundRate = rateProperties.getProperty(fundName);
        if (StringUtils.isNotEmpty(fundRate)) {
            return new BigDecimal(fundRate);
        }
        return null;
    }
}
