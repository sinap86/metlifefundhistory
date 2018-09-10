package hu.sinap86.metlifefundhistory.web.session;

import hu.sinap86.metlifefundhistory.config.Constants;
import hu.sinap86.metlifefundhistory.config.TransactionHistoryQuerySettings;
import hu.sinap86.metlifefundhistory.model.Contract;
import hu.sinap86.metlifefundhistory.util.CommonUtils;
import hu.sinap86.metlifefundhistory.web.TransactionDetailLinksExtractor;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MockedWebSessionManager implements WebSessionManager {

    private boolean authenticationWithPasswordSucceeded;
    private boolean authenticationWithSmsOtpSucceeded;

    private final List<FundRate> fundRates = Lists.newArrayList(
            createRate("Salsa latin-amerikai (HUF)", "1.07095"),
            createRate("Magyar kötvény", "3.97674"),
            createRate("Euromix", "2.58937"),
            createRate("Globális részvény", "1.64752"),
            createRate("Egyensúly globális kötvény", "1.04303")
    );

    @Override
    public List<FundRate> getFundRates(final Contract contract, final LocalDate queryDate) throws IOException {
        delay(3000);
        log.info("{} exchange rate(s) for {} extracted from online response.", fundRates.size(), queryDate);
        return fundRates;
    }

    @Override
    public boolean authenticateWithPassword(final String userName, final String password) {
        authenticationWithPasswordSucceeded = "1".equals(userName);
        log.debug("Authenticate user '{}' with userName and password {}", userName, (authenticationWithPasswordSucceeded ? "succeeded." : "NOT succeeded."));
        return authenticationWithPasswordSucceeded;
    }

    @Override
    public boolean authenticateWithSmsOtp(final String smsOtp) {
        authenticationWithSmsOtpSucceeded = "1".equals(smsOtp);
        log.debug("Authenticate with sms OTP {}", (authenticationWithSmsOtpSucceeded ? "succeeded." : "NOT succeeded."));
        return authenticationWithSmsOtpSucceeded;
    }

    @Override
    public boolean isAuthenticationWithPasswordSucceeded() {
        return authenticationWithPasswordSucceeded;
    }

    @Override
    public boolean isAuthenticationWithSmsOtpSucceeded() {
        return authenticationWithSmsOtpSucceeded;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticationWithPasswordSucceeded && authenticationWithSmsOtpSucceeded;
    }

    @Override
    public User getUser() {
        return User.builder()
                .id("123456")
                .name("Kovács Béla")
                .lastLogin("2018.07.18 10:35:56")
                .build();
    }

    @Override
    public Set<Contract> getUserContracts() {
        final Contract contract = Contract.builder()
                .id("123456")
                .name("Presztízs")
                .type("653")
                .typeName("befektetéshez kötött életbiztosítás")
                .currency("HUF")
                .actualValue(new BigDecimal("2408514.8118382804"))
                .surrenderValue(new BigDecimal("2241519.36"))
                .dueAmount(BigDecimal.ZERO)
                .paidToDate("2018-12-28")
                .build();
        return Sets.newHashSet(contract);
    }

    @Override
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

    @Override
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

    @Override
    public void logout() {
        log.debug("Logout called.");
    }

    private FundRate createRate(final String name, final String rate) {
        return FundRate.builder().fundName(name).rate(new BigDecimal(rate)).build();
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
