package hu.sinap86.metlifefundhistory.report.rate;

import hu.sinap86.metlifefundhistory.config.Constants;
import hu.sinap86.metlifefundhistory.util.CommonUtils;
import hu.sinap86.metlifefundhistory.web.MetLifeWebSessionManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
public class OnlineRateProvider implements RateProvider {

    private final List<MetLifeWebSessionManager.FundRate> fundRates;
    private final String rateDateString;

    private final MetLifeWebSessionManager webSessionManager;

    public OnlineRateProvider(final String contractTypeNumber, final String currency) throws IOException {
        CommonUtils.checkNotNull(contractTypeNumber, "contractTypeNumber");
        CommonUtils.checkNotNull(currency, "currency");

        final LocalDate now = LocalDate.now();
        rateDateString = now.format(Constants.DATE_FORMATTER);

        webSessionManager = new MetLifeWebSessionManager();
        fundRates = webSessionManager.getRates(contractTypeNumber, currency, now);
    }

    @Override
    public String getRateDate() {
        return rateDateString;
    }

    @Override
    public BigDecimal getExchangeRate(final String fundName) {
        /**
         * Check if all words in fundName is contained by fundRate's key.
         * One letter difference allowed.
         *
         * NOTE: Fund names in fundRates map are typically longer
         * than the ones in transaction history (fundName parameter)
         * so cannot look for exact match.
         */
        final String[] fundNameWords = fundName.split(" ");
        for (MetLifeWebSessionManager.FundRate fundRate : fundRates) {
            boolean containsAllWords = true;
            for (String fundNameWord : fundNameWords) {
                containsAllWords = containsAllWords
                        && CommonUtils.containsWordMaxOneLetterDifference(fundNameWord, fundRate.getFundName());
                if (!containsAllWords) {
                    break;
                }
            }
            if (containsAllWords) {
                return fundRate.getRate();
            }
        }
        return null;
    }

}
