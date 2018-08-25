package hu.sinap86.metlifefundhistory.web;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import hu.sinap86.metlifefundhistory.config.TransactionHistoryQuerySettings;
import hu.sinap86.metlifefundhistory.config.Constants;
import hu.sinap86.metlifefundhistory.util.CommonUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

// TODO call MetLife through REST api
@Slf4j
public class MetLifeWebSessionManager {

    private boolean authenticationWithPasswordSucceeded;
    private boolean authenticationWithSmsOtpSucceeded;

    // TODO valami figyelés kellene a háttérben, hogy lejárt-e a session
    public boolean isAuthenticated() {
        return authenticationWithPasswordSucceeded && authenticationWithSmsOtpSucceeded;
    }

    public boolean isAuthenticationWithPasswordSucceeded() {
        return authenticationWithPasswordSucceeded;
    }

    public boolean isAuthenticationWithSmsOtpSucceeded() {
        return authenticationWithSmsOtpSucceeded;
    }

    public boolean authenticate(final String userName, final char[] password) {
        authenticationWithPasswordSucceeded = "1".equals(userName);
        log.debug("Authenticate user '{}' with userName and password {}", userName, (authenticationWithPasswordSucceeded ? "succeeded." : "NOT succeeded."));
        return authenticationWithPasswordSucceeded;
    }

    public boolean authenticate(final String smsOtp) {
        authenticationWithSmsOtpSucceeded = "1".equals(smsOtp);
        log.debug("Authenticate with sms OTP {}", (authenticationWithSmsOtpSucceeded ? "succeeded." : "NOT succeeded."));
        return authenticationWithSmsOtpSucceeded;
    }

    public Collection<String> getUserContracts() {
        return Lists.newArrayList("00177736");
    }

    public void logout() {
        log.debug("Logout called.");
    }

    public JsonObject queryTransactionHistory(final TransactionHistoryQuerySettings querySettings) {
        final File file = new File("./data/transactions_20110617-20180717.json");
        return getJsonObject(file);
    }

    public JsonObject queryTransactionData(final TransactionDetailLinksExtractor.Link url) {
        final String groupName = url.getGroup() == TransactionDetailLinksExtractor.TransactionGroup.RENEWALS_ANNIVERSARY ?
                "renewalsanniversaryprocess" : url.getGroup().getGroupName().toLowerCase();
        final File file = new File("./data/transactions/"
                + groupName + '/'
                + url.getTransactionNumber() + Constants.JSON_FILE_EXTENSION
        );
        return getJsonObject(file);
    }

    private JsonObject getJsonObject(final File file) {
        try {
            return CommonUtils.getAsJsonObject(file);
        } catch (IOException e) {
            log.error("Cannot read JSON content from: " + file.getAbsolutePath());
            return new JsonObject();
        }
    }
}
