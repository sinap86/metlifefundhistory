package hu.sinap86.metlifefundhistory.web;

import hu.sinap86.metlifefundhistory.config.Constants;
import hu.sinap86.metlifefundhistory.config.TransactionHistoryQuerySettings;
import hu.sinap86.metlifefundhistory.model.Contract;
import hu.sinap86.metlifefundhistory.util.CommonUtils;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
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
    public static class FundRate {

        private String fundName;
        private BigDecimal rate;
    }

    private BaseHttpClient httpClient;

    private boolean cookiesAccepted;
    private boolean authenticationWithPasswordSucceeded;
    private boolean authenticationWithSmsOtpSucceeded;

    private BaseHttpClient getHttpClient() {
        if (httpClient == null) {
            httpClient = new BaseHttpClient(true);
        }
        return httpClient;
    }

    public List<FundRate> getRates(final Contract contract, final LocalDate date) throws IOException {
        CommonUtils.checkNotNull(date, "date");

        makeAcceptCookiesRequestIfNecessary();

        final String paddedContractTypeNumber = StringUtils.leftPad(contract.getType(), 4, '0');
        final String dateStr = date.format(Constants.DATE_FORMATTER);

        final HttpUriRequest rateRequest = RequestBuilder.get()
                .setUri(makeUri("http://www.metlifehungary.hu/portfoliok/portfoliovalues/index.jsp"))
                .addParameter("pageId", "page_1.jsp")
                .addParameter("showSelection", "false")
                .addParameter("showFunds", "false")
                .addParameter("compareMode", "compareContract")
                .addParameter("mainComponentCode", paddedContractTypeNumber)
                .addParameter("currency", contract.getCurrency())
                .addParameter("from", dateStr)
                .addParameter("yahoo_calendarInput1-yearselect", String.valueOf(date.getYear()))
                .addParameter("to", dateStr)
                .addParameter("yahoo_calendarInput2-yearselect", String.valueOf(date.getYear()))
                .addParameter("next", "Összehasonlítás")
                .build();
        final String responseHtml = getHttpClient().execute(rateRequest);

        return extractRatesFromHtml(responseHtml);
    }

    private void makeAcceptCookiesRequestIfNecessary() throws IOException {
        if (cookiesAccepted) {
            return;
        }
        getHttpClient().execute(new HttpGet("https://www.metlifehungary.hu/cookie/accept_e.html"));
        cookiesAccepted = true;
    }

    private URI makeUri(final String url) throws IOException {
        final URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
        return uri;
    }

    private List<FundRate> extractRatesFromHtml(final String responseHtml) {
        final Document doc = Jsoup.parse(responseHtml);

        final Elements rows = doc.select("div#tablazat > table > tbody > tr");
        if (CollectionUtils.isEmpty(rows)) {
            log.warn("Cannot extract rates from online response!");
            return Lists.newArrayList();
        }

        final List<FundRate> fundRates = new ArrayList<>();
        for (final Element row : rows) {
            final Elements cols = row.select("td");
            if (CollectionUtils.isEmpty(cols) || cols.size() < 2) {
                continue;
            }
            final String fundName = cols.get(0).text();
            try {
                final BigDecimal rate = CommonUtils.parse(cols.get(1).text(), Constants.NUMBER_FORMAT_HU);
                log.debug("Extracted rate of '{}' : {}", fundName, rate);
                fundRates.add(FundRate.builder()
                        .fundName(fundName)
                        .rate(rate)
                        .build());
            } catch (ParseException e) {
                log.error(String.format("Cannot parse '%s' rate value:", fundName), e);
            }
        }
        return fundRates;
    }

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
                .type("653")
                .typeName("befektetéshez kötött életbiztosítás")
                .currency("HUF")
                .actualValue(new BigDecimal("3608514.8118382804"))
                .surrenderValue(new BigDecimal("3241519.36"))
                .dueAmount(BigDecimal.ZERO)
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
