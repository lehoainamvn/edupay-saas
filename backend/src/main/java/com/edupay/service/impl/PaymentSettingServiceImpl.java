package com.edupay.service.impl;

import com.edupay.config.TenantContext;
import com.edupay.dto.request.PaymentSettingRequest;
import com.edupay.dto.response.BankResponse;
import com.edupay.dto.response.PaymentSettingResponse;
import com.edupay.entity.PaymentSetting;
import com.edupay.repository.PaymentSettingRepository;
import com.edupay.security.SecuritySanitizer;
import com.edupay.security.annotation.AuditAction;
import com.edupay.service.PaymentSettingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * Triển khai dịch vụ Quản lý cấu hình tài khoản ngân hàng nhận tiền VietQR (Self-Service)
 * và tích hợp VietQR API v2 để lấy danh sách 54+ ngân hàng chuẩn Napas247.
 * 100% nghiệp vụ cô lập theo TenantContext.getTenantId() (P0).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentSettingServiceImpl implements PaymentSettingService {

    private final PaymentSettingRepository paymentSettingRepository;
    private final ObjectMapper objectMapper;

    private static final String VIETQR_BANKS_API = "https://api.vietqr.io/v2/banks";
    private static final long CACHE_TTL_MS = 24 * 60 * 60 * 1000L; // Cache 24 giờ

    private final AtomicReference<List<BankResponse>> cachedBanks = new AtomicReference<>();
    private volatile long lastFetchTime = 0L;

    @Override
    public PaymentSettingResponse getPaymentSetting() {
        UUID tenantId = TenantContext.getTenantId();
        Optional<PaymentSetting> opt = paymentSettingRepository.findByTenantId(tenantId);

        if (opt.isEmpty()) {
            return PaymentSettingResponse.builder()
                    .configured(false)
                    .transferPrefix("HP")
                    .sampleTransferContent("HP9A01210")
                    .build();
        }

        PaymentSetting setting = opt.get();
        return toResponse(setting);
    }

    @Override
    @Transactional
    @AuditAction(action = "PAYMENT_SETTING_UPDATED", resourceType = "PAYMENT_SETTING")
    public PaymentSettingResponse savePaymentSetting(PaymentSettingRequest request) {
        UUID tenantId = TenantContext.getTenantId();

        // 1. Chuẩn hóa dữ liệu đầu vào
        String cleanBin = request.getBankBin().trim();
        String cleanBankName = SecuritySanitizer.sanitizePlainText(request.getBankName().trim());
        String cleanAccNum = request.getAccountNumber().trim().replaceAll("\\s+", "");
        String cleanAccName = removeVietnameseAccents(request.getAccountName().trim()).toUpperCase();

        String cleanPrefix = (request.getTransferPrefix() != null && !request.getTransferPrefix().isBlank())
                ? request.getTransferPrefix().trim().toUpperCase().replaceAll("[^A-Z0-9]", "")
        : "HP";
        if (cleanPrefix.isBlank()) {
            cleanPrefix = "HP";
        }

        // 2. Tìm cấu hình hiện tại để Upsert
        Optional<PaymentSetting> existingOpt = paymentSettingRepository.findByTenantId(tenantId);
        PaymentSetting setting;

        if (existingOpt.isPresent()) {
            setting = existingOpt.get();
            setting.setBankBin(cleanBin);
            setting.setBankName(cleanBankName);
            setting.setAccountNumber(cleanAccNum);
            setting.setAccountName(cleanAccName);
            setting.setTransferPrefix(cleanPrefix);
            log.info("Cập nhật cấu hình nhận tiền VietQR cho Tenant: {}", tenantId);
        } else {
            setting = PaymentSetting.builder()
                    .bankBin(cleanBin)
                    .bankName(cleanBankName)
                    .accountNumber(cleanAccNum)
                    .accountName(cleanAccName)
                    .transferPrefix(cleanPrefix)
                    .build();
            setting.setTenantId(tenantId);
            log.info("Tạo mới cấu hình nhận tiền VietQR cho Tenant: {}", tenantId);
        }

        PaymentSetting saved = paymentSettingRepository.save(setting);
        return toResponse(saved);
    }

    @Override
    public List<BankResponse> getSupportedBanks() {
        long now = System.currentTimeMillis();
        List<BankResponse> currentCache = cachedBanks.get();

        if (currentCache != null && !currentCache.isEmpty() && (now - lastFetchTime < CACHE_TTL_MS)) {
            return currentCache;
        }

        // Gọi VietQR API v2
        try {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(3000);
            requestFactory.setReadTimeout(4000);
            RestTemplate restTemplate = new RestTemplate(requestFactory);

            String jsonResponse = restTemplate.getForObject(VIETQR_BANKS_API, String.class);
            if (jsonResponse != null && !jsonResponse.isBlank()) {
                JsonNode root = objectMapper.readTree(jsonResponse);
                String code = root.path("code").asText("");
                JsonNode dataNode = root.path("data");

                if (("00".equals(code) || code.isBlank()) && dataNode.isArray() && !dataNode.isEmpty()) {
                    List<BankResponse> bankList = new ArrayList<>();
                    for (JsonNode node : dataNode) {
                        bankList.add(BankResponse.builder()
                                .id(node.path("id").isNumber() ? node.path("id").asLong() : null)
                                .bin(node.path("bin").asText(""))
                                .code(node.path("code").asText(""))
                                .shortName(node.path("shortName").asText(node.path("short_name").asText("")))
                                .name(node.path("name").asText(""))
                                .logo(node.path("logo").asText(""))
                                .transferSupported(node.path("transferSupported").asInt(1) == 1)
                                .lookupSupported(node.path("lookupSupported").asInt(1) == 1)
                                .build());
                    }

                    if (!bankList.isEmpty()) {
                        log.info("Đã đồng bộ thành công {} ngân hàng từ VietQR API v2", bankList.size());
                        cachedBanks.set(bankList);
                        lastFetchTime = now;
                        return bankList;
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Không thể gọi VietQR API ({}: {}). Tự động kích hoạt danh sách 54+ ngân hàng dự phòng.",
                    ex.getClass().getSimpleName(), ex.getMessage());
        }

        // Nếu gọi thất bại hoặc chưa có cache, trả về danh sách 54+ ngân hàng chuẩn Napas247 dự phòng
        List<BankResponse> fallback = getFallbackBanks();
        cachedBanks.set(fallback);
        lastFetchTime = now;
        return fallback;
    }

    private PaymentSettingResponse toResponse(PaymentSetting setting) {
        String cleanPrefix = (setting.getTransferPrefix() != null && !setting.getTransferPrefix().isBlank())
                ? setting.getTransferPrefix()
                : "HP";
        String sampleContent = cleanPrefix + "9A01210";
        String sampleUrl = buildSampleVietQrUrl(setting.getBankBin(), setting.getAccountNumber(), setting.getAccountName(), sampleContent);

        return PaymentSettingResponse.builder()
                .id(setting.getId())
                .bankBin(setting.getBankBin())
                .bankName(setting.getBankName())
                .accountNumber(setting.getAccountNumber())
                .accountName(setting.getAccountName())
                .transferPrefix(cleanPrefix)
                .sampleTransferContent(sampleContent)
                .vietQrSampleUrl(sampleUrl)
                .configured(true)
                .build();
    }

    private String buildSampleVietQrUrl(String bankBin, String accountNumber, String accountName, String transferContent) {
        try {
            String encodedName = URLEncoder.encode(accountName != null ? accountName : "", StandardCharsets.UTF_8);
            String encodedAddInfo = URLEncoder.encode(transferContent != null ? transferContent : "HP9A01210", StandardCharsets.UTF_8);
            return String.format(
                    "https://img.vietqr.io/image/%s-%s-compact2.png?amount=50000&addInfo=%s&accountName=%s",
                    bankBin, accountNumber, encodedAddInfo, encodedName);
        } catch (Exception e) {
            return String.format("https://img.vietqr.io/image/%s-%s-compact2.png", bankBin, accountNumber);
        }
    }

    private String removeVietnameseAccents(String str) {
        if (str == null) return "";
        String normalized = Normalizer.normalize(str, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalized).replaceAll("").replace('đ', 'd').replace('Đ', 'D');
    }

    /**
     * Danh sách 56 ngân hàng & ví điện tử chuẩn Napas247 của VietQR phục vụ Offline Fallback.
     */
    private List<BankResponse> getFallbackBanks() {
        return List.of(
                BankResponse.builder().id(1L).bin("970422").code("MB").shortName("MBBank").name("Ngân hàng TMCP Quân đội").logo("https://api.vietqr.io/img/MB.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(2L).bin("970436").code("VCB").shortName("Vietcombank").name("Ngân hàng TMCP Ngoại thương Việt Nam").logo("https://api.vietqr.io/img/VCB.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(3L).bin("970407").code("TCB").shortName("Techcombank").name("Ngân hàng TMCP Kỹ thương Việt Nam").logo("https://api.vietqr.io/img/TCB.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(4L).bin("970418").code("BIDV").shortName("BIDV").name("Ngân hàng TMCP Đầu tư và Phát triển Việt Nam").logo("https://api.vietqr.io/img/BIDV.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(5L).bin("970415").code("ICB").shortName("VietinBank").name("Ngân hàng TMCP Công thương Việt Nam").logo("https://api.vietqr.io/img/ICB.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(6L).bin("970416").code("ACB").shortName("ACB").name("Ngân hàng TMCP Á Châu").logo("https://api.vietqr.io/img/ACB.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(7L).bin("970432").code("VPB").shortName("VPBank").name("Ngân hàng TMCP Việt Nam Thịnh Vượng").logo("https://api.vietqr.io/img/VPB.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(8L).bin("970423").code("TPB").shortName("TPBank").name("Ngân hàng TMCP Tiên Phong").logo("https://api.vietqr.io/img/TPB.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(9L).bin("970403").code("STB").shortName("Sacombank").name("Ngân hàng TMCP Sài Gòn Thương Tín").logo("https://api.vietqr.io/img/STB.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(10L).bin("970437").code("HDB").shortName("HDBank").name("Ngân hàng TMCP Phát triển TP.HCM").logo("https://api.vietqr.io/img/HDB.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(11L).bin("970441").code("VIB").shortName("VIB").name("Ngân hàng TMCP Quốc tế").logo("https://api.vietqr.io/img/VIB.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(12L).bin("970443").code("SHB").shortName("SHB").name("Ngân hàng TMCP Sài Gòn - Hà Nội").logo("https://api.vietqr.io/img/SHB.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(13L).bin("970405").code("VBA").shortName("Agribank").name("Ngân hàng Nông nghiệp & PTNT Việt Nam").logo("https://api.vietqr.io/img/VBA.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(14L).bin("970448").code("OCB").shortName("OCB").name("Ngân hàng TMCP Phương Đông").logo("https://api.vietqr.io/img/OCB.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(15L).bin("970426").code("MSB").shortName("MSB").name("Ngân hàng TMCP Hàng Hải").logo("https://api.vietqr.io/img/MSB.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(16L).bin("970440").code("SEAB").shortName("SeABank").name("Ngân hàng TMCP Đông Nam Á").logo("https://api.vietqr.io/img/SEAB.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(17L).bin("970449").code("LPB").shortName("LPBank").name("Ngân hàng TMCP Lộc Phát Việt Nam").logo("https://api.vietqr.io/img/LPB.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(18L).bin("970431").code("EIB").shortName("Eximbank").name("Ngân hàng TMCP Xuất Nhập khẩu Việt Nam").logo("https://api.vietqr.io/img/EIB.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(19L).bin("970429").code("SCB").shortName("SCB").name("Ngân hàng TMCP Sài Gòn").logo("https://api.vietqr.io/img/SCB.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(20L).bin("970409").code("BAB").shortName("Bac A Bank").name("Ngân hàng TMCP Bắc Á").logo("https://api.vietqr.io/img/BAB.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(21L).bin("970428").code("NAB").shortName("Nam A Bank").name("Ngân hàng TMCP Nam Á").logo("https://api.vietqr.io/img/NAB.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(22L).bin("970412").code("PVCB").shortName("PVcomBank").name("Ngân hàng TMCP Đại Chúng Việt Nam").logo("https://api.vietqr.io/img/PVCB.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(23L).bin("970400").code("SGB").shortName("Saigonbank").name("Ngân hàng TMCP Sài Gòn Công Thương").logo("https://api.vietqr.io/img/SGB.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(24L).bin("970433").code("VBB").shortName("VietBank").name("Ngân hàng TMCP Việt Nam Thương Tín").logo("https://api.vietqr.io/img/VBB.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(25L).bin("970438").code("BVB").shortName("BaoViet Bank").name("Ngân hàng TMCP Bảo Việt").logo("https://api.vietqr.io/img/BVB.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(26L).bin("970452").code("KLB").shortName("Kienlongbank").name("Ngân hàng TMCP Kiên Long").logo("https://api.vietqr.io/img/KLB.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(27L).bin("970454").code("VCCB").shortName("BVBank").name("Ngân hàng TMCP Bản Việt").logo("https://api.vietqr.io/img/VCCB.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(28L).bin("970408").code("GPB").shortName("GPBank").name("Ngân hàng Thương mại TNHH MTV Dầu Khí Toàn Cầu").logo("https://api.vietqr.io/img/GPB.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(29L).bin("970414").code("OJB").shortName("OceanBank").name("Ngân hàng Thương mại TNHH MTV Đại Dương").logo("https://api.vietqr.io/img/OJB.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(30L).bin("970444").code("CBB").shortName("CBBank").name("Ngân hàng Thương mại TNHH MTV Xây dựng Việt Nam").logo("https://api.vietqr.io/img/CBB.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(31L).bin("970425").code("ABB").shortName("ABBANK").name("Ngân hàng TMCP An Bình").logo("https://api.vietqr.io/img/ABB.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(32L).bin("970430").code("PGB").shortName("PGBank").name("Ngân hàng TMCP Thịnh vượng và Phát triển").logo("https://api.vietqr.io/img/PGB.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(33L).bin("970442").code("SHBVN").shortName("Shinhan Bank").name("Ngân hàng TNHH MTV Shinhan Việt Nam").logo("https://api.vietqr.io/img/SHBVN.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(34L).bin("970457").code("WOO").shortName("Woori Bank").name("Ngân hàng TNHH MTV Woori Việt Nam").logo("https://api.vietqr.io/img/WOO.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(35L).bin("970410").code("SCVN").shortName("Standard Chartered").name("Ngân hàng TNHH MTV Standard Chartered Việt Nam").logo("https://api.vietqr.io/img/SCVN.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(36L).bin("458761").code("HSBC").shortName("HSBC").name("Ngân hàng TNHH MTV HSBC Việt Nam").logo("https://api.vietqr.io/img/HSBC.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(37L).bin("533948").code("CITIBANK").shortName("Citibank").name("Ngân hàng Citibank Việt Nam").logo("https://api.vietqr.io/img/CITIBANK.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(38L).bin("970439").code("PBVN").shortName("Public Bank").name("Ngân hàng TNHH MTV Public Việt Nam").logo("https://api.vietqr.io/img/PBVN.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(39L).bin("970446").code("HLBVN").shortName("Hong Leong").name("Ngân hàng TNHH MTV Hong Leong Việt Nam").logo("https://api.vietqr.io/img/HLBVN.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(40L).bin("422589").code("CIMB").shortName("CIMB").name("Ngân hàng TNHH MTV CIMB Việt Nam").logo("https://api.vietqr.io/img/CIMB.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(41L).bin("970458").code("UOB").shortName("UOB").name("Ngân hàng United Overseas Bank Việt Nam").logo("https://api.vietqr.io/img/UOB.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(42L).bin("970434").code("IVB").shortName("Indovina Bank").name("Ngân hàng TNHH Indovina").logo("https://api.vietqr.io/img/IVB.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(43L).bin("970421").code("VRB").shortName("VRB").name("Ngân hàng Liên doanh Việt - Nga").logo("https://api.vietqr.io/img/VRB.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(44L).bin("970456").code("IBKHN").shortName("IBK Hà Nội").name("Ngân hàng Công nghiệp Hàn Quốc - Chi nhánh Hà Nội").logo("https://api.vietqr.io/img/IBK.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(45L).bin("970455").code("IBKHCM").shortName("IBK TP.HCM").name("Ngân hàng Công nghiệp Hàn Quốc - Chi nhánh TP.HCM").logo("https://api.vietqr.io/img/IBK.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(46L).bin("801011").code("NHB").shortName("Nonghyup").name("Ngân hàng Nonghyup - Chi nhánh Hà Nội").logo("https://api.vietqr.io/img/NHB.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(47L).bin("970462").code("KBHN").shortName("Kookmin Hà Nội").name("Ngân hàng Kookmin - Chi nhánh Hà Nội").logo("https://api.vietqr.io/img/KBHN.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(48L).bin("970463").code("KBHCM").shortName("Kookmin TP.HCM").name("Ngân hàng Kookmin - Chi nhánh TP.HCM").logo("https://api.vietqr.io/img/KBHCM.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(49L).bin("970466").code("KEBHANAHN").shortName("KEB Hana HN").name("Ngân hàng KEB Hana - Chi nhánh Hà Nội").logo("https://api.vietqr.io/img/KEBHANAHN.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(50L).bin("970467").code("KEBHANAHCM").shortName("KEB Hana HCM").name("Ngân hàng KEB Hana - Chi nhánh TP.HCM").logo("https://api.vietqr.io/img/KEBHANAHCM.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(51L).bin("668888").code("KBANK").shortName("KBank").name("Ngân hàng Kasikornbank - Chi nhánh TP.HCM").logo("https://api.vietqr.io/img/KBANK.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(52L).bin("970445").code("COOPBANK").shortName("Co-opBank").name("Ngân hàng Hợp tác xã Việt Nam").logo("https://api.vietqr.io/img/COOPBANK.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(53L).bin("963388").code("TIMO").shortName("Timo").name("Ngân hàng số Timo by BVBank").logo("https://api.vietqr.io/img/TIMO.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(54L).bin("546034").code("CAKE").shortName("Cake").name("Ngân hàng số Cake by VPBank").logo("https://api.vietqr.io/img/CAKE.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(55L).bin("971005").code("VTLMONEY").shortName("Viettel Money").name("Tổng Công ty Dịch vụ Số Viettel").logo("https://api.vietqr.io/img/VIETTELMONEY.png").transferSupported(true).lookupSupported(true).build(),
                BankResponse.builder().id(56L).bin("971011").code("VNPTMONEY").shortName("VNPT Money").name("Tập đoàn Bưu chính Viễn thông Việt Nam").logo("https://api.vietqr.io/img/VNPTMONEY.png").transferSupported(true).lookupSupported(true).build()
        );
    }
}
