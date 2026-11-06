package com.edupay.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO đại diện cho thông tin một ngân hàng hỗ trợ chuẩn VietQR Napas247.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin ngân hàng hỗ trợ chuẩn VietQR Napas247")
public class BankResponse {

    @Schema(description = "Mã định danh nội bộ VietQR", example = "1")
    private Long id;

    @Schema(description = "Mã BIN 6 số của ngân hàng do NAPAS quy định", example = "970422")
    private String bin;

    @Schema(description = "Mã định danh viết tắt của ngân hàng", example = "MB")
    private String code;

    @Schema(description = "Tên viết tắt thương mại của ngân hàng", example = "MBBank")
    private String shortName;

    @Schema(description = "Tên đầy đủ của ngân hàng bằng tiếng Việt", example = "Ngân hàng TMCP Quân đội")
    private String name;

    @Schema(description = "URL ảnh logo chính thức của ngân hàng", example = "https://api.vietqr.io/img/MB.png")
    private String logo;

    @Schema(description = "Hỗ trợ tạo mã VietQR chuyển tiền Napas247", example = "true")
    private boolean transferSupported;

    @Schema(description = "Hỗ trợ tra cứu tên chủ tài khoản", example = "true")
    private boolean lookupSupported;
}
