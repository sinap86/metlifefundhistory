package hu.sinap86.metlifefundhistory.parser;

import hu.sinap86.metlifefundhistory.config.Constants;
import hu.sinap86.metlifefundhistory.model.Contract;
import hu.sinap86.metlifefundhistory.util.CommonUtils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
public class TransactionHistoryProcessor {

    private Contract contract;

    public Contract process(final File dataDirectory) throws IOException {
        Files.walk(Paths.get(dataDirectory.toURI()))
                .filter(p -> Files.isRegularFile(p) && p.getFileName().toString().toLowerCase().endsWith(Constants.JSON_FILE_EXTENSION))
                .forEach(file -> processFile(file.toFile()));

        return contract;
    }

    private void processFile(final File transactionDataFile) {
        try {
            final TransactionDataParser parser = TransactionDataParser.TransactionDataParserFactory.getInstance(transactionDataFile);
            if (parser == null) {
                log.warn("No parser for file: {}", transactionDataFile.getAbsolutePath());
                return;
            }

            final Contract parsedContract = parser.parse();
            CommonUtils.checkNotNull(parsedContract, "parsedContract");
            if (contract == null) {
                contract = parsedContract;
            } else {
                mergeData(parsedContract);
            }

            log.debug("Processed transaction detail data file: {}", transactionDataFile.getAbsolutePath());
        } catch (Exception e) {
            log.error("Cannot process transaction detail data: " + transactionDataFile.getAbsolutePath(), e);
        }
    }

    private void mergeData(final Contract source) throws IOException {
        if (!StringUtils.equals(contract.getId(), source.getId())) {
            throw new IOException("Transaction data directory contains data for multiple contracts!");
        }
        CommonUtils.add(source.getFundHistories(), contract.getFundHistories());
    }

}
