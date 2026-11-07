package com.edupay.service;

import com.edupay.dto.request.UpdateSystemParameterRequest;
import com.edupay.dto.response.SystemParameterResponse;

import java.util.List;

public interface SystemParameterService {

    /**
     * Lấy danh sách tham số của tenant hiện tại, hỗ trợ lọc theo nhóm và tìm kiếm từ khóa.
     * Sử dụng In-memory Cache nếu đã nạp.
     */
    List<SystemParameterResponse> getAllParameters(String group, String keyword);

    /**
     * Lấy chi tiết tham số theo khóa paramKey.
     */
    SystemParameterResponse getParameterByKey(String paramKey);

    /**
     * Cập nhật giá trị tham số cấu hình và cập nhật In-memory Cache tức thì (Hot-reload).
     */
    SystemParameterResponse updateParameter(String paramKey, UpdateSystemParameterRequest request);

    /**
     * Tải lại toàn bộ In-memory Cache từ database cho tenant hiện tại.
     */
    void reloadCache();

    /**
     * Khôi phục toàn bộ tham số về giá trị chuẩn ban đầu của hệ thống.
     */
    void resetToDefaults();

    // ─── CÁC HÀM TRUY XUẤT NHANH (0ms) DÀNH CHO CÁC SERVICE KHÁC ───────────────

    String getStringValue(String paramKey, String defaultValue);

    int getIntValue(String paramKey, int defaultValue);

    boolean getBooleanValue(String paramKey, boolean defaultValue);
}

