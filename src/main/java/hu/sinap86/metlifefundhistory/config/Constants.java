package hu.sinap86.metlifefundhistory.config;

import lombok.experimental.UtilityClass;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@UtilityClass
public class Constants {

    public static final String[] SUPPORTED_PROXY_SCHEMES = new String[]{ "http", "https" };

    public static final String USAGE_DESCRIPTION_FILE = "usage_description.html";

    public static final String JSON_FILE_EXTENSION = ".json";

    public static final DecimalFormat UI_AMOUNT_FORMAT = new DecimalFormat("###,##0.##");

    public static final Locale LOCALE_HU = new Locale("hu");

    public static final NumberFormat NUMBER_FORMAT_HU = NumberFormat.getInstance(LOCALE_HU);

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    public static final DateTimeFormatter RESULT_FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd_HHmm");
}
