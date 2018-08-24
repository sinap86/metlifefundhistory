package hu.sinap86.metlifefundhistory.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static hu.sinap86.metlifefundhistory.util.Utils.getString;

@Slf4j
public class TransactionDetailLinksExtractor {

    public enum TransactionGroup {
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

        public String getGroupName() {
            return groupName;
        }
    }

    @Getter
    @Builder
    @ToString
    public static class Link {
        private TransactionGroup group;
        private String transactionNumber;
        private String url;
    }

    public List<Link> getLinks(final JsonObject rootObject) {
        final List<Link> transactionDetailLinks = new ArrayList<>();
        final Set<String> unknownTransactionGroups = new HashSet<>();

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
                continue;
            }

            final String url = getTransactionDetailUrl(ownerId, contractId, transactionNumber,
                    transactionGroup.getNameInDetailLink());
            final Link link = Link.builder()
                    .group(transactionGroup)
                    .transactionNumber(transactionNumber)
                    .url(url)
                    .build();
            transactionDetailLinks.add(link);
        }

        log.debug("Unknown transaction groups:");
        unknownTransactionGroups.forEach(log::debug);

        log.debug("Extracted transaction data links:");
        transactionDetailLinks.forEach(link -> log.debug("\t" + link));

        return transactionDetailLinks;
    }

    private static String getTransactionDetailUrl(final String ownerId, final String contractId, final String transactionNumber, final String transactionGroup) {
        return String.format("https://www.metlifehungary.hu/eFund/api/owners/%s/contracts/%s/sumlifetransactions/%s/%s", ownerId, contractId, transactionNumber, transactionGroup);
    }

}
