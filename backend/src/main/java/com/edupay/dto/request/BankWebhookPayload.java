package com.edupay.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO nhận dữ liệu Webhook giao dịch biến động số dư từ SePay hoặc Casso.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BankWebhookPayload {

    // SePay standard fields
    private Long id;
    private String gateway;
    private String transactionDate;
    private String accountNumber;
    
    @JsonAlias({"amountIn", "amount"})
    private BigDecimal amountIn;
    
    private BigDecimal amountOut;
    private BigDecimal accumulated;
    private String code;
    
    @JsonAlias({"transactionContent", "description", "content"})
    private String transactionContent;
    
    @JsonAlias({"referenceNumber", "referenceCode", "tid"})
    private String referenceNumber;
    
    private String body;

    // Casso standard wrapper support
    private Integer error;
    private List<CassoTransactionItem> data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CassoTransactionItem {
        private Long id;
        private String tid;
        private String description;
        private BigDecimal amount;
        private String when;
        @JsonAlias({"bank_sub_acc_id", "accountNumber"})
        private String bankSubAccId;
    }
}
