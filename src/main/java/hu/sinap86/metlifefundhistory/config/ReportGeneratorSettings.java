package hu.sinap86.metlifefundhistory.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.File;
import java.time.LocalDate;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ReportGeneratorSettings {

    protected boolean useOnlineRates;
    protected LocalDate rateDate;
    protected File rateFile;
    protected File transactionHistoryDirectory;
}
