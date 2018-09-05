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

import javax.swing.*;

@Slf4j
public class FundReportGenerator extends SwingWorker<Void, Void> {

    public static final int TRANSACTION_LIST_PARSED_PROGRESS = 20;
    public static final int RATES_PROVIDED_PROGRESS = 50;
    private Result result;

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

    @Override
    protected Void doInBackground() throws Exception {
        generate();
        return null;
    }

    private void generate() throws IOException {
        log.debug("report generation started.");

        final Map<String, FundHistory> fundHistoryByName = new TransactionHistoryProcessor().process(settings.getTransactionHistoryDirectory());
        if (fundHistoryByName.isEmpty()) {
            setProgress(100);
            log.warn("No fund history elements were parsed.");
            return;
        }
        log.debug("Fund history elements parsed.");

        if (isCancelled()) {
            log.debug("report generation cancelled.");
            return;
        }

        setProgress(TRANSACTION_LIST_PARSED_PROGRESS);

        this.result = persist(fundHistoryByName);
        log.debug("report generation finished.");
        setProgress(100);
    }

    private Result persist(final Map<String, FundHistory> fundHistoryByName) throws IOException {
        // TODO get params from fundHistoryByName
        final RateProvider rateProvider = settings.isUseOnlineRates() ? new OnlineRateProvider("653", "HUF") :
                                          new FileRateProvider(settings.getRateFile());
        log.debug("Rates provided for active funds.");

        if (isCancelled()) {
            log.debug("report generation cancelled.");
            return null;
        }
        setProgress(RATES_PROVIDED_PROGRESS);

        final File resultFile = new File(settings.getTransactionHistoryDirectory(), Constants.REPORT_FILE_NAME);

        final SpreadsheetTransactionHistoryPersister persister = new SpreadsheetTransactionHistoryPersister(resultFile, rateProvider);
        final List<String> warnings = persister.persist(fundHistoryByName);
        log.debug("Report generated successfully with {} warnings.", warnings.size());

        return Result.builder()
                .reportFile(resultFile)
                .warnings(warnings)
                .build();
    }

    public Result getResult() {
        return result;
    }
}
