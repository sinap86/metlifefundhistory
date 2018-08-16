package hu.sinap86.metlifefundhistory.config;

import lombok.Builder;
import lombok.Data;

import java.io.File;

@Data
@Builder
public class ReportGeneratorSettings {

    private boolean useOnlineRates;
    private File rateFile;
    private File transactionHistoryDirectory;
}
