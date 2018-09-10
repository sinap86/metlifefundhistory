package hu.sinap86.metlifefundhistory.config;

import lombok.Getter;
import lombok.ToString;

import java.io.File;
import java.time.LocalDate;

@Getter
@ToString
public class TransactionHistoryQuerySettings extends ReportGeneratorSettings {

    private String contract;
    private LocalDate fromDate;
    private LocalDate toDate;

    public static class TransactionHistoryQuerySettingsBuilder extends ReportGeneratorSettings.ReportGeneratorSettingsBuilder {

        protected TransactionHistoryQuerySettings toBuild;

        public TransactionHistoryQuerySettingsBuilder() {
            toBuild = new TransactionHistoryQuerySettings();
        }

        public TransactionHistoryQuerySettingsBuilder useOnlineRates(boolean useOnlineRates) {
            toBuild.useOnlineRates = useOnlineRates;
            return this;
        }

        public TransactionHistoryQuerySettingsBuilder rateDate(LocalDate rateDate) {
            toBuild.rateDate = rateDate;
            return this;
        }

        public TransactionHistoryQuerySettingsBuilder rateFile(File rateFile) {
            toBuild.rateFile = rateFile;
            return this;
        }

        public TransactionHistoryQuerySettingsBuilder transactionHistoryDirectory(File transactionHistoryDirectory) {
            toBuild.transactionHistoryDirectory = transactionHistoryDirectory;
            return this;
        }

        public TransactionHistoryQuerySettingsBuilder contract(String contract) {
            toBuild.contract = contract;
            return this;
        }

        public TransactionHistoryQuerySettingsBuilder fromDate(LocalDate fromDate) {
            toBuild.fromDate = fromDate;
            return this;
        }

        public TransactionHistoryQuerySettingsBuilder toDate(LocalDate toDate) {
            toBuild.toDate = toDate;
            return this;
        }

        public TransactionHistoryQuerySettings build() {
            return toBuild;
        }
    }

    public static TransactionHistoryQuerySettingsBuilder builder() {
        return new TransactionHistoryQuerySettingsBuilder();
    }
}
