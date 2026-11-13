package com.edupay.service.impl;

import com.edupay.config.TenantContext;
import com.edupay.dto.request.UpdateSystemParameterRequest;
import com.edupay.dto.response.SystemParameterResponse;
import com.edupay.entity.SystemParameter;
import com.edupay.exception.BusinessException;
import com.edupay.exception.ResourceNotFoundException;
import com.edupay.repository.SystemParameterRepository;
import com.edupay.service.AuditLogService;
import com.edupay.service.SystemParameterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemParameterServiceImpl implements SystemParameterService {

    private final SystemParameterRepository systemParameterRepository;
    private final AuditLogService auditLogService;

    // ─── In-memory Cache: TenantId -> (ParamKey -> SystemParameterResponse) ─────
    private final Map<UUID, ConcurrentHashMap<String, SystemParameterResponse>> tenantCache = new ConcurrentHashMap<>();

    // ─── Định nghĩa danh mục mẫu mặc định của toàn hệ thống ───────────────────
    private record DefaultParamSpec(String key, String group, String type, String defaultValue,
                                    String displayName, String description, boolean isSystem) {}

    private static final List<DefaultParamSpec> DEFAULT_SPECS = List.of(
            // SYSTEM
            new DefaultParamSpec("SYSTEM_CENTER_NAME", "SYSTEM", "STRING", "Trung tâm Dạy kèm EduPay",
                    "Tên trung tâm / Lớp học", "Tên hiển thị trên biên nhận học phí và cổng tra cứu công khai của phụ huynh", true),
            new DefaultParamSpec("SYSTEM_AUTO_LOGOUT_HOURS", "SYSTEM", "NUMBER", "8",
                    "Thời gian tự động đăng xuất (giờ)", "Thời lượng phiên làm việc của tài khoản trước khi cần đăng nhập lại (1 - 72 giờ)", true),
            new DefaultParamSpec("SYSTEM_ALLOW_TEACHER_EDIT_FEE", "SYSTEM", "BOOLEAN", "true",
                    "Cho phép giáo viên sửa mức học phí", "Cho phép tài khoản vai trò giáo viên điều chỉnh mức học phí buổi khi điểm danh", false),
            new DefaultParamSpec("DEFAULT_TRANSFER_PREFIX", "SYSTEM", "STRING", "EDUPAY",
                    "Tiền tố chuyển khoản VietQR", "Tiền tố mặc định của nội dung chuyển khoản khi tự động sinh mã QR học phí", true),
            new DefaultParamSpec("MAX_STUDENTS_PER_CLASS", "SYSTEM", "NUMBER", "50",
                    "Sĩ số tối đa mỗi lớp", "Sĩ số học sinh tối đa cho phép trong một lớp học", false),

            // TUITION
            new DefaultParamSpec("TUITION_DUE_DAYS", "TUITION", "NUMBER", "7",
                    "Hạn thanh toán học phí (ngày)", "Số ngày phụ huynh được phép thanh toán kể từ ngày phát hành phiếu thu", true),
            new DefaultParamSpec("TUITION_AUTO_CREATE_DAY", "TUITION", "NUMBER", "28",
                    "Ngày tự động chốt học phí trong tháng", "Ngày hệ thống tự động tổng hợp số buổi điểm danh và sinh phiếu thu hàng tháng (1 - 31)", false),
            new DefaultParamSpec("TUITION_OVERDUE_POLICY", "TUITION", "STRING", "WARN_ONLY",
                    "Chính sách xử lý khi quá hạn học phí", "Quy định hình thức xử lý khi học sinh chưa đóng học phí đúng hạn", false),

            // SECURITY
            new DefaultParamSpec("SECURITY_MAX_LOGIN_ATTEMPTS", "SECURITY", "NUMBER", "5",
                    "Số lần đăng nhập sai tối đa", "Số lần đăng nhập thất bại tối đa trước khi tài khoản bị tạm khóa (1 - 10 lần)", true),
            new DefaultParamSpec("SECURITY_LOCKOUT_MINUTES", "SECURITY", "NUMBER", "15",
                    "Thời gian tạm khóa tài khoản (phút)", "Thời gian tài khoản bị tạm khóa sau khi nhập sai mật khẩu quá số lần quy định", true),

            // NOTIFICATION
            new DefaultParamSpec("NOTIFICATION_AUTO_REMINDER", "NOTIFICATION", "BOOLEAN", "true",
                    "Tự động nhắc nợ trước ngày hết hạn", "Tự động kích hoạt tin nhắn nhắc nhở phụ huynh khi sắp đến hạn thanh toán", false),
            new DefaultParamSpec("NOTIFICATION_REMINDER_BEFORE_DAYS", "NOTIFICATION", "NUMBER", "2",
                    "Số ngày nhắc trước hạn thanh toán", "Số ngày hệ thống gửi thông báo nhắc nhở trước khi phiếu thu hết hạn", false),
            new DefaultParamSpec("NOTIFICATION_TEMPLATE_INVOICE", "NOTIFICATION", "STRING",
                    "Kính gửi Quý Phụ huynh, học phí tháng {month} của em {studentName} là {amount}đ. Vui lòng thanh toán qua link VietQR: {invoiceUrl}. Trân trọng cảm ơn!",
                    "Mẫu tin nhắn gửi phiếu thu học phí", "Nội dung tin nhắn tự động khi gửi thông báo phiếu thu qua Zalo hoặc SMS cho phụ huynh", false)
    );

    @Override
    @Transactional
    public List<SystemParameterResponse> getAllParameters(String group, String keyword) {
        UUID tenantId = TenantContext.getTenantId();
        ensureCacheLoaded(tenantId);

        ConcurrentHashMap<String, SystemParameterResponse> map = tenantCache.get(tenantId);
        if (map == null) {
            return Collections.emptyList();
        }

        return map.values().stream()
                .filter(p -> !StringUtils.hasText(group) || "ALL".equalsIgnoreCase(group) || p.getParamGroup().equalsIgnoreCase(group))
                .filter(p -> {
                    if (!StringUtils.hasText(keyword)) return true;
                    String kw = keyword.trim().toLowerCase();
                    return p.getParamKey().toLowerCase().contains(kw)
                            || p.getDisplayName().toLowerCase().contains(kw)
                            || (p.getDescription() != null && p.getDescription().toLowerCase().contains(kw));
                })
                .sorted(Comparator.comparing(SystemParameterResponse::getParamGroup)
                        .thenComparing(SystemParameterResponse::getParamKey))
                .collect(Collectors.toList());
    }

    @Override
    public SystemParameterResponse getParameterByKey(String paramKey) {
        UUID tenantId = TenantContext.getTenantId();
        ensureCacheLoaded(tenantId);

        ConcurrentHashMap<String, SystemParameterResponse> map = tenantCache.get(tenantId);
        if (map != null && map.containsKey(paramKey)) {
            return map.get(paramKey);
        }

        SystemParameter entity = systemParameterRepository.findByTenantIdAndParamKey(tenantId, paramKey)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tham số: " + paramKey));
        return toResponse(entity);
    }

    @Override
    @Transactional
    public SystemParameterResponse updateParameter(String paramKey, UpdateSystemParameterRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        SystemParameter entity = systemParameterRepository.findByTenantIdAndParamKey(tenantId, paramKey)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tham số: " + paramKey));

        String rawValue = request.getParamValue() != null ? request.getParamValue().trim() : "";
        validateValue(entity, rawValue);
        if ("BOOLEAN".equalsIgnoreCase(entity.getDataType())) {
            rawValue = rawValue.toLowerCase();
        }

        String oldValue = entity.getParamValue();
        entity.setParamValue(rawValue);

        if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
            entity.setDisplayName(request.getDisplayName().trim());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription().trim());
        }

        SystemParameter updated = systemParameterRepository.save(entity);

        SystemParameterResponse response = toResponse(updated);

        // ─── Hot-Reload In-Memory Cache tức thì (0ms) ─────────────────────────
        ConcurrentHashMap<String, SystemParameterResponse> map = tenantCache.computeIfAbsent(tenantId, k -> new ConcurrentHashMap<>());
        map.put(paramKey, response);
        log.info("Hot-reload tham số thành công: tenantId={}, key={}, oldVal={}, newVal={}",
                tenantId, paramKey, oldValue, rawValue);

        // ─── Ghi log kiểm toán (Audit Trail) ──────────────────────────────────
        UUID actorUserId = getUserIdFromContext();
        auditLogService.logAction(tenantId, actorUserId, "UPDATE_SYSTEM_PARAM", "SYSTEM_PARAMETER",
                paramKey, oldValue, rawValue, "INFO", null, null);

        return response;
    }

    @Override
    @Transactional
    public SystemParameterResponse createParameter(com.edupay.dto.request.CreateSystemParameterRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        String paramKey = request.getParamKey().trim().toUpperCase();

        if (systemParameterRepository.existsByTenantIdAndParamKey(tenantId, paramKey)) {
            throw new BusinessException("PARAM_ALREADY_EXISTS", "Mã tham số '" + paramKey + "' đã tồn tại trong hệ thống.");
        }

        String rawValue = request.getParamValue() != null ? request.getParamValue().trim() : "";
        String dataType = request.getDataType() != null ? request.getDataType().trim().toUpperCase() : "STRING";

        if ("NUMBER".equalsIgnoreCase(dataType)) {
            try {
                Long.parseLong(rawValue);
            } catch (NumberFormatException e) {
                throw new BusinessException("INVALID_PARAMETER_VALUE", "Giá trị tham số phải là số nguyên hợp lệ.");
            }
        } else if ("BOOLEAN".equalsIgnoreCase(dataType)) {
            if (!"true".equalsIgnoreCase(rawValue) && !"false".equalsIgnoreCase(rawValue)) {
                throw new BusinessException("INVALID_PARAMETER_VALUE", "Giá trị tham số chỉ nhận true hoặc false.");
            }
            rawValue = rawValue.toLowerCase();
        }

        SystemParameter param = SystemParameter.builder()
                .paramKey(paramKey)
                .paramValue(rawValue)
                .paramGroup(request.getParamGroup().trim().toUpperCase())
                .dataType(dataType)
                .displayName(request.getDisplayName().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .isSystem(false)
                .build();
        param.setTenantId(tenantId);

        SystemParameter saved = systemParameterRepository.save(param);
        SystemParameterResponse response = toResponse(saved);

        ConcurrentHashMap<String, SystemParameterResponse> map = tenantCache.computeIfAbsent(tenantId, k -> new ConcurrentHashMap<>());
        map.put(paramKey, response);

        log.info("Đã tạo tham số mới: tenantId={}, key={}, value={}", tenantId, paramKey, rawValue);
        UUID actorUserId = getUserIdFromContext();
        auditLogService.logAction(tenantId, actorUserId, "CREATE_SYSTEM_PARAM", "SYSTEM_PARAMETER",
                paramKey, null, rawValue, "INFO", null, null);

        return response;
    }

    @Override
    @Transactional
    public void deleteParameter(String paramKey) {
        UUID tenantId = TenantContext.getTenantId();
        SystemParameter param = systemParameterRepository.findByTenantIdAndParamKey(tenantId, paramKey)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tham số: " + paramKey));

        if (Boolean.TRUE.equals(param.getIsSystem())) {
            throw new BusinessException("CANNOT_DELETE_SYSTEM_PARAM",
                    "Tham số '" + paramKey + "' là tham số cốt lõi của hệ thống, không được phép xóa.");
        }

        systemParameterRepository.deleteByTenantIdAndParamKey(tenantId, paramKey);

        ConcurrentHashMap<String, SystemParameterResponse> map = tenantCache.get(tenantId);
        if (map != null) {
            map.remove(paramKey);
        }

        log.info("Đã xóa tham số: tenantId={}, key={}", tenantId, paramKey);
        UUID actorUserId = getUserIdFromContext();
        auditLogService.logAction(tenantId, actorUserId, "DELETE_SYSTEM_PARAM", "SYSTEM_PARAMETER",
                paramKey, param.getParamValue(), null, "INFO", null, null);
    }

    @Override
    public void reloadCache() {
        UUID tenantId = TenantContext.getTenantId();
        tenantCache.remove(tenantId);
        ensureCacheLoaded(tenantId);
        log.info("Đã làm mới toàn bộ In-memory Cache cho tenantId={}", tenantId);
    }

    @Override
    @Transactional
    public void resetToDefaults() {
        UUID tenantId = TenantContext.getTenantId();
        for (DefaultParamSpec spec : DEFAULT_SPECS) {
            SystemParameter param = systemParameterRepository.findByTenantIdAndParamKey(tenantId, spec.key())
                    .orElse(null);
            if (param != null) {
                param.setParamValue(spec.defaultValue());
                systemParameterRepository.save(param);
            } else {
                SystemParameter newParam = SystemParameter.builder()
                        .paramKey(spec.key())
                        .paramGroup(spec.group())
                        .dataType(spec.type())
                        .paramValue(spec.defaultValue())
                        .displayName(spec.displayName())
                        .description(spec.description())
                        .isSystem(spec.isSystem())
                        .build();
                newParam.setTenantId(tenantId);
                systemParameterRepository.save(newParam);
            }
        }
        reloadCache();
        log.info("Đã khôi phục bộ tham số cấu hình về mặc định cho tenantId={}", tenantId);
    }

    // ─── HÀM TRUY CẬP NHANH (0ms) DÀNH CHO CÁC SERVICE KHÁC ────────────────────

    @Override
    public String getStringValue(String paramKey, String defaultValue) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) return defaultValue;
        ensureCacheLoaded(tenantId);
        ConcurrentHashMap<String, SystemParameterResponse> map = tenantCache.get(tenantId);
        if (map != null && map.containsKey(paramKey)) {
            return map.get(paramKey).getParamValue();
        }
        return defaultValue;
    }

    @Override
    public int getIntValue(String paramKey, int defaultValue) {
        String val = getStringValue(paramKey, null);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            log.warn("Không thể chuyển đổi tham số {}='{}' sang kiểu int, dùng default={}", paramKey, val, defaultValue);
            return defaultValue;
        }
    }

    @Override
    public boolean getBooleanValue(String paramKey, boolean defaultValue) {
        String val = getStringValue(paramKey, null);
        if (val == null) return defaultValue;
        return "true".equalsIgnoreCase(val.trim()) || "1".equals(val.trim());
    }

    // ─── INTERNAL HELPERS ─────────────────────────────────────────────────────

    private void ensureCacheLoaded(UUID tenantId) {
        if (tenantId == null) return;
        if (!tenantCache.containsKey(tenantId)) {
            synchronized (this) {
                if (!tenantCache.containsKey(tenantId)) {
                    List<SystemParameter> list = systemParameterRepository.findAllByTenantIdOrderByParamGroupAscParamKeyAsc(tenantId);
                    if (list.isEmpty()) {
                        // Tự động seed cho tenant mới
                        list = seedDefaultsForTenant(tenantId);
                    }
                    ConcurrentHashMap<String, SystemParameterResponse> map = new ConcurrentHashMap<>();
                    for (SystemParameter p : list) {
                        map.put(p.getParamKey(), toResponse(p));
                    }
                    tenantCache.put(tenantId, map);
                    log.info("Nạp {} tham số vào In-memory Cache cho tenantId={}", map.size(), tenantId);
                }
            }
        }
    }

    private List<SystemParameter> seedDefaultsForTenant(UUID tenantId) {
        List<SystemParameter> toSave = new ArrayList<>();
        for (DefaultParamSpec spec : DEFAULT_SPECS) {
            SystemParameter p = SystemParameter.builder()
                    .paramKey(spec.key())
                    .paramGroup(spec.group())
                    .dataType(spec.type())
                    .paramValue(spec.defaultValue())
                    .displayName(spec.displayName())
                    .description(spec.description())
                    .isSystem(spec.isSystem())
                    .build();
            p.setTenantId(tenantId);
            toSave.add(p);
        }
        List<SystemParameter> saved = systemParameterRepository.saveAll(toSave);
        log.info("Đã tự động khởi tạo {} tham số cấu hình mặc định cho tenant mới: {}", saved.size(), tenantId);
        return saved;
    }

    private void validateValue(SystemParameter entity, String rawValue) {
        if (rawValue.isEmpty()) {
            throw new BusinessException("INVALID_PARAMETER_VALUE", "Giá trị tham số không được để trống");
        }

        String type = entity.getDataType();
        String key = entity.getParamKey();

        if ("NUMBER".equalsIgnoreCase(type)) {
            long intVal;
            try {
                intVal = Long.parseLong(rawValue);
            } catch (NumberFormatException e) {
                throw new BusinessException("INVALID_PARAMETER_VALUE",
                        "Giá trị tham số '" + entity.getDisplayName() + "' phải là số nguyên hợp lệ.");
            }

            // Kiểm tra ràng buộc hợp lý từng tham số cụ thể
            if ("SYSTEM_AUTO_LOGOUT_HOURS".equalsIgnoreCase(key) && (intVal < 1 || intVal > 72)) {
                throw new BusinessException("INVALID_PARAMETER_VALUE", "Thời gian tự động đăng xuất phải từ 1 đến 72 giờ.");
            }
            if ("TUITION_DUE_DAYS".equalsIgnoreCase(key) && (intVal < 1 || intVal > 90)) {
                throw new BusinessException("INVALID_PARAMETER_VALUE", "Hạn đóng học phí phải từ 1 đến 90 ngày.");
            }
            if ("TUITION_AUTO_CREATE_DAY".equalsIgnoreCase(key) && (intVal < 1 || intVal > 31)) {
                throw new BusinessException("INVALID_PARAMETER_VALUE", "Ngày chốt học phí phải từ ngày 1 đến 31 trong tháng.");
            }
            if ("NOTIFICATION_REMINDER_BEFORE_DAYS".equalsIgnoreCase(key) && (intVal < 1 || intVal > 10)) {
                throw new BusinessException("INVALID_PARAMETER_VALUE", "Số ngày nhắc trước hạn phải từ 1 đến 10 ngày.");
            }
        } else if ("BOOLEAN".equalsIgnoreCase(type)) {
            if (!"true".equalsIgnoreCase(rawValue) && !"false".equalsIgnoreCase(rawValue)) {
                throw new BusinessException("INVALID_PARAMETER_VALUE",
                        "Giá trị tham số '" + entity.getDisplayName() + "' chỉ nhận true hoặc false.");
            }
        }
    }

    private SystemParameterResponse toResponse(SystemParameter p) {
        return SystemParameterResponse.builder()
                .id(p.getId())
                .paramKey(p.getParamKey())
                .paramValue(p.getParamValue())
                .paramGroup(p.getParamGroup())
                .dataType(p.getDataType())
                .displayName(p.getDisplayName())
                .description(p.getDescription())
                .isSystem(p.getIsSystem())
                .updatedAt(p.getUpdatedAt() != null ? p.getUpdatedAt() : p.getCreatedAt())
                .build();
    }

    private UUID getUserIdFromContext() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() != null) {
                return UUID.fromString(auth.getPrincipal().toString());
            }
        } catch (Exception ignored) {}
        return null;
    }
}

