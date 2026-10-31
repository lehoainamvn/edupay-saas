package com.edupay.service.impl;

import com.edupay.config.TenantContext;
import com.edupay.dto.request.StudentRequest;
import com.edupay.dto.response.StudentImportResultResponse;
import com.edupay.dto.response.StudentResponse;
import com.edupay.entity.ClassEntity;
import com.edupay.entity.Student;
import com.edupay.entity.StudentStatus;
import com.edupay.exception.BusinessException;
import com.edupay.exception.DuplicateResourceException;
import com.edupay.exception.ResourceNotFoundException;
import com.edupay.mapper.StudentMapper;
import com.edupay.repository.ClassRepository;
import com.edupay.repository.StudentRepository;
import com.edupay.service.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Triển khai nghiệp vụ Quản lý Học sinh & Import hàng loạt Excel/CSV bằng Apache POI.
 * Tuân thủ nghiêm ngặt luật Multi-Tenant P0:
 * - Luôn lấy tenantId từ TenantContext.getTenantId()
 * - Validate SĐT phụ huynh chuẩn di động Việt Nam (10 số)
 * - Kiểm tra trùng học sinh / SĐT phụ huynh trong cùng 1 lớp học
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StudentServiceImpl implements StudentService {

    private static final Pattern VN_PHONE_PATTERN = Pattern.compile("^0(3|5|7|8|9)[0-9]{8}$");

    private final StudentRepository studentRepository;
    private final ClassRepository classRepository;
    private final StudentMapper studentMapper;

    @Override
    @Transactional(readOnly = true)
    public List<StudentResponse> getStudents(UUID classId, StudentStatus status, String search) {
        UUID tenantId = requireTenantId();

        List<Student> students = (classId != null)
                ? studentRepository.findAllByTenantIdAndClassIdOrderByCreatedAtDesc(tenantId, classId)
                : studentRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId);

        Map<UUID, ClassEntity> classMap = classRepository.findAllByTenantIdOrderByIsActiveDescCreatedAtDesc(tenantId)
                .stream()
                .collect(Collectors.toMap(ClassEntity::getId, Function.identity(), (a, b) -> a));

        String normalizedSearch = normalizeSearchText(search);

        return students.stream()
                .filter(s -> status == null || s.getStatus() == status)
                .filter(s -> matchesSearch(s, normalizedSearch))
                .map(s -> enrichStudentResponse(s, classMap.get(s.getClassId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public StudentResponse getStudentById(UUID id) {
        UUID tenantId = requireTenantId();
        Student student = findStudentByIdAndTenant(id, tenantId);
        ClassEntity classEntity = classRepository.findByIdAndTenantId(student.getClassId(), tenantId).orElse(null);
        return enrichStudentResponse(student, classEntity);
    }

    @Override
    @Transactional
    public StudentResponse createStudent(StudentRequest request) {
        UUID tenantId = requireTenantId();
        ClassEntity classEntity = verifyClassOwnership(request.getClassId(), tenantId);

        String fullName = request.getFullName().trim();
        String nickname = trimOrNull(request.getNickname());
        String parentName = trimOrNull(request.getParentName());
        String normalizedPhone = normalizePhone(request.getParentPhone());

        validateVietnamesePhone(normalizedPhone);
        checkDuplicateInClassOnCreate(tenantId, classEntity.getId(), fullName, nickname, normalizedPhone);

        Student student = studentMapper.toEntity(request);
        student.setTenantId(tenantId);
        student.setClassId(classEntity.getId());
        student.setFullName(fullName);
        student.setNickname(nickname);
        student.setParentName(parentName);
        student.setParentPhone(normalizedPhone);
        if (student.getStatus() == null) {
            student.setStatus(StudentStatus.STUDYING);
        }

        Student saved = studentRepository.save(student);
        log.info("Tenant [{}] created student [{}] in class [{}]", tenantId, saved.getFullName(), classEntity.getName());

        return enrichStudentResponse(saved, classEntity);
    }

    @Override
    @Transactional
    public StudentResponse updateStudent(UUID id, StudentRequest request) {
        UUID tenantId = requireTenantId();
        Student existing = findStudentByIdAndTenant(id, tenantId);
        ClassEntity targetClass = verifyClassOwnership(request.getClassId(), tenantId);

        String fullName = request.getFullName().trim();
        String nickname = trimOrNull(request.getNickname());
        String parentName = trimOrNull(request.getParentName());
        String normalizedPhone = normalizePhone(request.getParentPhone());

        validateVietnamesePhone(normalizedPhone);

        if (studentRepository.existsByTenantIdAndClassIdAndParentPhoneAndIdNotAndStatusNot(
                tenantId, targetClass.getId(), normalizedPhone, id, StudentStatus.DROPPED)) {
            throw new DuplicateResourceException(
                    "Số điện thoại phụ huynh '" + normalizedPhone + "' đã tồn tại trong lớp " + targetClass.getName());
        }

        studentMapper.updateEntityFromRequest(request, existing);
        existing.setClassId(targetClass.getId());
        existing.setFullName(fullName);
        existing.setNickname(nickname);
        existing.setParentName(parentName);
        existing.setParentPhone(normalizedPhone);
        existing.setCustomFee(request.getCustomFee());
        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }

        Student updated = studentRepository.save(existing);
        log.info("Tenant [{}] updated student id [{}]", tenantId, updated.getId());

        return enrichStudentResponse(updated, targetClass);
    }

    @Override
    @Transactional
    public void softDeleteStudent(UUID id) {
        UUID tenantId = requireTenantId();
        Student student = findStudentByIdAndTenant(id, tenantId);
        student.setStatus(StudentStatus.DROPPED);
        studentRepository.save(student);
        log.info("Tenant [{}] soft-deleted student id [{}] (status = DROPPED)", tenantId, id);
    }

    @Override
    @Transactional
    public StudentResponse updateStudentStatus(UUID id, StudentStatus status) {
        UUID tenantId = requireTenantId();
        Student student = findStudentByIdAndTenant(id, tenantId);
        student.setStatus(status);
        Student saved = studentRepository.save(student);
        ClassEntity classEntity = classRepository.findByIdAndTenantId(saved.getClassId(), tenantId).orElse(null);
        return enrichStudentResponse(saved, classEntity);
    }

    @Override
    @Transactional
    public StudentImportResultResponse importStudentsFromFile(UUID classId, MultipartFile file) {
        UUID tenantId = requireTenantId();
        ClassEntity classEntity = verifyClassOwnership(classId, tenantId);

        if (file == null || file.isEmpty()) {
            throw new BusinessException("EMPTY_IMPORT_FILE", "File import không được để trống");
        }

        String originalFilename = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase();
        List<RawImportRow> rawRows;

        try {
            if (originalFilename.endsWith(".csv")) {
                rawRows = parseCsvRows(file);
            } else if (originalFilename.endsWith(".xlsx") || originalFilename.endsWith(".xls")) {
                rawRows = parseExcelRows(file);
            } else {
                throw new BusinessException("UNSUPPORTED_FILE_FORMAT",
                        "Chỉ hỗ trợ định dạng file Excel (.xlsx, .xls) hoặc CSV (.csv)");
            }
        } catch (IOException ex) {
            log.error("Failed to parse student import file for tenant [{}]: ", tenantId, ex);
            throw new BusinessException("FILE_PARSE_ERROR", "Không thể đọc nội dung file. Vui lòng kiểm tra lại định dạng file mẫu.");
        }

        List<StudentResponse> importedList = new ArrayList<>();
        List<String> rowErrors = new ArrayList<>();
        Set<String> seenPhonesInBatch = new HashSet<>();

        for (RawImportRow row : rawRows) {
            String fullName = row.fullName() != null ? row.fullName().trim() : "";
            String nickname = trimOrNull(row.nickname());
            String parentName = trimOrNull(row.parentName());
            String phone = normalizePhone(row.parentPhone());

            if (fullName.isEmpty() && phone.isEmpty()) {
                continue; // Bỏ qua dòng trống
            }

            if (fullName.length() < 2) {
                rowErrors.add("Dòng " + row.rowNumber() + ": Họ tên học sinh bị trống hoặc quá ngắn");
                continue;
            }

            if (!VN_PHONE_PATTERN.matcher(phone).matches()) {
                rowErrors.add("Dòng " + row.rowNumber() + " (" + fullName + "): SĐT phụ huynh '" + row.parentPhone()
                        + "' không hợp lệ (cần 10 số di động VN, VD: 0912345678)");
                continue;
            }

            if (!seenPhonesInBatch.add(phone)) {
                rowErrors.add("Dòng " + row.rowNumber() + " (" + fullName + "): Trùng SĐT phụ huynh '" + phone + "' ngay trong file import");
                continue;
            }

            if (studentRepository.existsByTenantIdAndClassIdAndParentPhoneAndStatusNot(
                    tenantId, classId, phone, StudentStatus.DROPPED)) {
                rowErrors.add("Dòng " + row.rowNumber() + " (" + fullName + "): SĐT phụ huynh '" + phone
                        + "' đã tồn tại trong lớp " + classEntity.getName());
                continue;
            }

            BigDecimal customFee = null;
            if (row.customFeeText() != null && !row.customFeeText().isBlank()) {
                try {
                    String digitsOnly = row.customFeeText().replaceAll("[^0-9.]", "");
                    if (!digitsOnly.isEmpty()) {
                        BigDecimal parsed = new BigDecimal(digitsOnly);
                        if (parsed.compareTo(BigDecimal.ZERO) > 0) {
                            customFee = parsed;
                        }
                    }
                } catch (NumberFormatException ignored) {
                    rowErrors.add("Dòng " + row.rowNumber() + " (" + fullName + "): Cột Học phí riêng '" + row.customFeeText() + "' không phải là số hợp lệ");
                    continue;
                }
            }

            Student student = Student.builder()
                    .classId(classId)
                    .fullName(fullName)
                    .nickname(nickname)
                    .parentName(parentName)
                    .parentPhone(phone)
                    .customFee(customFee)
                    .status(StudentStatus.STUDYING)
                    .build();
            student.setTenantId(tenantId);

            Student saved = studentRepository.save(student);
            importedList.add(enrichStudentResponse(saved, classEntity));
        }

        int totalProcessed = importedList.size() + rowErrors.size();
        log.info("Tenant [{}] imported {}/{} students into class [{}]",
                tenantId, importedList.size(), totalProcessed, classEntity.getName());

        return StudentImportResultResponse.builder()
                .totalRows(totalProcessed)
                .importedCount(importedList.size())
                .skippedCount(rowErrors.size())
                .importedStudents(importedList)
                .rowErrors(rowErrors)
                .build();
    }

    @Override
    public byte[] generateImportTemplateExcel() {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Danh_Sach_Hoc_Sinh");

            // Style cho dòng Header
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            String[] headers = {
                    "Họ và tên học sinh (*)",
                    "Biệt danh ở nhà (Tên gọi)",
                    "Họ tên phụ huynh",
                    "SĐT phụ huynh (*)",
                    "Học phí riêng (VNĐ - Để trống nếu theo lớp)"
            };

            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(24);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 7500);
            }

            // 3 dòng dữ liệu mẫu chuẩn thực tế
            String[][] sampleRows = {
                    {"Nguyễn Hoàng Nam", "Bé Bờm", "Nguyễn Văn Hải", "0912345678", ""},
                    {"Trần Khánh Linh", "Bé Sóc", "Trần Thị Thu", "0987654321", "130000"},
                    {"Phạm Gia Bảo", "Bi", "Phạm Quốc Khánh", "0905123456", ""}
            };

            for (int r = 0; r < sampleRows.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < sampleRows[r].length; c++) {
                    row.createCell(c).setCellValue(sampleRows[r][c]);
                }
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            log.error("Failed to generate Excel template: ", ex);
            throw new BusinessException("TEMPLATE_GENERATION_ERROR", "Không thể khởi tạo file Excel mẫu");
        }
    }

    private List<RawImportRow> parseExcelRows(MultipartFile file) throws IOException {
        List<RawImportRow> rows = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int lastRowNum = sheet.getLastRowNum();

            // Dòng 0 là tiêu đề cột, bắt đầu đọc từ dòng 1
            for (int i = 1; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String fullName = formatter.formatCellValue(row.getCell(0)).trim();
                String nickname = formatter.formatCellValue(row.getCell(1)).trim();
                String parentName = formatter.formatCellValue(row.getCell(2)).trim();
                String parentPhone = formatter.formatCellValue(row.getCell(3)).trim();
                String customFeeText = formatter.formatCellValue(row.getCell(4)).trim();

                if (fullName.isEmpty() && parentPhone.isEmpty()) {
                    continue;
                }
                rows.add(new RawImportRow(i + 1, fullName, nickname, parentName, parentPhone, customFeeText));
            }
        }
        return rows;
    }

    private List<RawImportRow> parseCsvRows(MultipartFile file) throws IOException {
        List<RawImportRow> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String cleaned = line.replace("\uFEFF", "").trim();
                if (cleaned.isEmpty()) continue;

                // Bỏ qua dòng tiêu đề đầu tiên
                if (lineNumber == 1 && (cleaned.toLowerCase().contains("họ") || cleaned.toLowerCase().contains("name") || cleaned.toLowerCase().contains("sđt"))) {
                    continue;
                }

                String[] cols = cleaned.split("[,;\\t]", -1);
                String fullName = cols.length > 0 ? stripQuotes(cols[0]) : "";
                String nickname = cols.length > 1 ? stripQuotes(cols[1]) : "";
                String parentName = cols.length > 2 ? stripQuotes(cols[2]) : "";
                String parentPhone = cols.length > 3 ? stripQuotes(cols[3]) : "";
                String customFeeText = cols.length > 4 ? stripQuotes(cols[4]) : "";

                rows.add(new RawImportRow(lineNumber, fullName, nickname, parentName, parentPhone, customFeeText));
            }
        }
        return rows;
    }

    private String stripQuotes(String value) {
        if (value == null) return "";
        return value.trim().replaceAll("^\"|\"$", "").trim();
    }

    private void checkDuplicateInClassOnCreate(
            UUID tenantId, UUID classId, String fullName, String nickname, String normalizedPhone) {
        if (studentRepository.existsByTenantIdAndClassIdAndParentPhoneAndStatusNot(
                tenantId, classId, normalizedPhone, StudentStatus.DROPPED)) {
            throw new DuplicateResourceException(
                    "Số điện thoại phụ huynh '" + normalizedPhone + "' đã tồn tại trong lớp học này");
        }
        if (studentRepository.existsDuplicateNameInClass(tenantId, classId, fullName, nickname)) {
            throw new DuplicateResourceException(
                    "Học sinh '" + fullName + "' đã có trong danh sách lớp này");
        }
    }

    private void validateVietnamesePhone(String normalizedPhone) {
        if (!VN_PHONE_PATTERN.matcher(normalizedPhone).matches()) {
            throw new BusinessException(
                    "INVALID_PHONE_NUMBER",
                    "Số điện thoại phụ huynh '" + normalizedPhone + "' không hợp lệ. Vui lòng nhập 10 chữ số di động Việt Nam (VD: 0912345678)."
            );
        }
    }

    public static String normalizePhone(String rawPhone) {
        if (rawPhone == null) return "";
        String cleaned = rawPhone.replaceAll("[\\s.\\-()]", "");
        if (cleaned.startsWith("+84")) {
            cleaned = "0" + cleaned.substring(3);
        } else if (cleaned.startsWith("84") && cleaned.length() == 11) {
            cleaned = "0" + cleaned.substring(2);
        } else if (cleaned.length() == 9 && !cleaned.startsWith("0")) {
            // Trường hợp Excel tự cắt số 0 ở đầu (VD: 912345678 -> 0912345678)
            cleaned = "0" + cleaned;
        }
        return cleaned;
    }

    private ClassEntity verifyClassOwnership(UUID classId, UUID tenantId) {
        return classRepository.findByIdAndTenantId(classId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học được chọn trong tài khoản của bạn"));
    }

    private Student findStudentByIdAndTenant(UUID id, UUID tenantId) {
        return studentRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin học sinh trong lớp của bạn"));
    }

    private StudentResponse enrichStudentResponse(Student student, ClassEntity classEntity) {
        StudentResponse response = studentMapper.toResponse(student);
        if (classEntity != null) {
            response.setClassName(classEntity.getName());
            response.setClassFeeType(classEntity.getFeeType());
            response.setClassDefaultFee(classEntity.getDefaultFee());
            response.setEffectiveFee(
                    student.getCustomFee() != null ? student.getCustomFee() : classEntity.getDefaultFee()
            );
        } else {
            response.setEffectiveFee(student.getCustomFee());
        }
        return response;
    }

    private boolean matchesSearch(Student s, String normalizedQuery) {
        if (normalizedQuery.isEmpty()) return true;
        String combined = normalizeSearchText(
                s.getFullName() + " " +
                Optional.ofNullable(s.getNickname()).orElse("") + " " +
                Optional.ofNullable(s.getParentName()).orElse("") + " " +
                Optional.ofNullable(s.getParentPhone()).orElse("")
        );
        return combined.contains(normalizedQuery);
    }

    private String normalizeSearchText(String input) {
        if (input == null || input.isBlank()) return "";
        String nfd = Normalizer.normalize(input.trim().toLowerCase(), Normalizer.Form.NFD);
        return nfd.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd');
    }

    private String trimOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private UUID requireTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("TENANT_CONTEXT_MISSING", "Không xác định được thông tin giáo viên (Tenant Context)");
        }
        return tenantId;
    }

    private record RawImportRow(
            int rowNumber,
            String fullName,
            String nickname,
            String parentName,
            String parentPhone,
            String customFeeText
    ) {}
}
