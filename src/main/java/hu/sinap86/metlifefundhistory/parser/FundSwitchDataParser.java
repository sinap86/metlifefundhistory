package hu.sinap86.metlifefundhistory.parser;

import static hu.sinap86.metlifefundhistory.util.CommonUtils.getBigDecimal;
import static hu.sinap86.metlifefundhistory.util.CommonUtils.getString;

import hu.sinap86.metlifefundhistory.model.Contract;
import hu.sinap86.metlifefundhistory.model.FundHistory;
import hu.sinap86.metlifefundhistory.model.HistoryElement;
import hu.sinap86.metlifefundhistory.util.CommonUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.math.BigDecimal;
import java.util.List;

public class FundSwitchDataParser extends TransactionDataParser {

    public FundSwitchDataParser(final JsonObject rootObject) {
        super(rootObject, null, "nofDunitsInit", "nofDunitsAcum", "priceUsed", "priceDate");
    }

    @Override
    public Contract parse() {
        final Contract contract = parseContract(rootObject);
        final List<FundHistory> fundHistories = contract.getFundHistories();
        processTransactionDataArray(fundHistories, "fundSwitchSources");
        processTransactionDataArray(fundHistories, "fundSwitchTargets");
        return contract;
    }

    private void processTransactionDataArray(final List<FundHistory> fundHistories, final String transactionArrayTagName) {
        final String transactionName = getString(rootObject, TRANSACTION_NAME);
        final String transactionCode = getString(rootObject, TRANSACTION_CODE);
        final String transactionDate = getString(rootObject, TRANSACTION_DATE);
        final String priceDate = getString(rootObject, priceDateTagName);

        final JsonArray transactionArray = rootObject.getAsJsonArray(transactionArrayTagName);
        for (JsonElement transactionElement : transactionArray) {
            final JsonObject transaction = transactionElement.getAsJsonObject();

            final String fundName = getString(transaction, FUND_NAME);
            final String fundCode = getString(transaction, UNIT_VIRTUAL_FUND);
            final BigDecimal numberOfInitialUnits = getBigDecimal(transaction, numberOfInitialUnitsTagName);
            final BigDecimal numberOfAccumulationUnits = getBigDecimal(transaction, numberOfAccumulationUnitsTagName);
            final BigDecimal rate = getBigDecimal(transaction, rateTagName);

            final HistoryElement historyElement = HistoryElement.builder()
                    .transactionName(transactionName)
                    .transactionCode(transactionCode)
                    .transactionDate(transactionDate)
                    .numberOfInitialUnits(numberOfInitialUnits)
                    .numberOfAccumulationUnits(numberOfAccumulationUnits)
                    .rate(rate)
                    .priceDate(priceDate)
                    .build();
            CommonUtils.add(fundHistories, fundName, fundCode, historyElement);
        }
    }
}
