package hu.sinap86.metlifefundhistory.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import hu.sinap86.metlifefundhistory.config.TransactionHistoryQuerySettings;
import hu.sinap86.metlifefundhistory.exception.TransactionDataDownloadException;
import hu.sinap86.metlifefundhistory.config.Constants;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

@Slf4j
public class TransactionDataDownloader {

    private MetLifeWebSessionManager webSessionManager;

    public TransactionDataDownloader(final MetLifeWebSessionManager webSessionManager) {
        this.webSessionManager = webSessionManager;
    }

    public void download(final TransactionHistoryQuerySettings querySettings) throws TransactionDataDownloadException {
        final JsonObject transactionList = webSessionManager.queryTransactionHistory(querySettings);
        if (transactionList == null) {
            throw new TransactionDataDownloadException();
        }

        try {
            final List<TransactionDetailLinksExtractor.Link> detailLinks = new TransactionDetailLinksExtractor().getLinks(transactionList);
            for (TransactionDetailLinksExtractor.Link detailLink : detailLinks) {
                final JsonObject transactionData = webSessionManager.queryTransactionData(detailLink);
                final File transactionDataFile = getFile(querySettings.getTransactionHistoryDirectory(), detailLink);
                writeToFile(transactionData, transactionDataFile);
            }
        } catch (Exception e) {
            throw new TransactionDataDownloadException(e);
        }
    }

    private File getFile(final File rootDirectory, final TransactionDetailLinksExtractor.Link link) throws IOException {
        final File transactionGroupDirectory = new File(rootDirectory, link.getGroup().getGroupName());
        if (!transactionGroupDirectory.exists()) {
            boolean created = transactionGroupDirectory.mkdir();
            if (!created) {
                throw new IOException("Cannot create directory: " + transactionGroupDirectory.getAbsolutePath());
            }
        }
        return new File(transactionGroupDirectory, link.getTransactionNumber() + Constants.JSON_FILE_EXTENSION);
    }

    private void writeToFile(final JsonObject transactionData, final File file) throws IOException {
        try (final FileWriter writer = new FileWriter(file)) {
            final Gson gson = new GsonBuilder().create();
            gson.toJson(transactionData, writer);
        }
    }
}
