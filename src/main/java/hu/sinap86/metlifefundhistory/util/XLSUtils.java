package hu.sinap86.metlifefundhistory.util;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import java.math.BigDecimal;

@UtilityClass
public class XLSUtils {

    public static void setColumnWidths(final XSSFSheet sheet, final int... widths) {
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
        }
    }

    public static void writeCells(final XSSFRow row, final CellStyle style, final Object... cellValues) {
        for (Object cellValue : cellValues) {
            createCell(row, cellValue, style);
        }
    }

    public static void createCell(final XSSFRow row, final Object value) {
        createCell(row, value, null);
    }

    public static void createCell(final XSSFRow row, final Object value, final CellStyle style) {
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
            cell.setCellStyle(style);
        }
    }

    public static XSSFRow nextRow(final XSSFSheet sheet) {
        return nextRow(sheet, sheet.getLastRowNum() + 1);
    }

    public static XSSFRow nextRow(final XSSFSheet sheet, final int rowNum) {
        return sheet.createRow(rowNum);
    }

    public static XSSFCell nextCell(final XSSFRow row) {
        return row.createCell(row.getLastCellNum() < 0 ? 0 : row.getLastCellNum());
    }
}
