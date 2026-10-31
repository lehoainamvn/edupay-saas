package com.edupay.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Kết quả trả về sau khi Import hàng loạt học sinh từ file Excel / CSV.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Kết quả import danh sách học sinh từ file Excel/CSV")
public class StudentImportResultResponse {

    @Schema(description = "Tổng số dòng dữ liệu đọc được từ file", example = "10")
    private int totalRows;

    @Schema(description = "Số lượng học sinh được thêm thành công vào lớp", example = "8")
    private int importedCount;

    @Schema(description = "Số dòng bị bỏ qua do trùng SĐT trong lớp hoặc sai định dạng", example = "2")
    private int skippedCount;

    @Builder.Default
    @Schema(description = "Danh sách học sinh vừa được import thành công")
    private List<StudentResponse> importedStudents = new ArrayList<>();

    @Builder.Default
    @Schema(description = "Danh sách chi tiết lỗi của các dòng bị bỏ qua")
    private List<String> rowErrors = new ArrayList<>();
}
