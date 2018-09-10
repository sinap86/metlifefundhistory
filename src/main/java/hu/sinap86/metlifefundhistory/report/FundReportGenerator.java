package hu.sinap86.metlifefundhistory.report;

import hu.sinap86.metlifefundhistory.config.Constants;
import hu.sinap86.metlifefundhistory.config.ReportGeneratorSettings;
import hu.sinap86.metlifefundhistory.model.Contract;
import hu.sinap86.metlifefundhistory.parser.TransactionHistoryProcessor;
import hu.sinap86.metlifefundhistory.report.persist.SpreadsheetTransactionHistoryPersister;
import hu.sinap86.metlifefundhistory.report.rate.FileRateProvider;
import hu.sinap86.metlifefundhistory.report.rate.OnlineRateProvider;
import hu.sinap86.metlifefundhistory.report.rate.RateProvider;
import hu.sinap86.metlifefundhistory.util.CommonUtils;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

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
        CommonUtils.checkNotNull(settings, "settings");
        this.settings = settings;
    }

    @Override
    protected Void doInBackground() throws Exception {
        try {
            generate();
        } catch (Exception e) {
            log.error("Exception occurred during execution:", e);
        }
        setProgress(100);
        return null;
    }

    private void generate() throws IOException {
        log.debug("report generation started.");

        final Contract contract = new TransactionHistoryProcessor().process(settings.getTransactionHistoryDirectory());
        if (contract == null || contract.getFundHistories().isEmpty()) {
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

        this.result = persist(contract);
        log.debug("report generation finished.");
    }

    private Result persist(final Contract contract) throws IOException {
        final RateProvider rateProvider = settings.isUseOnlineRates() ? new OnlineRateProvider(contract, settings.getRateDate()) :
                                          new FileRateProvider(settings.getRateFile());
        log.debug("Rates provided for active funds.");

        if (isCancelled()) {
            log.debug("report generation cancelled.");
            return null;
        }
        setProgress(RATES_PROVIDED_PROGRESS);

        final File resultFile = getResultFile();
        final SpreadsheetTransactionHistoryPersister persister = new SpreadsheetTransactionHistoryPersister(resultFile, rateProvider);
        final List<String> warnings = persister.persist(contract);
        log.debug("Report generated successfully with {} warnings.", warnings.size());

        return Result.builder()
                .reportFile(resultFile)
                .warnings(warnings)
                .build();
    }

    private File getResultFile() {
        final String filename = String.format("results-%s.xlsx", LocalDateTime.now().format(Constants.RESULT_FILE_NAME_FORMATTER));
        return new File(settings.getTransactionHistoryDirectory(), filename);
    }

    public Result getResult() {
        return result;
    }
}
