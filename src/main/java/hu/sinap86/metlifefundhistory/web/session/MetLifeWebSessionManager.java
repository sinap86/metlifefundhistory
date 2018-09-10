package hu.sinap86.metlifefundhistory.web.session;

import hu.sinap86.metlifefundhistory.config.Constants;
import hu.sinap86.metlifefundhistory.config.TransactionHistoryQuerySettings;
import hu.sinap86.metlifefundhistory.model.Contract;
import hu.sinap86.metlifefundhistory.util.CommonUtils;
import hu.sinap86.metlifefundhistory.web.BaseHttpClient;
import hu.sinap86.metlifefundhistory.web.TransactionDetailLinksExtractor;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class MetLifeWebSessionManager implements WebSessionManager {

    private boolean authenticationWithPasswordSucceeded;
    private boolean authenticationWithSmsOtpSucceeded;
    private User user;
    private final Set<Contract> contracts = new HashSet<>();

    private BaseHttpClient httpClient;

    private BaseHttpClient getHttpClient() {
        if (httpClient == null) {
            httpClient = new BaseHttpClient(true);
            httpClient.addCookie("acceptcookie", "Yes", "www.metlifehungary.hu");
        }
        return httpClient;
    }

    @Override
    public List<FundRate> getFundRates(final Contract contract, final LocalDate queryDate) throws IOException {
        CommonUtils.checkNotNull(queryDate, "queryDate");

        final String paddedContractTypeNumber = StringUtils.leftPad(contract.getType(), 4, '0');
        final String queryDateStr = queryDate.format(Constants.DATE_FORMATTER_DOTTED);

        final HttpUriRequest rateRequest = RequestBuilder.get()
                .setUri(makeUri("http://www.metlifehungary.hu/portfoliok/portfoliovalues/index.jsp"))
                .addParameter("pageId", "page_1.jsp")
                .addParameter("showSelection", "false")
                .addParameter("showFunds", "false")
                .addParameter("compareMode", "compareContract")
                .addParameter("mainComponentCode", paddedContractTypeNumber)
                .addParameter("currency", contract.getCurrency())
                .addParameter("from", queryDateStr)
                .addParameter("yahoo_calendarInput1-yearselect", String.valueOf(queryDate.getYear()))
                .addParameter("to", queryDateStr)
                .addParameter("yahoo_calendarInput2-yearselect", String.valueOf(queryDate.getYear()))
                .addParameter("next", "Összehasonlítás")
                .build();
        final String responseHtml = getHttpClient().execute(rateRequest);

        final List<FundRate> fundRates = extractRatesFromHtml(responseHtml);
        log.info("{} exchange rate(s) for {} extracted from online response.", fundRates.size(), queryDateStr);
        return fundRates;
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

    @Override
    public boolean authenticateWithPassword(final String userName, final String password) {
        try {
            final HttpUriRequest loginRequest = RequestBuilder.post()
                    .setUri(makeUri("https://www.metlifehungary.hu/security/UI/Login?realm=efund"))
                    .addParameter("IDToken1", userName)
                    .addParameter("IDToken2", password)
                    .build();

            final String responseHtml = getHttpClient().execute(loginRequest);
            authenticationWithPasswordSucceeded = isSuccess(responseHtml);
        } catch (Exception e) {
            log.error("Cannot authenticate with user name and password:", e);
            authenticationWithPasswordSucceeded = false;
        }
        log.info("Authenticate user '{}' with userName and password {}", userName, (authenticationWithPasswordSucceeded ? "succeeded." : "NOT succeeded."));
        return authenticationWithPasswordSucceeded;
    }

    @Override
    public boolean authenticateWithSmsOtp(final String smsOtp) {
        if (!authenticationWithPasswordSucceeded) {
            throw new IllegalStateException("Call authenticateWithPassword() first!");
        }

        try {
            final HttpUriRequest smsAuthRequest = RequestBuilder.post()
                    .setUri(new URI("https://www.metlifehungary.hu/security/UI/Login?realm=efund"))
                    .addParameter("IDToken1", smsOtp)
                    .build();

            final String responseHtml = getHttpClient().execute(smsAuthRequest);
            authenticationWithSmsOtpSucceeded = isSuccess(responseHtml);
        } catch (Exception e) {
            log.error("Cannot authenticate with SMS OTP:", e);
            authenticationWithSmsOtpSucceeded = false;
        }
        log.info("Authenticate with sms OTP {}", (authenticationWithSmsOtpSucceeded ? "succeeded." : "NOT succeeded."));
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

    // TODO valami figyelés kellene a háttérben, hogy lejárt-e a session
    @Override
    public boolean isAuthenticated() {
        return authenticationWithPasswordSucceeded && authenticationWithSmsOtpSucceeded;
    }

    @Override
    public User getUser() {
        checkAuthenticated();

        if (user != null) {
            log.debug("User data returned from cache.");
            return user;
        }

        try {
            final JsonObject userReply = getHttpClient().executeGetRequestForJsonReply("https://www.metlifehungary.hu/eFund/api/security/user");
            final String userId = CommonUtils.getString(userReply, "name");
            if (StringUtils.isEmpty(userId)) {
                throw new IllegalStateException("Cannot extract user id from reply!");
            }

            final JsonObject headerReply = getHttpClient().executeGetRequestForJsonReply(String.format(
                    "https://www.metlifehungary.hu/eFund/api/owners/%s/header", userId
            ));
            final String name = CommonUtils.getString(headerReply, "ownersName");

            user = User.builder()
                    .id(userId)
                    .name(name)
                    .lastLogin(CommonUtils.getString(headerReply, "lastLogin"))
                    .build();
            log.info("User data of '{}' queried successfully.", name);
        } catch (Exception e) {
            log.error("Cannot query user data:", e);
            user = null;
        }
        return user;
    }

    @Override
    public Set<Contract> getUserContracts() {
        checkAuthenticated();

        if (CollectionUtils.isNotEmpty(contracts)) {
            log.debug("Contracts' data returned from cache.");
            return contracts;
        }

        try {
            final JsonObject contractsReply = getHttpClient().executeGetRequestForJsonReply("https://www.metlifehungary.hu/eFund/api/owners/%s/ownerscontracts", user.getId());
            final JsonArray contractElements = contractsReply.getAsJsonArray("contracts");
            for (JsonElement contractElement : contractElements) {
                final JsonObject contractData = contractElement.getAsJsonObject().getAsJsonObject("contractData");

                final Contract contract = Contract.builder()
                        .id(CommonUtils.getString(contractData, "contractId"))
                        .name(CommonUtils.getString(contractData, "fantasyName"))
                        .type(CommonUtils.getString(contractData, "contractTypeNumber"))
                        .typeName(CommonUtils.getString(contractData, "contractTypeName"))
                        .currency(CommonUtils.getString(contractData, "currency"))
                        .actualValue(CommonUtils.getBigDecimal(contractData, "actualValue"))
                        .surrenderValue(CommonUtils.getBigDecimal(contractData, "surrenderValue"))
                        .dueAmount(CommonUtils.getBigDecimal(contractData, "dueAmount"))
                        .paidToDate(CommonUtils.getString(contractData, "paidToDate"))
                        .build();
                contracts.add(contract);
            }
            log.info("{} contracts' data extracted from online response.", contracts.size());

        } catch (Exception e) {
            log.error("Cannot get user contacts' data:", e);
            contracts.clear();
        }
        return contracts;
    }

    @Override
    public JsonObject queryTransactionHistory(final TransactionHistoryQuerySettings querySettings) throws IOException {
        checkAuthenticated();

        final StringBuilder sbRequestUrl = new StringBuilder()
                .append("https://www.metlifehungary.hu/eFund/api/owners/")
                .append(user.getId())
                .append("/contracts/")
                .append(querySettings.getContract())
                .append("/sumlifetransactions?dateFrom=")
                .append(querySettings.getFromDate().format(Constants.DATE_FORMATTER_DASHED))
                .append("&dateTo=")
                .append(querySettings.getToDate().format(Constants.DATE_FORMATTER_DASHED))
                .append("&recordFrom=1&recordTo=")
                .append(Constants.QUERY_TRANSACTION_HISTORY_MAX_TRANSACTION_COUNT);

        final JsonObject reply = getHttpClient().executeGetRequestForJsonReply(sbRequestUrl.toString());
        log.info("Transaction history queried successfully for {}", querySettings);
        return reply;
    }

    @Override
    public JsonObject queryTransactionData(final TransactionDetailLinksExtractor.Link url) throws IOException {
        checkAuthenticated();

        final JsonObject reply = getHttpClient().executeGetRequestForJsonReply(url.getUrl());
        log.info("Transaction data queried successfully for {}", url.getUrl());
        return reply;
    }

    @Override
    public void logout() {
        // TODO
        throw new NotImplementedException("logout");
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

    private boolean isSuccess(final String responseHtml) {
        return !StringUtils.contains(responseHtml, "Hibás bejelentkezési kísérlet")
               && !StringUtils.contains(responseHtml, "Authentication Failed");
    }

    private void checkAuthenticated() {
        if (!isAuthenticated()) {
            throw new IllegalStateException("User not authenticated!");
        }
    }

}
