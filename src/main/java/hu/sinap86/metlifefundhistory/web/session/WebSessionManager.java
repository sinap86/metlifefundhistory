package hu.sinap86.metlifefundhistory.web.session;

import hu.sinap86.metlifefundhistory.config.TransactionHistoryQuerySettings;
import hu.sinap86.metlifefundhistory.model.Contract;
import hu.sinap86.metlifefundhistory.web.TransactionDetailLinksExtractor;

import com.google.gson.JsonObject;
import lombok.Builder;
import lombok.Getter;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface WebSessionManager {

    @Builder
    @Getter
    class User {

        private String id;
        private String name;
        private String lastLogin;
    }

    @Builder
    @Getter
    class FundRate {

        private String fundName;
        private BigDecimal rate;
    }

    List<FundRate> getFundRates(final Contract contract, final LocalDate queryDate) throws IOException;

    boolean authenticateWithPassword(final String userName, final char[] password);

    boolean authenticateWithSmsOtp(final String smsOtp);

    boolean isAuthenticationWithPasswordSucceeded();

    boolean isAuthenticationWithSmsOtpSucceeded();

    boolean isAuthenticated();

    User getUser();

    List<Contract> getUserContracts();

    JsonObject queryTransactionHistory(final TransactionHistoryQuerySettings querySettings);

    JsonObject queryTransactionData(final TransactionDetailLinksExtractor.Link url);

    void logout();

}
