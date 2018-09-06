package hu.sinap86.metlifefundhistory.util;

import hu.sinap86.metlifefundhistory.model.FundHistory;
import hu.sinap86.metlifefundhistory.model.HistoryElement;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@UtilityClass
public class CommonUtils {

    public static JsonObject getAsJsonObject(final File file) throws IOException {
        return new JsonParser().parse(new FileReader(file)).getAsJsonObject();
    }

    public static JsonObject getAsJsonObject(final String json) {
        return new JsonParser().parse(json).getAsJsonObject();
    }

    private static JsonElement getAsJsonElement(final JsonObject jsonObject, final String memberName) {
        if (jsonObject == null || jsonObject.isJsonNull()) {
            return null;
        }
        final JsonElement jsonElement = jsonObject.get(memberName);
        if (jsonElement == null || jsonElement.isJsonNull()) {
            return null;
        }
        return jsonElement;
    }

    public static String getString(final JsonObject jsonObject, final String memberName) {
        final JsonElement jsonElement = getAsJsonElement(jsonObject, memberName);
        if (jsonElement == null) {
            return null;
        }
        return jsonElement.getAsString();
    }

    public static BigDecimal getBigDecimal(final JsonObject jsonObject, final String memberName) {
        final JsonElement jsonElement = getAsJsonElement(jsonObject, memberName);
        if (jsonElement == null) {
            return null;
        }
        return jsonElement.getAsBigDecimal();
    }

    public static void add(final List<FundHistory> histories, final String fundName, final String fundCode, final HistoryElement historyElement) {
        final Optional<FundHistory> optional = histories.stream().filter(h -> StringUtils.equals(h.getFundName(), fundName)).findFirst();
        if (optional.isPresent()) {
            optional.get().getHistoryElements().add(historyElement);
        } else {
            final FundHistory fundHistory = FundHistory.builder()
                    .fundName(fundName)
                    .fundCode(fundCode)
                    .build();
            fundHistory.getHistoryElements().add(historyElement);
            histories.add(fundHistory);
        }
    }


    public static void add(final List<FundHistory> source, final List<FundHistory> target) {
        source.forEach(sourceHistory -> {
            final FundHistory targetHistory = target.stream()
                    .filter(h -> StringUtils.equals(h.getFundName(), sourceHistory.getFundName()))
                    .findFirst()
                    .orElse(null);
            if (targetHistory == null) {
                target.add(sourceHistory);
            } else {
                if (StringUtils.isEmpty(targetHistory.getFundCode())) {
                    targetHistory.setFundCode(sourceHistory.getFundCode());
                }
                targetHistory.getHistoryElements().addAll(sourceHistory.getHistoryElements());
            }
        });
    }

    public static <T> void add(final Map<String, List<T>> map, final String key, final T value) {
        List<T> valueList = map.get(key);
        if (valueList == null) {
            valueList = new ArrayList<>();
            map.put(key, valueList);
        }
        valueList.add(value);
    }

    public static BigDecimal calculateYearlyAverageInterestRate(final BigDecimal totalBalance, final FundHistory history) {
        if (totalBalance == null) {
            return null;
        }
        // total balance for the whole period
        final BigDecimal sumAmountOfPositiveTransactions = history.getHistoryElements().stream()
                .filter(historyElement -> historyElement.getSumAmount().compareTo(BigDecimal.ZERO) > 0)
                .map(HistoryElement::getSumAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (BigDecimal.ZERO.equals(sumAmountOfPositiveTransactions)) {
            return BigDecimal.ZERO;
        }
        final BigDecimal totalBalanceInPercent = totalBalance.divide(sumAmountOfPositiveTransactions, RoundingMode.HALF_UP);
        // average yearly interest rate
        final long periodLengthInMonths = history.getPeriodLengthInMonths();
        if (periodLengthInMonths == 0) {
            return totalBalanceInPercent;
        }
        return totalBalanceInPercent.divide(BigDecimal.valueOf(periodLengthInMonths), RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(12));
    }

    public static BigDecimal parse(final String numberString, final NumberFormat numberFormat) throws ParseException {
        return new BigDecimal(numberFormat.parse(numberString).toString());
    }

    public static boolean containsWordMaxOneLetterDifference(final String aWord, final String string) {
        final String[] words = string.split(" ");
        for (String word : words) {
            int distance = StringUtils.getLevenshteinDistance(aWord, word);
            if (distance <= 1) {
                return true;
            }
        }
        return false;
    }

    public static void checkNotNull(final Object object, final String argName) {
        if (object == null) {
            throw new IllegalArgumentException(String.format("Argument '%s' must not be null!", argName));
        }
    }
}
