package com.edupay.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin cấu hình tài khoản nhận tiền học phí VietQR của giáo viên")
public class PaymentSettingResponse {

    @Schema(description = "ID cấu hình")
    private UUID id;

    @Schema(description = "Mã BIN 6 số của ngân hàng", example = "970422")
    private String bankBin;

    @Schema(description = "Tên ngân hàng", example = "MBBank")
    private String bankName;

    @Schema(description = "Số tài khoản ngân hàng nhận tiền", example = "0988111222")
    private String accountNumber;

    @Schema(description = "Họ và tên chủ tài khoản", example = "LE HOAI NAM")
    private String accountName;

    @Schema(description = "Tiền tố nội dung chuyển khoản", example = "HP")
    private String transferPrefix;

    @Schema(description = "Đường dẫn ảnh VietQR mẫu để kiểm tra", example = "https://img.vietqr.io/image/970422-0988111222-compact2.png?amount=50000&addInfo=TEST%20EDUPAY")
    private String vietQrSampleUrl;

    @Schema(description = "Trạng thái đã cấu hình hay chưa", example = "true")
    private boolean configured;
}

