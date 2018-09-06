package hu.sinap86.metlifefundhistory.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@EqualsAndHashCode(of = { "id" })
public class Contract {

    @NonNull
    private String id;
    private String name;
    @NonNull
    private String type;
    private String typeName;
    @NonNull
    private String currency;
    private BigDecimal actualValue;
    private BigDecimal surrenderValue;
    private BigDecimal dueAmount;
    private String paidToDate;
    private final List<FundHistory> fundHistories = new ArrayList<>();
}
