package hu.sinap86.metlifefundhistory.config;

import lombok.experimental.UtilityClass;

import java.text.DecimalFormat;

@UtilityClass
public class Constants {

    public static final String[] SUPPORTED_PROXY_SCHEMES = new String[]{ "http", "https" };

    public static final String USAGE_DESCRIPTION_FILE = "usage_description.html";

    public static final String JSON_FILE_EXTENSION = ".json";

    // TODO settings?
    public static final String REPORT_FILE_NAME = "results.xlsx";

    public static final DecimalFormat UI_AMOUNT_FORMAT = new DecimalFormat("###,##0.##");
}
