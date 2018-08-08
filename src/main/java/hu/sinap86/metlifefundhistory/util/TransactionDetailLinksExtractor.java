package hu.sinap86.metlifefundhistory.util;

import static hu.sinap86.metlifefundhistory.util.Utils.getString;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class TransactionDetailLinksExtractor {

    enum TransactionGroup {
        PREMIUM("Premium"),
        BENEFIT_BILLING("BenefitBilling"),
        RENEWALS_ANNIVERSARY("RenwalsAnniversary"),
        FUND_SWITCH("FundSwitch");

        private String groupName;

        TransactionGroup(final String groupName) {
            this.groupName = groupName;
        }

        public static TransactionGroup lookup(final String groupName) {
            if (StringUtils.isEmpty(groupName)) {
                return null;
            }
            for (TransactionGroup group : values()) {
                if (groupName.equals(group.groupName)) {
                    return group;
                }
            }
            return null;
        }

        String getNameInDetailLink() {
            if (this == RENEWALS_ANNIVERSARY) {
                return "renewalsanniversaryprocess";
            }
            return groupName.toLowerCase();
        }
    }

    public static void main(String[] args) throws Exception {
        final File transactionListFile = getTransactionListFile(args);

        final Map<String, List<String>> transactionDetailLinks = new HashMap<>();
        final Set<String> unknownTransactionGroups = new HashSet<>();

        final JsonObject rootObject = Utils.getAsJsonObject(transactionListFile);
        final String ownerId = getString(rootObject, "ownerId");
        final String contractId = getString(rootObject.getAsJsonObject("contract"), "contractId");
        final JsonArray transactions = rootObject.getAsJsonArray("transactions");
        for (final JsonElement element : transactions) {
            final JsonObject transaction = element.getAsJsonObject();
            final String transactionGroupName = getString(transaction, "transactionGroup");
            if (StringUtils.isEmpty(transactionGroupName)) {
                continue;
            }
            final String transactionName = getString(transaction, "transactionName");
            final String transactionNumber = getString(transaction, "tranno");

            final TransactionGroup transactionGroup = TransactionGroup.lookup(transactionGroupName);
            if (transactionGroup == null) {
                unknownTransactionGroups.add(String.format("\t%s (%s)", transactionName, transactionGroupName));
            } else {
                final String transactionDetailLink = getTransactionDetailLink(ownerId, contractId, transactionNumber, transactionGroup.getNameInDetailLink());
                Utils.add(transactionDetailLinks, transactionName, transactionDetailLink);
            }
        }

        log.debug("Excluded transaction groups:");
        unknownTransactionGroups.forEach(log::debug);

        log.debug("Processed transaction types:");
        transactionDetailLinks.forEach((group, links) -> {
            log.debug("\t" + group);
            links.forEach(link -> log.debug("\t\t" + link));
        });
    }

    private static File getTransactionListFile(final String[] args) {
        if (ArrayUtils.isEmpty(args)) {
            throw new IllegalStateException("Command line argument required: transaction list data file path");
        }

        final File transactionsFile = new File(args[0]);
        if (!transactionsFile.exists() || !transactionsFile.canRead()) {
            throw new IllegalArgumentException(String.format("Transaction list data file (%s) does not exists or not readable!", transactionsFile.getAbsolutePath()));
        }
        log.debug("Extracting transaction data links from list file: {}", transactionsFile.getAbsolutePath());
        return transactionsFile;
    }

    private static String getTransactionDetailLink(final String ownerId, final String contractId, final String transactionNumber, final String transactionGroup) {
        return String.format("https://www.metlifehungary.hu/eFund/api/owners/%s/contracts/%s/sumlifetransactions/%s/%s", ownerId, contractId, transactionNumber, transactionGroup);
    }

}
