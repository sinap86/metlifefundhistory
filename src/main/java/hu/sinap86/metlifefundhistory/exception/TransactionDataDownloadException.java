package hu.sinap86.metlifefundhistory.exception;

public class TransactionDataDownloadException extends Exception {

    public TransactionDataDownloadException() {
    }

    public TransactionDataDownloadException(final Exception e) {
        super(e);
    }

    public TransactionDataDownloadException(final String message) {
        super(message);
    }

    public TransactionDataDownloadException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
