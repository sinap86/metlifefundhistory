package hu.sinap86.metlifefundhistory;

import hu.sinap86.metlifefundhistory.model.FundHistory;
import hu.sinap86.metlifefundhistory.parser.TransactionDataParser;
import hu.sinap86.metlifefundhistory.util.Utils;
import hu.sinap86.metlifefundhistory.xls.TransactionHistoryPersister;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
public class TransactionHistoryProcessor {

    public static void main(String[] args) throws Exception {
        final File dataDirectory = getTransactionDataDirectory(args);
        final File resultFile = new File(dataDirectory.getParentFile(), "results.xlsx");
        final File ratesFile = getRatesFileOrNull(args);
        new TransactionHistoryProcessor(resultFile, ratesFile).execute(dataDirectory);
    }

    private static File getTransactionDataDirectory(final String[] args) {
        if (ArrayUtils.isEmpty(args)) {
            throw new IllegalStateException("Command line argument required: transaction data directory path");
        }
        final File dataDirectory = new File(args[0]);
        if (!dataDirectory.exists() || !dataDirectory.isDirectory()) {
            throw new IllegalArgumentException(String.format("Transactions data directory (%s) does not exists or not a directory!", dataDirectory.getAbsolutePath()));
        }
        if (!dataDirectory.canWrite()) {
            throw new IllegalStateException(String.format("Cannot save result: transactions data directory (%s) is not writable!", dataDirectory.getAbsolutePath()));
        }
        log.debug("Extracting transaction data from directory: {}", dataDirectory.getAbsolutePath());
        return dataDirectory;
    }

    private static File getRatesFileOrNull(final String[] args) {
        if (ArrayUtils.isNotEmpty(args) && args.length > 1) {
            return new File(args[1]);
        }
        return null;
    }

    private final File resultFile;
    private final File ratesFile;
    private final Map<String, FundHistory> fundHistoryByName = new TreeMap<>();

    public TransactionHistoryProcessor(final File resultFile, final File ratesFile) {
        this.resultFile = resultFile;
        this.ratesFile = ratesFile;
    }

    private void execute(final File dataDirectory) throws IOException {
        Files.walk(Paths.get(dataDirectory.toURI()))
                .filter(Files::isRegularFile)
                .forEach(file -> process(file.toFile()));

        if (fundHistoryByName.isEmpty()) {
            log.warn("No fund history elements were parsed.");
            return;
        }

        new TransactionHistoryPersister(resultFile, ratesFile).persist(fundHistoryByName);
    }

    private void process(final File transactionDataFile) {
        try {
            final TransactionDataParser parser = TransactionDataParser.TransactionDataParserFactory.getInstance(transactionDataFile);

            final Collection<FundHistory> parsedHistories = parser.parse();
            Utils.addHistoryElements(fundHistoryByName, parsedHistories);
            log.debug("Processed transaction detail data file: {}", transactionDataFile.getAbsolutePath());
        } catch (Exception e) {
            log.error("Cannot process transaction detail data: " + transactionDataFile.getAbsolutePath(), e);
        }
    }

}
