package hu.sinap86.metlifefundhistory.report;

import hu.sinap86.metlifefundhistory.parser.TransactionHistoryProcessor;
import hu.sinap86.metlifefundhistory.config.ReportGeneratorSettings;
import hu.sinap86.metlifefundhistory.model.FundHistory;
import hu.sinap86.metlifefundhistory.report.rate.FileRateProvider;
import hu.sinap86.metlifefundhistory.report.rate.OnlineRateProvider;
import hu.sinap86.metlifefundhistory.report.rate.RateProvider;
import hu.sinap86.metlifefundhistory.util.Constants;
import hu.sinap86.metlifefundhistory.report.persist.SpreadsheetTransactionHistoryPersister;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@Slf4j
public class FundReportGenerator {

    private ReportGeneratorSettings settings;

    public FundReportGenerator(final ReportGeneratorSettings settings) {
        if (settings == null) {
            throw new IllegalStateException("No settings given!");
        }
        // TODO validate settings
        this.settings = settings;
    }

    public File generate() throws IOException {

        final Map<String, FundHistory> fundHistoryByName = new TransactionHistoryProcessor().process(settings.getTransactionHistoryDirectory());
        if (fundHistoryByName.isEmpty()) {
            log.warn("No fund history elements were parsed.");
            return null;
        }

        return persist(fundHistoryByName);
    }

    private File persist(final Map<String, FundHistory> fundHistoryByName) throws IOException {
        final RateProvider rateProvider = settings.isUseOnlineRates() ? new OnlineRateProvider() : new FileRateProvider(settings.getRateFile());
        final File resultFile = new File(settings.getTransactionHistoryDirectory(), Constants.REPORT_FILE_NAME);

        new SpreadsheetTransactionHistoryPersister(resultFile, rateProvider).persist(fundHistoryByName);
        log.debug("Report generated successfully.");
        return resultFile;
    }
}
