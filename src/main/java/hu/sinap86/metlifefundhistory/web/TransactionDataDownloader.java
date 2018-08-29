package hu.sinap86.metlifefundhistory.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import hu.sinap86.metlifefundhistory.config.Constants;
import hu.sinap86.metlifefundhistory.config.TransactionHistoryQuerySettings;
import hu.sinap86.metlifefundhistory.exception.TransactionDataDownloadException;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

@Slf4j
public class TransactionDataDownloader extends SwingWorker<Void, Void> {

    private static final int TRANSACTION_LIST_QUERY_PROGRESS = 10;

    private MetLifeWebSessionManager webSessionManager;
    private TransactionHistoryQuerySettings querySettings;

    public TransactionDataDownloader(final MetLifeWebSessionManager webSessionManager, final TransactionHistoryQuerySettings querySettings) {
        this.webSessionManager = webSessionManager;
        this.querySettings = querySettings;
    }

    @Override
    protected Void doInBackground() throws TransactionDataDownloadException {
        download();
        return null;
    }

    private void download() throws TransactionDataDownloadException {
        log.debug("download started.");

        final JsonObject transactionList = webSessionManager.queryTransactionHistory(querySettings);
        if (transactionList == null) {
            throw new TransactionDataDownloadException();
        }
        if (isCancelled()) {
            log.debug("download cancelled.");
            return;
        }

        setProgress(TRANSACTION_LIST_QUERY_PROGRESS);

        try {
            final List<TransactionDetailLinksExtractor.Link> detailLinks = new TransactionDetailLinksExtractor().getLinks(transactionList);
            double progressStep = (double) (100 - getProgress()) / detailLinks.size();
            for (int i = 0; i < detailLinks.size(); i++) {
                if (isCancelled()) {
                    log.debug("download cancelled.");
                    break;
                }
                final TransactionDetailLinksExtractor.Link detailLink = detailLinks.get(i);

                final JsonObject transactionData = webSessionManager.queryTransactionData(detailLink);
                final File transactionDataFile = getFile(querySettings.getTransactionHistoryDirectory(), detailLink);
                writeToFile(transactionData, transactionDataFile);
                log.debug("downloaded '{}' transaction, number: {}", detailLink.getGroup(), detailLink.getTransactionNumber());

                setProgress(TRANSACTION_LIST_QUERY_PROGRESS + (int) ((i + 1) * progressStep));
            }
        } catch (Exception e) {
            throw new TransactionDataDownloadException(e);
        }
        log.debug("download finished.");
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
