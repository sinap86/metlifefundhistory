package hu.sinap86.metlifefundhistory.config;

import lombok.Builder;
import lombok.Data;

import java.io.File;
import java.time.LocalDate;

@Data
@Builder
public class TransactionHistoryQuerySettings {

    private String contract;
    private LocalDate fromDate;
    private LocalDate toDate;
    private File transactionHistoryDirectory;

}
