package hu.sinap86.metlifefundhistory.report.persist;

import hu.sinap86.metlifefundhistory.model.Contract;
import hu.sinap86.metlifefundhistory.model.FundHistory;
import hu.sinap86.metlifefundhistory.model.HistoryElement;
import hu.sinap86.metlifefundhistory.report.rate.RateProvider;
import hu.sinap86.metlifefundhistory.util.CommonUtils;

import com.google.common.collect.Lists;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

@Slf4j
public class SpreadsheetTransactionHistoryPersister extends BaseXSSFHandler {

    @Data
    @Builder
    private static class FundHistorySummary {

        private String fundName;
        private BigDecimal depositSum;
        private BigDecimal reductionSum;
        private BigDecimal currentValue;
        private BigDecimal rate;
        private BigDecimal totalBalance;
        private BigDecimal averageInterestRate;
        private boolean fundSold;
    }

    private final File resultFile;
    private final RateProvider rateProvider;
    private final List<String> warnings = new ArrayList<>();

    private XSSFSheet summarySheet;

    public SpreadsheetTransactionHistoryPersister(final File resultFile, final RateProvider rateProvider) {
        this.resultFile = resultFile;
        this.rateProvider = rateProvider;
        initSummarySheet();
    }

    private void initSummarySheet() {
        summarySheet = workbook.createSheet("Összesen");

        final int[] columnWidths;
        final String[] headerTexts;
        if (rateProvider.isRatesLoadedSuccessfully()) {
            columnWidths = new int[]{ 30, 20, 20, 20, 20, 20, 30 };
            headerTexts = new String[]{ "Eszközalap", "Összes befizetés", "Összes levonás", "Aktuális érték", "Mérleg", "Átlagos éves kamat", StringUtils.EMPTY };
        } else {
            columnWidths = new int[]{ 30, 20, 20, 20, 20, 30 };
            headerTexts = new String[]{ "Eszközalap", "Összes befizetés", "Összes levonás", "Mérleg", "Átlagos éves kamat", StringUtils.EMPTY };
        }

        setColumnWidths(summarySheet, columnWidths);

        final XSSFRow row = nextRow(summarySheet, 0);
        writeCells(row, Style.TEXT_SHEET_HEADER, (Object[]) headerTexts);

        summarySheet.createFreezePane(0, 1);
    }

    public List<String> persist(final Contract contract) throws IOException {
        CommonUtils.checkNotNull(contract, "contract");
        warnings.clear();

        final List<FundHistorySummary> fundHistorySummaryList = Lists.newArrayList();

        final List<FundHistory> fundHistories = contract.getFundHistories();
        fundHistories.sort(Comparator.comparing(FundHistory::getFundName));
        fundHistories.forEach(fundHistory -> {
            final XSSFSheet fundSheet = workbook.createSheet(fundHistory.getFundName());
            createFundSheetHeader(fundSheet);

            Collections.sort(fundHistory.getHistoryElements());
            for (HistoryElement historyElement : fundHistory.getHistoryElements()) {
                final XSSFRow row = nextRow(fundSheet);

                createCell(row, historyElement.getPriceDate());
                createCell(row, historyElement.getSumOfUnits(), Style.UNIT);
                createCell(row, historyElement.getRate(), Style.RATE);
                createCell(row, historyElement.getSumAmount(), Style.AMOUNT);
                createCell(row, historyElement.getTransactionName());
            }

            final FundHistorySummary summary = summarize(fundHistory);
            createSummarySheetRow(summary);
            createFundSheetTotalRows(fundSheet, summary);
            fundHistorySummaryList.add(summary);
        });

        createSummarySheetTotalRow(fundHistorySummaryList);

        // Write the output to a file
        try (FileOutputStream fileOut = new FileOutputStream(resultFile)) {
            workbook.write(fileOut);
            log.info("Results written into: {}", resultFile.getAbsolutePath());
        }
        return warnings;
    }

    private void createFundSheetHeader(final XSSFSheet sheet) {
        setColumnWidths(sheet, 30, 30, 20, 20, 30);

        final XSSFRow row = nextRow(sheet, 0);
        writeCells(row, Style.TEXT_SHEET_HEADER, "Használt árfolyam napja", "Kezdeti és felhalmozási egységek száma (db)", "Egységárfolyam", "Összeg", "Művelet");

        sheet.createFreezePane(0, 1);
    }

    private FundHistorySummary summarize(final FundHistory history) {
        final String fundName = history.getFundName();
        final BigDecimal totalUnits = history.getTotalUnits();
        final boolean fundSold = totalUnits.doubleValue() == 0;

        final BigDecimal rate;
        final BigDecimal totalBalance;
        final BigDecimal currentValue;
        if (fundSold) {
            rate = null;
            totalBalance = history.getTotalBalance();
            currentValue = null;
        } else {
            rate = getExchangeRate(fundName);
            totalBalance = rate != null ? history.getTotalBalance(rate) : null;
            currentValue = rate != null ? totalUnits.multiply(rate) : null;
        }
        final BigDecimal averageInterestRate = CommonUtils.calculateYearlyAverageInterestRate(totalBalance, history);

        // Rendszeres díj kiegyenlítés (B522), Előrefizetés (T536)
        final BigDecimal depositSum = sumAmount(history, "B522", "T536");
        // Költségelvonás (B633), Évfordulós költségelvonás (B675)
        final BigDecimal reductionSum = sumAmount(history, "B633", "B675");

        return FundHistorySummary.builder()
                .fundName(fundName)
                .depositSum(depositSum)
                .reductionSum(reductionSum)
                .currentValue(currentValue)
                .rate(rate)
                .totalBalance(totalBalance)
                .averageInterestRate(averageInterestRate)
                .fundSold(fundSold)
                .build();
    }

    private BigDecimal getExchangeRate(final String fundName) {
        final BigDecimal fundRate = rateProvider.getExchangeRate(fundName);
        if (fundRate == null) {
            log.warn("No exchange rate for '{}' fund!.", fundName);
            warnings.add(String.format("Nem található árfolyam a(z) '%s' alaphoz!", fundName));
        }
        return fundRate;
    }

    private void createFundSheetTotalRows(final XSSFSheet sheet, final FundHistorySummary fundHistorySummary) {
        if (fundHistorySummary.totalBalance == null || fundHistorySummary.averageInterestRate == null) {
            return;
        }

        final Style textStyle = Style.TEXT_BOLD_BORDERED;
        final Style coloredAmountStyle = fundHistorySummary.totalBalance.compareTo(BigDecimal.ZERO) >= 0 ? Style.AMOUNT_BOLD_BORDERED_GREEN : Style.AMOUNT_BOLD_BORDERED_RED;
        final Style coloredPercentStyle = fundHistorySummary.averageInterestRate.compareTo(BigDecimal.ZERO) >= 0 ? Style.PERCENT_BOLD_BORDERED_GREEN : Style.PERCENT_BOLD_BORDERED_RED;

        XSSFRow row = nextRow(sheet);
        createCell(row, fundHistorySummary.rate == null ? "Záró mérleg" : String.format("Mérleg %s-n", rateProvider.getRateDate()), textStyle);
        createCell(row, StringUtils.EMPTY, textStyle);
        createCell(row, fundHistorySummary.rate, textStyle);
        createCell(row, fundHistorySummary.totalBalance, coloredAmountStyle);
        createCell(row, StringUtils.EMPTY, textStyle);

        row = nextRow(sheet);
        createCell(row, "Átlagos éves kamat", textStyle);
        createCell(row, StringUtils.EMPTY, textStyle);
        createCell(row, StringUtils.EMPTY, textStyle);
        createCell(row, fundHistorySummary.averageInterestRate, coloredPercentStyle);
        createCell(row, StringUtils.EMPTY, textStyle);
    }

    private void createSummarySheetRow(final FundHistorySummary fundHistorySummary) {
        final boolean notSold = !fundHistorySummary.fundSold;

        final Style textStyle = fundHistorySummary.fundSold ? Style.TEXT : Style.TEXT_BOLD_ITALIC;
        final Style amountStyle = fundHistorySummary.fundSold ? Style.AMOUNT : Style.AMOUNT_BOLD_ITALIC;
        final Style coloredAmountStyle = getColoredAmountStyle(fundHistorySummary.totalBalance, fundHistorySummary.fundSold);
        final Style coloredPercentStyle = getColoredPercentStyle(fundHistorySummary.averageInterestRate, fundHistorySummary.fundSold);

        final XSSFRow row = nextRow(summarySheet);
        createCell(row, fundHistorySummary.fundName, textStyle);
        createCell(row, fundHistorySummary.depositSum, amountStyle);
        createCell(row, fundHistorySummary.reductionSum, amountStyle);

        if (notSold && !rateProvider.isRatesLoadedSuccessfully()) {
            // no rates are available for active funds
            createCell(row, StringUtils.EMPTY, textStyle);
            createCell(row, StringUtils.EMPTY, textStyle);
        } else {
            // fund was sold or (active and rates are available)
            if (rateProvider.isRatesLoadedSuccessfully()) {
                createCell(row, fundHistorySummary.currentValue, amountStyle);
            }
            createCell(row, fundHistorySummary.totalBalance, coloredAmountStyle);
            createCell(row, fundHistorySummary.averageInterestRate, coloredPercentStyle);
        }
        if (notSold) {
            if (rateProvider.isRatesLoadedSuccessfully() && fundHistorySummary.currentValue != null) {
                createCell(row, String.format("%s-i állapot szerint", rateProvider.getRateDate()), textStyle);
            } else {
                createCell(row, "Hiányzó árfolyam", Style.TEXT_RED);
            }
        }
    }

    private Style getColoredAmountStyle(final BigDecimal number, final boolean isSimple) {
        if (number == null) {
            return Style.AMOUNT;
        }

        final boolean isPositive = number.compareTo(BigDecimal.ZERO) >= 0;
        if (isPositive) {
            if (isSimple) {
                return Style.AMOUNT_GREEN;
            } else {
                return Style.AMOUNT_BOLD_ITALIC_GREEN;
            }
        } else {
            if (isSimple) {
                return Style.AMOUNT_RED;
            } else {
                return Style.AMOUNT_BOLD_ITALIC_RED;
            }
        }
    }

    private Style getColoredPercentStyle(final BigDecimal number, final boolean isSimple) {
        if (number == null) {
            return Style.PERCENT;
        }

        final boolean isPositive = number.compareTo(BigDecimal.ZERO) >= 0;
        if (isPositive) {
            if (isSimple) {
                return Style.PERCENT_GREEN;
            } else {
                return Style.PERCENT_BOLD_ITALIC_GREEN;
            }
        } else {
            if (isSimple) {
                return Style.PERCENT_RED;
            } else {
                return Style.PERCENT_BOLD_ITALIC_RED;
            }
        }
    }

    private void createSummarySheetTotalRow(final List<FundHistorySummary> fundHistorySummaryList) {
        final Style textStyle = Style.TEXT_BOLD_BORDERED;
        final Style amountStyle = Style.AMOUNT_BOLD_BORDERED;

        final boolean hasAtLeastOneActiveFundWithoutRate = fundHistorySummaryList.stream().anyMatch(s -> !s.fundSold && s.getRate() == null);

        final XSSFRow row = nextRow(summarySheet);
        createCell(row, "Össezsen", textStyle);
        createCell(row, sum(fundHistorySummaryList, FundHistorySummary::getDepositSum), amountStyle);
        createCell(row, sum(fundHistorySummaryList, FundHistorySummary::getReductionSum), amountStyle);
        if (rateProvider.isRatesLoadedSuccessfully()) {
            if (hasAtLeastOneActiveFundWithoutRate) {
                createCell(row, StringUtils.EMPTY, textStyle);
            } else {
                createCell(row, sum(fundHistorySummaryList, FundHistorySummary::getCurrentValue), amountStyle);
            }
        }
        if (hasAtLeastOneActiveFundWithoutRate) {
            createCell(row, StringUtils.EMPTY, textStyle);
        } else {
            createCell(row, sum(fundHistorySummaryList, FundHistorySummary::getTotalBalance), amountStyle);
        }
        createCell(row, null, textStyle);

    }

    private BigDecimal sum(final List<FundHistorySummary> fundHistorySummaryList, final Function<FundHistorySummary, BigDecimal> function) {
        BigDecimal sum = BigDecimal.ZERO;
        for (FundHistorySummary summary : fundHistorySummaryList) {
            final BigDecimal result = function.apply(summary);
            if (result != null) {
                sum = sum.add(result);
            }
        }
        return sum;
    }

    private BigDecimal sumAmount(final FundHistory history, final String... transactionCodes) {
        return history.getHistoryElements().stream()
                .filter(historyElement -> Lists.newArrayList(transactionCodes).contains(historyElement.getTransactionCode()))
                .map(HistoryElement::getSumAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
