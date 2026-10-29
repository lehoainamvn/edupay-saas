package com.edupay.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Hình thức thu học phí của lớp học:
 * - PER_LESSON: Thu theo số buổi thực học trong tháng
 * - MONTHLY_PACKAGE: Thu trọn gói cố định theo tháng
 */
@Getter
@RequiredArgsConstructor
public enum FeeType {
    PER_LESSON("Theo buổi"),
    MONTHLY_PACKAGE("Trọn tháng");

    private final String label;
}
