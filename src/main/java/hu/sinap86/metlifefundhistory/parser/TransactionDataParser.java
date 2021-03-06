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
import lombok.Builder;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Builder
public class TransactionDataParser {

    protected static final String TRANSACTION_NAME = "transactionName";
    protected static final String TRANSACTION_CODE = "transactionCode";
    protected static final String TRANSACTION_DATE = "transactionDate";
    protected static final String FUND_NAME = "fundName";
    protected static final String UNIT_VIRTUAL_FUND = "unitVirtualFund";

    protected final JsonObject rootObject;
    protected final String transactionArrayTagName;
    protected final String numberOfInitialUnitsTagName;
    protected final String numberOfAccumulationUnitsTagName;
    protected final String rateTagName;
    protected final String priceDateTagName;

    public TransactionDataParser(final JsonObject rootObject, final String transactionArrayTagName, final String numberOfInitialUnitsTagName, final String numberOfAccumulationUnitsTagName,
                                 final String rateTagName, final String priceDateTagName) {
        CommonUtils.checkNotNull(rootObject, "rootObject");
        this.rootObject = rootObject;
        this.transactionArrayTagName = transactionArrayTagName;
        this.numberOfInitialUnitsTagName = numberOfInitialUnitsTagName;
        this.numberOfAccumulationUnitsTagName = numberOfAccumulationUnitsTagName;
        this.rateTagName = rateTagName;
        this.priceDateTagName = priceDateTagName;
    }

    public Contract parse() {
        final Contract contract = parseContract(rootObject);
        final String transactionName = getString(rootObject, TRANSACTION_NAME);
        final String transactionCode = getString(rootObject, TRANSACTION_CODE);
        final String transactionDate = getString(rootObject, TRANSACTION_DATE);

        final List<FundHistory> fundHistories = contract.getFundHistories();
        final JsonArray transactionArray = rootObject.getAsJsonArray(transactionArrayTagName);
        CommonUtils.checkNotNull(transactionArray, "transactionArray");

        for (JsonElement transactionElement : transactionArray) {
            final JsonObject transaction = transactionElement.getAsJsonObject();

            final String fundName = getString(transaction, FUND_NAME);
            final String fundCode = getString(transaction, UNIT_VIRTUAL_FUND);
            final BigDecimal numberOfInitialUnits = getBigDecimal(transaction, numberOfInitialUnitsTagName);
            final BigDecimal numberOfAccumulationUnits = getBigDecimal(transaction, numberOfAccumulationUnitsTagName);
            final BigDecimal rate = getBigDecimal(transaction, rateTagName);
            final String priceDate = getString(transaction, priceDateTagName);

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
        return contract;
    }

    protected Contract parseContract(final JsonObject rootObject) {
        final JsonObject contract = rootObject.getAsJsonObject("contract");
        CommonUtils.checkNotNull(contract, "contract");

        return Contract.builder()
                .id(getString(contract, "contractId"))
                .name(getString(contract, "fantasyName"))
                .type(getString(contract, "contractTypeNumber"))
                .typeName(getString(contract, "contractTypeName"))
                .currency(getString(contract, "currency"))
                .actualValue(getBigDecimal(contract, "actualValue"))
                .surrenderValue(getBigDecimal(contract, "surrenderValue"))
                .dueAmount(getBigDecimal(contract, "dueAmount"))
                .paidToDate(getString(contract, "paidToDate"))
                .build();
    }

    public static class TransactionDataParserFactory {

        public static TransactionDataParser getInstance(final File transactionDataFile) throws IOException {
            final JsonObject rootObject = CommonUtils.getAsJsonObject(transactionDataFile);
            final String transactionCode = getString(rootObject, "transactionCode");
            if (transactionCode == null) {
                return null;
            }

            switch (transactionCode) {
                // Rendszeres díj kiegyenlítés (B522), Előrefizetés (T536) - Premium
                case "B522":
                case "T536": {
                    return TransactionDataParser.builder()
                            .rootObject(rootObject)
                            .transactionArrayTagName("premiumValues")
                            .numberOfInitialUnitsTagName("investedInitUnitNumber")
                            .numberOfAccumulationUnitsTagName("investedAcumUnitNumber")
                            .rateTagName("sellRate")
                            .priceDateTagName("priceDate")
                            .build();
                }
                // Költségelvonás (B633) - BenefitBilling
                case "B633": {
                    return TransactionDataParser.builder()
                            .rootObject(rootObject)
                            .transactionArrayTagName("benefitBillingValues")
                            .numberOfInitialUnitsTagName("numberOfInitialUnits")
                            .numberOfAccumulationUnitsTagName("numberOfAccumulationUnits")
                            .rateTagName("exchangeRate")
                            .priceDateTagName("priceDateUsed")
                            .build();
                }
                // Évfordulós költségelvonás (B675) - RenewalsAnniversaryProcess
                case "B675": {
                    return TransactionDataParser.builder()
                            .rootObject(rootObject)
                            .transactionArrayTagName("unitValues")
                            .numberOfInitialUnitsTagName("numberOfDunits")
                            .numberOfAccumulationUnitsTagName(null)
                            .rateTagName("buyRate")
                            .priceDateTagName("priceDate")
                            .build();
                }
                // Egységáthelyezés, főbiztosítás (BZ89) - FundSwitch
                case "BZ89": {
                    return new FundSwitchDataParser(rootObject);
                }
                default:
                    throw new IllegalStateException(String.format("No data parser configured for '%s' transaction!", transactionCode));
            }
        }
    }
}
