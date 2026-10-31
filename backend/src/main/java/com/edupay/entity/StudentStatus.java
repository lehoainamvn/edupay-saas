package com.edupay.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Trạng thái theo học của Học sinh:
 * - STUDYING: Đang theo học
 * - PAUSED: Tạm nghỉ bảo lưu
 * - DROPPED: Đã nghỉ học (Soft delete để giữ lịch sử điểm danh và hóa đơn)
 */
@Getter
@RequiredArgsConstructor
public enum StudentStatus {
    STUDYING("Đang học"),
    PAUSED("Tạm nghỉ"),
    DROPPED("Đã nghỉ");

    private final String label;
}
