package hu.sinap86.metlifefundhistory.report;

import hu.sinap86.metlifefundhistory.config.Constants;
import hu.sinap86.metlifefundhistory.config.ReportGeneratorSettings;
import hu.sinap86.metlifefundhistory.model.FundHistory;
import hu.sinap86.metlifefundhistory.parser.TransactionHistoryProcessor;
import hu.sinap86.metlifefundhistory.report.persist.SpreadsheetTransactionHistoryPersister;
import hu.sinap86.metlifefundhistory.report.rate.FileRateProvider;
import hu.sinap86.metlifefundhistory.report.rate.OnlineRateProvider;
import hu.sinap86.metlifefundhistory.report.rate.RateProvider;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
public class FundReportGenerator {

    @Builder
    @Getter
    @ToString
    public static class Result {

        private File reportFile;
        private List<String> warnings;
    }

    private ReportGeneratorSettings settings;

    public FundReportGenerator(final ReportGeneratorSettings settings) {
        if (settings == null) {
            throw new IllegalStateException("No settings given!");
        }
        this.settings = settings;
    }

    public Result generate() throws IOException {

        final Map<String, FundHistory> fundHistoryByName = new TransactionHistoryProcessor().process(settings.getTransactionHistoryDirectory());
        if (fundHistoryByName.isEmpty()) {
            log.warn("No fund history elements were parsed.");
            return null;
        }

        return persist(fundHistoryByName);
    }

    private Result persist(final Map<String, FundHistory> fundHistoryByName) throws IOException {
        final RateProvider rateProvider = settings.isUseOnlineRates() ? new OnlineRateProvider() : new FileRateProvider(settings.getRateFile());
        final File resultFile = new File(settings.getTransactionHistoryDirectory(), Constants.REPORT_FILE_NAME);

        final SpreadsheetTransactionHistoryPersister persister = new SpreadsheetTransactionHistoryPersister(resultFile, rateProvider);
        final List<String> warnings = persister.persist(fundHistoryByName);
        log.debug("Report generated successfully with {} warnings.", warnings.size());

        return Result.builder()
                .reportFile(resultFile)
                .warnings(warnings)
                .build();
    }
}
