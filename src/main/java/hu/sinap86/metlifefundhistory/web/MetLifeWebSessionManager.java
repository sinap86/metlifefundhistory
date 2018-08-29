package hu.sinap86.metlifefundhistory.web;

import hu.sinap86.metlifefundhistory.config.Constants;
import hu.sinap86.metlifefundhistory.config.TransactionHistoryQuerySettings;
import hu.sinap86.metlifefundhistory.util.CommonUtils;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

// TODO call MetLife through REST api
@Slf4j
public class MetLifeWebSessionManager {

    @Builder
    @Getter
    public static class User {

        private String id;
        private String name;
        private String lastLogin;
    }

    @Builder
    @Getter
    public static class Contract {

        private String id;
        private String name;
        private String contractTypeNumber;
        private String contractTypeName;
        private String currency;
        private double actualValue;
        private double surrenderValue;
        private double dueAmount;
        private String paidToDate;
    }

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

    public User getUser() {
        return User.builder()
                .id("123456")
                .name("Kovács Béla")
                .lastLogin("2018.07.18 10:35:56")
                .build();
    }

    public List<Contract> getUserContracts() {
        final Contract contract = Contract.builder()
                .id("123456")
                .name("Presztízs")
                .contractTypeNumber("653")
                .contractTypeName("befektetéshez kötött életbiztosítás")
                .currency("HUF")
                .actualValue(3608514.8118382804)
                .surrenderValue(3241519.36)
                .dueAmount(0)
                .paidToDate("2018-12-28")
                .build();
        return Lists.newArrayList(contract
//                , contract, contract, contract, contract
        );
    }

    public void logout() {
        log.debug("Logout called.");
    }

    public JsonObject queryTransactionHistory(final TransactionHistoryQuerySettings querySettings) {
        delay(100);

        try {
            //delay for task simulation
            TimeUnit.MILLISECONDS.sleep(2000);
        } catch (InterruptedException e) {
            System.err.println(e);
        }

        final File file = new File("./data/transactions_20110617-20180717.json");
        return getJsonObjectSafe(file);
    }

    public JsonObject queryTransactionData(final TransactionDetailLinksExtractor.Link url) {
        delay(10);

        final String groupName = url.getGroup() == TransactionDetailLinksExtractor.TransactionGroup.RENEWALS_ANNIVERSARY ?
                                 "renewalsanniversaryprocess" : url.getGroup().getGroupName().toLowerCase();
        final File file = new File("./data/transactions/"
                                   + groupName + '/'
                                   + url.getTransactionNumber() + Constants.JSON_FILE_EXTENSION
        );
        return getJsonObjectSafe(file);
    }

    private JsonObject getJsonObjectSafe(final File file) {
        try {
            return CommonUtils.getAsJsonObject(file);
        } catch (IOException e) {
            log.error("Cannot read JSON content from: " + file.getAbsolutePath());
            return new JsonObject();
        }
    }

    // delay for task simulation
    private void delay(final int timeout) {
        try {
            TimeUnit.MILLISECONDS.sleep(timeout);
        } catch (InterruptedException e) {
            log.error("", e);
        }
    }

}
