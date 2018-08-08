package hu.sinap86.metlifefundhistory.xls;

import static hu.sinap86.metlifefundhistory.util.XLSUtils.createCell;
import static hu.sinap86.metlifefundhistory.util.XLSUtils.writeCells;
import static hu.sinap86.metlifefundhistory.util.XLSUtils.setColumnWidths;
import static hu.sinap86.metlifefundhistory.util.XLSUtils.nextRow;

import hu.sinap86.metlifefundhistory.model.FundHistory;
import hu.sinap86.metlifefundhistory.model.HistoryElement;
import hu.sinap86.metlifefundhistory.util.Utils;

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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

@Slf4j
public class TransactionHistoryPersister {


    @Data
    @Builder
    static class FundHistorySummary {

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
    private final Properties rateProperties = new Properties();
    private final XSSFWorkbook workbook;
    private CellStyle sheetHeaderCellStyle;
    private XSSFSheet summarySheet;

    public TransactionHistoryPersister(final File resultFile, final File ratesFile) {
        this.resultFile = resultFile;
        if (ratesFile != null) {
            try {
                rateProperties.loadFromXML(new FileInputStream(ratesFile));
                log.debug("Using exchange rates for active funds from file: {}", ratesFile.getAbsolutePath());
            } catch (IOException e) {
                log.error("Cannot load exchange rates from file: " + ratesFile.getAbsolutePath(), e);
            }
        }
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
        setColumnWidths(summarySheet, 30, 20, 20, 20, 20, 20, 30);

        final XSSFRow row = nextRow(summarySheet, 0);
        writeCells(row, sheetHeaderCellStyle, "Eszközalap", "Összes befizetés", "Összes levonás", "Aktuális érték", "Mérleg", "Átlagos éves kamat", StringUtils.EMPTY);

        summarySheet.createFreezePane(0, 1);
    }

    public void persist(final Map<String, FundHistory> fundHistoryByName) throws IOException {
        final CellStyle unitStyle = createCellStyle(CELL_STYLE_UNIT);
        final CellStyle rateStyle = createCellStyle(CELL_STYLE_RATE);
        final CellStyle amountStyle = createCellStyle(CELL_STYLE_AMOUNT);
        final List<FundHistorySummary> fundHistorySummaryList = Lists.newArrayList();

        fundHistoryByName.forEach((fundName, fundHistory) -> {
            final XSSFSheet fundSheet = workbook.createSheet(fundName);
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
            rate = getExchangeRateOrZero(fundName);
            totalBalance = history.getTotalBalance(rate);
            currentValue = totalUnits.multiply(rate);
        }
        final BigDecimal averageInterestRate = Utils.calculateYearlyAverageInterestRate(totalBalance, history);

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

    private void createFundSheetTotalRows(final XSSFSheet sheet, final FundHistorySummary fundHistorySummary) {
        final Color color = getColor(fundHistorySummary.totalBalance);
        final CellStyle boldBordered = createBoldBorderedCellStyle();

        XSSFRow row = nextRow(sheet);
        createCell(row, fundHistorySummary.rate == null ? "Záró mérleg" : String.format("Mérleg %s-n", rateProperties.getProperty("RATES_DATE")), boldBordered);
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
        createCell(row, fundHistorySummary.currentValue, cellStyleAmount);
        createCell(row, fundHistorySummary.totalBalance, createCellStyle(CELL_STYLE_AMOUNT, color, notSold));
        createCell(row, fundHistorySummary.averageInterestRate, createCellStyle(CELL_STYLE_PERCENT, color, notSold));
        if (notSold) {
            createCell(row, String.format("%s-i állapot szerint", rateProperties.getProperty("RATES_DATE")), cellStyleText);
        }
    }

    private void createSummarySheetTotalRow(final List<FundHistorySummary> fundHistorySummaryList) {
        final CellStyle boldBordered = createBoldBorderedCellStyle();
        final CellStyle boldBorderedAmount = createBoldBorderedCellStyle(CELL_STYLE_AMOUNT);

        final XSSFRow row = nextRow(summarySheet);
        createCell(row, "Össezsen", boldBordered);
        createCell(row, sum(fundHistorySummaryList, FundHistorySummary::getDepositSum), boldBorderedAmount);
        createCell(row, sum(fundHistorySummaryList, FundHistorySummary::getReductionSum), boldBorderedAmount);
        createCell(row, sum(fundHistorySummaryList, FundHistorySummary::getCurrentValue), boldBorderedAmount);
        createCell(row, sum(fundHistorySummaryList, FundHistorySummary::getTotalBalance), boldBorderedAmount);
        createCell(row, null, boldBordered);

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

    private BigDecimal getExchangeRateOrZero(final String fundName) {
        final String fundRate = rateProperties.getProperty(fundName);
        if (StringUtils.isNotEmpty(fundRate)) {
            return new BigDecimal(fundRate);
        }
        log.warn("No exchange rate for '{}' fund!", fundName);
        return BigDecimal.ZERO;
    }

    private Color getColor(final BigDecimal amountSum) {
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
