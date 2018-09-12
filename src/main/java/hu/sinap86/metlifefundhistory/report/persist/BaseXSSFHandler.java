package hu.sinap86.metlifefundhistory.report.persist;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.awt.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class BaseXSSFHandler {

    public enum Style {
        TEXT,
        TEXT_RED,
        TEXT_SHEET_HEADER,
        TEXT_BOLD_ITALIC,
        TEXT_BOLD_BORDERED,
        AMOUNT,
        AMOUNT_GREEN,
        AMOUNT_RED,
        AMOUNT_BOLD_ITALIC,
        AMOUNT_BOLD_ITALIC_GREEN,
        AMOUNT_BOLD_ITALIC_RED,
        AMOUNT_BOLD_BORDERED,
        AMOUNT_BOLD_BORDERED_GREEN,
        AMOUNT_BOLD_BORDERED_RED,
        PERCENT,
        PERCENT_GREEN,
        PERCENT_RED,
        PERCENT_BOLD_ITALIC,
        PERCENT_BOLD_ITALIC_GREEN,
        PERCENT_BOLD_ITALIC_RED,
        PERCENT_BOLD_BORDERED_GREEN,
        PERCENT_BOLD_BORDERED_RED,
        UNIT,
        RATE
    }

    private static final String CELL_STYLE_UNIT = "#,##0.#####";
    private static final String CELL_STYLE_RATE = "0.#####";
    private static final String CELL_STYLE_AMOUNT = "#,##0 Ft";
    private static final String CELL_STYLE_PERCENT = "0.##%";

    protected final XSSFWorkbook workbook;
    private final Map<Style, CellStyle> styles = new HashMap<>();

    public BaseXSSFHandler() {
        this.workbook = new XSSFWorkbook();
        initStyles();
    }

    private void initStyles() {
        final Color red = Color.RED;
        final Color green = new Color(0, 102, 0);

        final CellStyle sheetHeaderCellStyle = createCellStyle(null, null, true, false);
        sheetHeaderCellStyle.setWrapText(true);
        sheetHeaderCellStyle.setAlignment(HorizontalAlignment.CENTER);
        sheetHeaderCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        styles.put(Style.TEXT, createCellStyle(null, false));
        styles.put(Style.TEXT_RED, createCellStyle(null, red, false));
        styles.put(Style.TEXT_SHEET_HEADER, sheetHeaderCellStyle);
        styles.put(Style.TEXT_BOLD_BORDERED, createBoldBorderedCellStyle());
        styles.put(Style.TEXT_BOLD_ITALIC, createCellStyle(null, true));
        styles.put(Style.AMOUNT, createCellStyle(CELL_STYLE_AMOUNT, false));
        styles.put(Style.AMOUNT_GREEN, createCellStyle(CELL_STYLE_AMOUNT, green, false));
        styles.put(Style.AMOUNT_RED, createCellStyle(CELL_STYLE_AMOUNT, red, false));
        styles.put(Style.AMOUNT_BOLD_BORDERED, createBoldBorderedCellStyle(CELL_STYLE_AMOUNT));
        styles.put(Style.AMOUNT_BOLD_BORDERED_GREEN, createBoldBorderedCellStyle(CELL_STYLE_AMOUNT, green));
        styles.put(Style.AMOUNT_BOLD_BORDERED_RED, createBoldBorderedCellStyle(CELL_STYLE_AMOUNT, red));
        styles.put(Style.AMOUNT_BOLD_ITALIC, createCellStyle(CELL_STYLE_AMOUNT, true));
        styles.put(Style.AMOUNT_BOLD_ITALIC_GREEN, createCellStyle(CELL_STYLE_AMOUNT, green, true));
        styles.put(Style.AMOUNT_BOLD_ITALIC_RED, createCellStyle(CELL_STYLE_AMOUNT, red, true));
        styles.put(Style.PERCENT, createCellStyle(CELL_STYLE_PERCENT, false));
        styles.put(Style.PERCENT_GREEN, createCellStyle(CELL_STYLE_PERCENT, green, false));
        styles.put(Style.PERCENT_RED, createCellStyle(CELL_STYLE_PERCENT, red, false));
        styles.put(Style.PERCENT_BOLD_BORDERED_GREEN, createBoldBorderedCellStyle(CELL_STYLE_PERCENT, green));
        styles.put(Style.PERCENT_BOLD_BORDERED_RED, createBoldBorderedCellStyle(CELL_STYLE_PERCENT, red));
        styles.put(Style.PERCENT_BOLD_ITALIC, createCellStyle(CELL_STYLE_PERCENT, true));
        styles.put(Style.PERCENT_BOLD_ITALIC_GREEN, createCellStyle(CELL_STYLE_PERCENT, green, true));
        styles.put(Style.PERCENT_BOLD_ITALIC_RED, createCellStyle(CELL_STYLE_PERCENT, red, true));
        styles.put(Style.UNIT, createCellStyle(CELL_STYLE_UNIT));
        styles.put(Style.RATE, createCellStyle(CELL_STYLE_RATE));
    }

    protected CellStyle getCellStyle(final Style style) {
        if (styles.containsKey(style)) {
            return styles.get(style);
        }
        log.warn("No cell style configured for {}", style);
        return null;
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

    protected void setColumnWidths(final XSSFSheet sheet, final int... widths) {
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
        }
    }

    protected void writeCells(final XSSFRow row, final BaseXSSFHandler.Style style, final Object... cellValues) {
        for (Object cellValue : cellValues) {
            createCell(row, cellValue, style);
        }
    }

    protected void createCell(final XSSFRow row, final Object value) {
        createCell(row, value, null);
    }

    protected void createCell(final XSSFRow row, final Object value, final BaseXSSFHandler.Style style) {
        final XSSFCell cell = nextCell(row);
        if (value == null) {
            cell.setCellValue(StringUtils.EMPTY);
        } else if (value instanceof BigDecimal) {
            cell.setCellValue(((BigDecimal) value).doubleValue());
        } else if (value instanceof String) {
            cell.setCellValue((String) value);
        } else {
            cell.setCellValue(String.valueOf(value));
        }
        if (style != null) {
            cell.setCellStyle(getCellStyle(style));
        }
    }

    protected XSSFRow nextRow(final XSSFSheet sheet) {
        return nextRow(sheet, sheet.getLastRowNum() + 1);
    }

    protected XSSFRow nextRow(final XSSFSheet sheet, final int rowNum) {
        return sheet.createRow(rowNum);
    }

    protected XSSFCell nextCell(final XSSFRow row) {
        return row.createCell(row.getLastCellNum() < 0 ? 0 : row.getLastCellNum());
    }
}
