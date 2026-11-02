package com.edupay.entity;

/**
 * Trạng thái điểm danh buổi học.
 * - PRESENT: Có mặt (Xanh lá - tính vào số buổi đi học thực tế để tính học phí).
 * - EXCUSED: Nghỉ có phép (Vàng - không tính học phí hoặc theo quy định lớp).
 * - ABSENT: Nghỉ không phép (Đỏ - vắng mặt không lý do).
 */
public enum AttendanceStatus {
    PRESENT,
    EXCUSED,
    ABSENT;

    public static AttendanceStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        if ("ABSENT_PERMITTED".equals(normalized)) {
            return EXCUSED;
        }
        return AttendanceStatus.valueOf(normalized);
    }
}

