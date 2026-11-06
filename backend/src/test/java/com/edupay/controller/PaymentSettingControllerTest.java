package com.edupay.controller;

import com.edupay.dto.request.PaymentSettingRequest;
import com.edupay.dto.response.BankResponse;
import com.edupay.dto.response.PaymentSettingResponse;
import com.edupay.security.JwtTokenProvider;
import com.edupay.security.RateLimitingFilter;
import com.edupay.service.PaymentSettingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentSettingController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentSettingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentSettingService paymentSettingService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private RateLimitingFilter rateLimitingFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /settings/payment trả về 200 OK và cấu hình ngân hàng")
    void getPaymentSetting_ReturnsOk() throws Exception {
        PaymentSettingResponse res = PaymentSettingResponse.builder()
                .id(UUID.randomUUID())
                .bankBin("970422")
                .bankName("MBBank")
                .accountNumber("0988111222")
                .accountName("LE HOAI NAM")
                .transferPrefix("HP")
                .sampleTransferContent("HP9A01210")
                .configured(true)
                .build();

        when(paymentSettingService.getPaymentSetting()).thenReturn(res);

        mockMvc.perform(get("/settings/payment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bankBin").value("970422"))
                .andExpect(jsonPath("$.data.bankName").value("MBBank"))
                .andExpect(jsonPath("$.data.accountNumber").value("0988111222"))
                .andExpect(jsonPath("$.data.sampleTransferContent").value("HP9A01210"));
    }

    @Test
    @DisplayName("PUT /settings/payment lưu thông tin tài khoản thành công")
    void savePaymentSetting_ValidRequest_ReturnsOk() throws Exception {
        PaymentSettingRequest request = PaymentSettingRequest.builder()
                .bankBin("970422")
                .bankName("MBBank")
                .accountNumber("0988111222")
                .accountName("LE HOAI NAM")
                .transferPrefix("HP")
                .build();

        PaymentSettingResponse response = PaymentSettingResponse.builder()
                .id(UUID.randomUUID())
                .bankBin("970422")
                .bankName("MBBank")
                .accountNumber("0988111222")
                .accountName("LE HOAI NAM")
                .transferPrefix("HP")
                .sampleTransferContent("HP9A01210")
                .configured(true)
                .build();

        when(paymentSettingService.savePaymentSetting(any(PaymentSettingRequest.class))).thenReturn(response);

        mockMvc.perform(put("/settings/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bankBin").value("970422"))
                .andExpect(jsonPath("$.data.sampleTransferContent").value("HP9A01210"));
    }

    @Test
    @DisplayName("GET /settings/payment/banks trả về danh sách các ngân hàng hỗ trợ")
    void getSupportedBanks_ReturnsOk() throws Exception {
        List<BankResponse> banks = List.of(
                BankResponse.builder().bin("970422").code("MB").shortName("MBBank").name("Ngân hàng TMCP Quân đội").build(),
                BankResponse.builder().bin("970436").code("VCB").shortName("Vietcombank").name("Ngân hàng TMCP Ngoại thương").build()
        );

        when(paymentSettingService.getSupportedBanks()).thenReturn(banks);

        mockMvc.perform(get("/settings/payment/banks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].bin").value("970422"))
                .andExpect(jsonPath("$.data[0].shortName").value("MBBank"))
                .andExpect(jsonPath("$.data[1].bin").value("970436"));
    }
}
