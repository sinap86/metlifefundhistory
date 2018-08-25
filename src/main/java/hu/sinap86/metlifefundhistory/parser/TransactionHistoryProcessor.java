package hu.sinap86.metlifefundhistory.parser;

import hu.sinap86.metlifefundhistory.model.FundHistory;
import hu.sinap86.metlifefundhistory.config.Constants;
import hu.sinap86.metlifefundhistory.util.CommonUtils;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
public class TransactionHistoryProcessor {

    private final Map<String, FundHistory> fundHistoryByName = new TreeMap<>();

    public Map<String, FundHistory> process(final File dataDirectory) throws IOException {
        fundHistoryByName.clear();

        Files.walk(Paths.get(dataDirectory.toURI()))
                .filter(p -> Files.isRegularFile(p) && p.getFileName().toString().toLowerCase().endsWith(Constants.JSON_FILE_EXTENSION))
                .forEach(file -> processFile(file.toFile()));

        return fundHistoryByName;
    }

    private void processFile(final File transactionDataFile) {
        try {
            final TransactionDataParser parser = TransactionDataParser.TransactionDataParserFactory.getInstance(transactionDataFile);
            if (parser == null) {
                log.warn("No parser for file: {}", transactionDataFile.getAbsolutePath());
                return;
            }

            final Collection<FundHistory> parsedHistories = parser.parse();
            CommonUtils.addHistoryElements(fundHistoryByName, parsedHistories);
            log.debug("Processed transaction detail data file: {}", transactionDataFile.getAbsolutePath());
        } catch (Exception e) {
            log.error("Cannot process transaction detail data: " + transactionDataFile.getAbsolutePath(), e);
        }
    }

}
