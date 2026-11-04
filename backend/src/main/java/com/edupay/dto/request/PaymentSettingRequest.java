package com.edupay.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu cấu hình tài khoản ngân hàng nhận tiền học phí VietQR")
public class PaymentSettingRequest {

    @NotBlank(message = "Mã BIN ngân hàng không được để trống")
    @Pattern(regexp = "^\\d{6}$", message = "Mã BIN ngân hàng phải gồm đúng 6 chữ số chuẩn Napas247")
    @Schema(description = "Mã BIN 6 số của ngân hàng (Ví dụ: 970422 cho MB, 970436 cho VCB)", example = "970422")
    private String bankBin;

    @NotBlank(message = "Tên ngân hàng không được để trống")
    @Size(max = 100, message = "Tên ngân hàng tối đa 100 ký tự")
    @Schema(description = "Tên viết tắt hoặc tên giao dịch của ngân hàng", example = "MBBank")
    private String bankName;

    @NotBlank(message = "Số tài khoản ngân hàng không được để trống")
    @Size(min = 4, max = 50, message = "Số tài khoản phải từ 4 đến 50 ký tự")
    @Schema(description = "Số tài khoản ngân hàng nhận tiền", example = "0988111222")
    private String accountNumber;

    @NotBlank(message = "Tên chủ tài khoản không được để trống")
    @Size(max = 255, message = "Tên chủ tài khoản tối đa 255 ký tự")
    @Schema(description = "Họ và tên chủ tài khoản (In hoa không dấu hoặc có dấu)", example = "LE HOAI NAM")
    private String accountName;

    @Size(max = 20, message = "Tiền tố chuyển khoản tối đa 20 ký tự")
    @Pattern(regexp = "^[A-Za-z0-9]*$", message = "Tiền tố chuyển khoản chỉ được chứa chữ cái và chữ số không dấu")
    @Schema(description = "Tiền tố nội dung chuyển khoản (Mặc định: HP)", example = "HP")
    @Builder.Default
    private String transferPrefix = "HP";
}

