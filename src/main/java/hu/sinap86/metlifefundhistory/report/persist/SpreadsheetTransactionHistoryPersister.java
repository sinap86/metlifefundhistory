package hu.sinap86.metlifefundhistory.report.persist;

import static hu.sinap86.metlifefundhistory.util.XLSUtils.createCell;
import static hu.sinap86.metlifefundhistory.util.XLSUtils.nextRow;
import static hu.sinap86.metlifefundhistory.util.XLSUtils.setColumnWidths;
import static hu.sinap86.metlifefundhistory.util.XLSUtils.writeCells;

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
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.awt.*;
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
public class SpreadsheetTransactionHistoryPersister {

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

    private static final String CELL_STYLE_UNIT = "#,##0.#####";
    private static final String CELL_STYLE_RATE = "0.#####";
    private static final String CELL_STYLE_AMOUNT = "#,##0 Ft";
    private static final String CELL_STYLE_PERCENT = "0.##%";

    private final File resultFile;
    private final RateProvider rateProvider;
    private final XSSFWorkbook workbook;
    private final List<String> warnings = new ArrayList<>();
    private CellStyle sheetHeaderCellStyle;
    private XSSFSheet summarySheet;

    public SpreadsheetTransactionHistoryPersister(final File resultFile, final RateProvider rateProvider) {
        this.resultFile = resultFile;
        this.rateProvider = rateProvider;
        this.workbook = new XSSFWorkbook();
        initStyles();
        initSummarySheet();
    }

    private void initStyles() {
        this.sheetHeaderCellStyle = createCellStyle(null, null, true, false);
        sheetHeaderCellStyle.setWrapText(true);
        sheetHeaderCellStyle.setAlignment(HorizontalAlignment.CENTER);
        sheetHeaderCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
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
        writeCells(row, sheetHeaderCellStyle, (Object[]) headerTexts);

        summarySheet.createFreezePane(0, 1);
    }

    public List<String> persist(final Contract contract) throws IOException {
        CommonUtils.checkNotNull(contract, "contract");
        warnings.clear();

        final CellStyle unitStyle = createCellStyle(CELL_STYLE_UNIT);
        final CellStyle rateStyle = createCellStyle(CELL_STYLE_RATE);
        final CellStyle amountStyle = createCellStyle(CELL_STYLE_AMOUNT);
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
                createCell(row, historyElement.getSumOfUnits(), unitStyle);
                createCell(row, historyElement.getRate(), rateStyle);
                createCell(row, historyElement.getSumAmount(), amountStyle);
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
        writeCells(row, sheetHeaderCellStyle, "Használt árfolyam napja", "Kezdeti és felhalmozási egységek száma (db)", "Egységárfolyam", "Összeg", "Művelet");

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

        final Color color = getColor(fundHistorySummary.totalBalance);
        final CellStyle boldBordered = createBoldBorderedCellStyle();

        XSSFRow row = nextRow(sheet);
        createCell(row, fundHistorySummary.rate == null ? "Záró mérleg" : String.format("Mérleg %s-n", rateProvider.getRateDate()), boldBordered);
        createCell(row, StringUtils.EMPTY, boldBordered);
        createCell(row, fundHistorySummary.rate, boldBordered);
        createCell(row, fundHistorySummary.totalBalance, createBoldBorderedCellStyle(CELL_STYLE_AMOUNT, color));
        createCell(row, StringUtils.EMPTY, boldBordered);

        row = nextRow(sheet);
        createCell(row, "Átlagos éves kamat", boldBordered);
        createCell(row, StringUtils.EMPTY, boldBordered);
        createCell(row, StringUtils.EMPTY, boldBordered);
        createCell(row, fundHistorySummary.averageInterestRate, createBoldBorderedCellStyle(CELL_STYLE_PERCENT, color));
        createCell(row, StringUtils.EMPTY, boldBordered);
    }

    private void createSummarySheetRow(final FundHistorySummary fundHistorySummary) {
        final boolean notSold = !fundHistorySummary.fundSold;
        final Color color = getColor(fundHistorySummary.totalBalance);
        final CellStyle cellStyleText = createCellStyle(null, notSold);
        final CellStyle cellStyleAmount = createCellStyle(CELL_STYLE_AMOUNT, notSold);

        final XSSFRow row = nextRow(summarySheet);
        createCell(row, fundHistorySummary.fundName, cellStyleText);
        createCell(row, fundHistorySummary.depositSum, cellStyleAmount);
        createCell(row, fundHistorySummary.reductionSum, cellStyleAmount);

        if (notSold && !rateProvider.isRatesLoadedSuccessfully()) {
            // no rates are available for active funds
            createCell(row, StringUtils.EMPTY, cellStyleText);
            createCell(row, StringUtils.EMPTY, cellStyleText);
        } else {
            // fund was sold or (active and rates are available)
            if (rateProvider.isRatesLoadedSuccessfully()) {
                createCell(row, fundHistorySummary.currentValue, cellStyleAmount);
            }
            createCell(row, fundHistorySummary.totalBalance, createCellStyle(CELL_STYLE_AMOUNT, color, notSold));
            createCell(row, fundHistorySummary.averageInterestRate, createCellStyle(CELL_STYLE_PERCENT, color, notSold));
        }
        if (notSold) {
            if (rateProvider.isRatesLoadedSuccessfully() && fundHistorySummary.currentValue != null) {
                createCell(row, String.format("%s-i állapot szerint", rateProvider.getRateDate()), cellStyleText);
            } else {
                createCell(row, "Hiányzó árfolyam", cellStyleText);
            }
        }
    }

    private void createSummarySheetTotalRow(final List<FundHistorySummary> fundHistorySummaryList) {
        final CellStyle boldBordered = createBoldBorderedCellStyle();
        final CellStyle boldBorderedAmount = createBoldBorderedCellStyle(CELL_STYLE_AMOUNT);

        final XSSFRow row = nextRow(summarySheet);
        createCell(row, "Össezsen", boldBordered);
        createCell(row, sumOrNull(fundHistorySummaryList, FundHistorySummary::getDepositSum), boldBorderedAmount);
        createCell(row, sumOrNull(fundHistorySummaryList, FundHistorySummary::getReductionSum), boldBorderedAmount);
        if (rateProvider.isRatesLoadedSuccessfully()) {
            createCell(row, sumOrNull(fundHistorySummaryList, FundHistorySummary::getCurrentValue), boldBorderedAmount);
        }
        createCell(row, sumOrNull(fundHistorySummaryList, FundHistorySummary::getTotalBalance), boldBorderedAmount);
        createCell(row, null, boldBordered);

    }

    private BigDecimal sumOrNull(final List<FundHistorySummary> fundHistorySummaryList, final Function<FundHistorySummary, BigDecimal> function) {
        BigDecimal sum = BigDecimal.ZERO;
        for (FundHistorySummary summary : fundHistorySummaryList) {
            final BigDecimal result = function.apply(summary);
            if (result == null) {
                return null;
            }
            sum = sum.add(result);
        }
        return sum;
    }

    private BigDecimal sumAmount(final FundHistory history, final String... transactionCodes) {
        return history.getHistoryElements().stream()
                .filter(historyElement -> Lists.newArrayList(transactionCodes).contains(historyElement.getTransactionCode()))
                .map(HistoryElement::getSumAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Color getColor(final BigDecimal amountSum) {
        if (amountSum == null) {
            return Color.BLACK;
        }
        final Color color;
        final int compare = amountSum.compareTo(BigDecimal.ZERO);
        if (compare < 0) {
            color = Color.RED;
        } else if (compare > 0) {
            color = new Color(0, 102, 0);
        } else {
            color = Color.BLACK;
        }
        return color;
    }

    private CellStyle createBoldBorderedCellStyle() {
        return createBoldBorderedCellStyle(null);
    }

    private CellStyle createBoldBorderedCellStyle(final String format) {
        return createBoldBorderedCellStyle(format, null);
    }

    private CellStyle createBoldBorderedCellStyle(final String format, final Color color) {
        final CellStyle cellStyle = createCellStyle(format, color, true, false);
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setBorderTop(BorderStyle.THIN);
        return cellStyle;
    }

    private CellStyle createCellStyle(final String format) {
        return createCellStyle(format, null, false, false);
    }

    private CellStyle createCellStyle(final String format, final boolean boldAndItalic) {
        return createCellStyle(format, null, boldAndItalic);
    }

    private CellStyle createCellStyle(final String format, final Color color, final boolean boldAndItalic) {
        return createCellStyle(format, color, boldAndItalic, boldAndItalic);
    }

    private CellStyle createCellStyle(final String format, final Color color, final boolean bold, final boolean italic) {
        final XSSFCellStyle cellStyle = workbook.createCellStyle();
        if (StringUtils.isNotEmpty(format)) {
            cellStyle.setDataFormat(workbook.createDataFormat().getFormat(format));
        }
        final XSSFFont font = workbook.createFont();
        cellStyle.setFont(font);
        if (bold) {
            font.setBold(true);
        }
        if (italic) {
            font.setItalic(true);
        }
        if (color != null) {
            font.setColor(new XSSFColor(color));
        }
        return cellStyle;
    }
}
