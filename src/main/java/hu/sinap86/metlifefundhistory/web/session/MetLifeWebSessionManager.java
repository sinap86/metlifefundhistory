package hu.sinap86.metlifefundhistory.web.session;

import hu.sinap86.metlifefundhistory.config.Constants;
import hu.sinap86.metlifefundhistory.config.TransactionHistoryQuerySettings;
import hu.sinap86.metlifefundhistory.model.Contract;
import hu.sinap86.metlifefundhistory.util.CommonUtils;
import hu.sinap86.metlifefundhistory.web.BaseHttpClient;
import hu.sinap86.metlifefundhistory.web.TransactionDetailLinksExtractor;

import com.google.common.collect.Lists;
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
import java.util.List;

@Slf4j
public class MetLifeWebSessionManager implements WebSessionManager {

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
        final String queryDateStr = queryDate.format(Constants.DATE_FORMATTER);

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

        return extractRatesFromHtml(responseHtml);
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

    @Override
    public boolean authenticateWithPassword(final String userName, final char[] password) {
        throw new NotImplementedException("authenticateWithPassword");
    }

    @Override
    public boolean authenticateWithSmsOtp(final String smsOtp) {
        throw new NotImplementedException("authenticateWithSmsOtp");
    }

    @Override
    public boolean isAuthenticationWithPasswordSucceeded() {
        throw new NotImplementedException("isAuthenticationWithPasswordSucceeded");
    }

    @Override
    public boolean isAuthenticationWithSmsOtpSucceeded() {
        throw new NotImplementedException("isAuthenticationWithSmsOtpSucceeded");
    }

    // TODO valami figyelés kellene a háttérben, hogy lejárt-e a session
    @Override
    public boolean isAuthenticated() {
        throw new NotImplementedException("isAuthenticated");
    }

    @Override
    public User getUser() {
        throw new NotImplementedException("getUser");
    }

    @Override
    public List<Contract> getUserContracts() {
        throw new NotImplementedException("getUserContracts");
    }

    @Override
    public JsonObject queryTransactionHistory(final TransactionHistoryQuerySettings querySettings) {
        throw new NotImplementedException("queryTransactionHistory");
    }

    @Override
    public JsonObject queryTransactionData(final TransactionDetailLinksExtractor.Link url) {
        throw new NotImplementedException("queryTransactionData");
    }

    @Override
    public void logout() {
        throw new NotImplementedException("logout");
    }

}
