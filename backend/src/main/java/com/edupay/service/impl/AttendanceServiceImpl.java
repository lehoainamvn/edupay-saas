package com.edupay.service.impl;

import com.edupay.config.TenantContext;
import com.edupay.dto.request.AttendanceBatchUpsertRequest;
import com.edupay.dto.request.AttendanceItemRequest;
import com.edupay.dto.request.AttendanceUpdateRequest;
import com.edupay.dto.response.*;
import com.edupay.entity.*;
import com.edupay.exception.BusinessException;
import com.edupay.exception.ResourceNotFoundException;
import com.edupay.repository.AttendanceRepository;
import com.edupay.repository.ClassRepository;
import com.edupay.repository.InvoiceRepository;
import com.edupay.repository.StudentRepository;
import com.edupay.security.SecuritySanitizer;
import com.edupay.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Triển khai dịch vụ Quản lý Điểm danh và Thống kê Buổi học (UC-13, UC-14, UC-15).
 * BẮT BUỘC: 100% logic nghiệp vụ được thực thi tại đây, cô lập dữ liệu theo TenantContext (P0).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceServiceImpl implements AttendanceService {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final Pattern MONTH_PATTERN = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");
    private static final Set<InvoiceStatus> ACTIVE_INVOICE_STATUSES = Set.of(InvoiceStatus.PENDING, InvoiceStatus.PAID);

    private final AttendanceRepository attendanceRepository;
    private final ClassRepository classRepository;
    private final StudentRepository studentRepository;
    private final InvoiceRepository invoiceRepository;

    @Override
    public DailyAttendanceResponse getDailyAttendance(UUID classId, LocalDate date) {
        UUID tenantId = TenantContext.getTenantId();
        LocalDate targetDate = (date != null) ? date : LocalDate.now();

        ClassEntity classEntity = findClassOrThrow(classId, tenantId);
        String billingMonth = targetDate.format(MONTH_FORMATTER);

        // Lấy danh sách ID học sinh đã bị khóa do đã xuất phiếu thu trong tháng
        Set<UUID> lockedStudentIds = invoiceRepository.findLockedStudentIdsByClassAndMonth(
                tenantId, classId, billingMonth, ACTIVE_INVOICE_STATUSES);

        // Lấy danh sách học sinh đang học trong lớp
        List<Student> students = studentRepository.findAllByTenantIdAndClassIdAndStatusOrderByFullNameAsc(
                tenantId, classId, StudentStatus.STUDYING);

        // Lấy các bản ghi điểm danh hiện có của ngày hôm đó
        List<Attendance> attendances = attendanceRepository.findAllByTenantIdAndClassIdAndLessonDate(
                tenantId, classId, targetDate);

        Map<UUID, Attendance> attendanceMap = attendances.stream()
                .collect(Collectors.toMap(Attendance::getStudentId, Function.identity(), (a, b) -> a));

        // Nếu có học sinh đã điểm danh trong ngày nhưng không còn trạng thái STUDYING (ví dụ PAUSED), vẫn nạp vào
        Set<UUID> existingStudentIds = students.stream().map(Student::getId).collect(Collectors.toSet());
        List<UUID> missingStudentIds = attendances.stream()
                .map(Attendance::getStudentId)
                .filter(sid -> !existingStudentIds.contains(sid))
                .toList();

        if (!missingStudentIds.isEmpty()) {
            List<Student> extraStudents = studentRepository.findAllByTenantIdAndIdIn(tenantId, missingStudentIds);
            students = new ArrayList<>(students);
            students.addAll(extraStudents);
            students.sort(Comparator.comparing(Student::getFullName, String.CASE_INSENSITIVE_ORDER));
        }

        // Tạo danh sách kết quả DailyAttendanceItemResponse
        List<DailyAttendanceItemResponse> studentItems = new ArrayList<>();
        int markedCount = 0;
        int presentCount = 0;
        int excusedCount = 0;
        int absentCount = 0;

        for (Student student : students) {
            Attendance attendance = attendanceMap.get(student.getId());
            boolean isLocked = lockedStudentIds.contains(student.getId());

            DailyAttendanceItemResponse.DailyAttendanceItemResponseBuilder builder = DailyAttendanceItemResponse.builder()
                    .studentId(student.getId())
                    .fullName(student.getFullName())
                    .nickname(student.getNickname())
                    .parentPhone(student.getParentPhone())
                    .isLocked(isLocked);

            if (attendance != null) {
                builder.attendanceId(attendance.getId())
                        .status(attendance.getStatus())
                        .note(attendance.getNote());

                markedCount++;
                if (attendance.getStatus() == AttendanceStatus.PRESENT) {
                    presentCount++;
                } else if (attendance.getStatus() == AttendanceStatus.EXCUSED) {
                    excusedCount++;
                } else if (attendance.getStatus() == AttendanceStatus.ABSENT) {
                    absentCount++;
                }
            } else {
                builder.attendanceId(null)
                        .status(null)
                        .note(null);
            }

            studentItems.add(builder.build());
        }

        boolean isClassLocked = !studentItems.isEmpty() && studentItems.stream().allMatch(DailyAttendanceItemResponse::isLocked);

        return DailyAttendanceResponse.builder()
                .classId(classEntity.getId())
                .className(classEntity.getName())
                .lessonDate(targetDate)
                .totalStudents(students.size())
                .markedCount(markedCount)
                .presentCount(presentCount)
                .excusedCount(excusedCount)
                .absentCount(absentCount)
                .isClassLocked(isClassLocked)
                .students(studentItems)
                .build();
    }

    @Override
    @Transactional
    public DailyAttendanceResponse batchUpsertAttendance(AttendanceBatchUpsertRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        ClassEntity classEntity = findClassOrThrow(request.getClassId(), tenantId);

        LocalDate lessonDate = request.getLessonDate();
        String billingMonth = lessonDate.format(MONTH_FORMATTER);

        // Lấy danh sách ID học sinh trong payload
        Set<UUID> studentIdsInPayload = request.getItems().stream()
                .map(AttendanceItemRequest::getStudentId)
                .collect(Collectors.toSet());

        // Kiểm tra chống trùng lặp học sinh trong cùng 1 request
        if (request.getItems().size() != studentIdsInPayload.size()) {
            throw new BusinessException(
                    "DUPLICATE_STUDENT_IN_PAYLOAD",
                    "Danh sách điểm danh chứa học sinh bị trùng lặp trong cùng một lượt lưu."
            );
        }

        // Xác thực Multi-Tenant & Class Ownership: Tất cả học sinh phải thuộc lớp này và thuộc quyền quản lý của Tenant
        List<Student> classStudents = studentRepository.findAllByTenantIdAndClassIdOrderByFullNameAsc(tenantId, request.getClassId());
        Set<UUID> validClassStudentIds = classStudents.stream().map(Student::getId).collect(Collectors.toSet());

        for (AttendanceItemRequest item : request.getItems()) {
            if (!validClassStudentIds.contains(item.getStudentId())) {
                throw new BusinessException(
                        "STUDENT_NOT_IN_CLASS",
                        String.format("Học sinh ID '%s' không thuộc lớp học này hoặc không thuộc quyền quản lý của bạn.", item.getStudentId())
                );
            }
        }

        // Kiểm tra xem có học sinh nào đang bị khóa bởi phiếu thu trong tháng không
        Set<UUID> lockedStudentIds = invoiceRepository.findLockedStudentIds(
                tenantId, studentIdsInPayload, billingMonth, ACTIVE_INVOICE_STATUSES);

        // Lấy các bản ghi điểm danh hiện có trong DB của ngày đó
        Map<UUID, Attendance> existingAttendanceMap = attendanceRepository
                .findAllByTenantIdAndClassIdAndLessonDate(tenantId, request.getClassId(), lessonDate)
                .stream()
                .collect(Collectors.toMap(Attendance::getStudentId, Function.identity(), (a, b) -> a));

        // Kiểm tra vi phạm khóa (Locking Rule): Nếu học sinh bị khóa mà trạng thái hoặc ghi chú bị thay đổi -> Chặn
        if (!lockedStudentIds.isEmpty()) {
            Map<UUID, Student> studentMap = classStudents.stream()
                    .filter(s -> lockedStudentIds.contains(s.getId()))
                    .collect(Collectors.toMap(Student::getId, Function.identity()));

            for (AttendanceItemRequest item : request.getItems()) {
                if (lockedStudentIds.contains(item.getStudentId())) {
                    String sanitizedNote = SecuritySanitizer.sanitizeNote(item.getNote());
                    Attendance existing = existingAttendanceMap.get(item.getStudentId());
                    boolean isDifferent = (existing == null)
                            || existing.getStatus() != item.getStatus()
                            || !Objects.equals(existing.getNote(), sanitizedNote);

                    if (isDifferent) {
                        Student student = studentMap.get(item.getStudentId());
                        String studentName = (student != null) ? student.getFullName() : item.getStudentId().toString();
                        throw new BusinessException(
                                "ATTENDANCE_LOCKED",
                                String.format("Không thể sửa điểm danh của học sinh '%s' vì kỳ học %s đã xuất phiếu thu học phí. Vui lòng hủy phiếu thu trước nếu muốn thay đổi.",
                                        studentName, billingMonth)
                        );
                    }
                }
            }
        }

        // Thực hiện Batch Upsert kèm làm sạch (sanitize) ghi chú chống XSS
        List<Attendance> toSave = new ArrayList<>();
        for (AttendanceItemRequest item : request.getItems()) {
            String sanitizedNote = SecuritySanitizer.sanitizeNote(item.getNote());
            Attendance record = existingAttendanceMap.get(item.getStudentId());
            if (record != null) {
                record.setStatus(item.getStatus());
                record.setNote(sanitizedNote);
                toSave.add(record);
            } else {
                Attendance newAttendance = Attendance.builder()
                        .classId(request.getClassId())
                        .studentId(item.getStudentId())
                        .lessonDate(lessonDate)
                        .status(item.getStatus())
                        .note(sanitizedNote)
                        .build();
                newAttendance.setTenantId(tenantId);
                toSave.add(newAttendance);
            }
        }

        attendanceRepository.saveAll(toSave);
        log.info("Batch upsert attendance thành công cho lớp {} ngày {} với {} học sinh",
                classEntity.getName(), lessonDate, toSave.size());

        return getDailyAttendance(request.getClassId(), lessonDate);
    }

    @Override
    public MonthlyAttendanceMatrixResponse getMonthlyAttendanceMatrix(UUID classId, String month) {
        UUID tenantId = TenantContext.getTenantId();
        ClassEntity classEntity = findClassOrThrow(classId, tenantId);

        validateMonthFormat(month);
        YearMonth ym = YearMonth.parse(month);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        // 1. Lấy tất cả bản ghi điểm danh của lớp trong tháng
        List<Attendance> monthlyAttendances = attendanceRepository
                .findAllByTenantIdAndClassIdAndLessonDateBetweenOrderByLessonDateAsc(
                        tenantId, classId, startDate, endDate);

        // 2. Danh sách các ngày có buổi học trong tháng (distinct và sắp xếp tăng dần)
        List<LocalDate> lessonDates = monthlyAttendances.stream()
                .map(Attendance::getLessonDate)
                .distinct()
                .sorted()
                .toList();

        // 3. Lấy danh sách học sinh bị khóa do đã xuất phiếu thu
        Set<UUID> lockedStudentIds = invoiceRepository.findLockedStudentIdsByClassAndMonth(
                tenantId, classId, month, ACTIVE_INVOICE_STATUSES);

        // 4. Lấy danh sách học sinh của lớp
        List<Student> students = studentRepository.findAllByTenantIdAndClassIdOrderByFullNameAsc(tenantId, classId);
        Set<UUID> existingStudentIds = students.stream().map(Student::getId).collect(Collectors.toSet());

        // Nếu có học sinh từng điểm danh trong tháng nhưng không còn nằm trong danh sách học sinh của lớp, vẫn nạp vào
        List<UUID> missingStudentIds = monthlyAttendances.stream()
                .map(Attendance::getStudentId)
                .filter(sid -> !existingStudentIds.contains(sid))
                .distinct()
                .toList();

        if (!missingStudentIds.isEmpty()) {
            List<Student> extraStudents = studentRepository.findAllByTenantIdAndIdIn(tenantId, missingStudentIds);
            students = new ArrayList<>(students);
            students.addAll(extraStudents);
            students.sort(Comparator.comparing(Student::getFullName, String.CASE_INSENSITIVE_ORDER));
        }

        // Gom các bản ghi điểm danh theo studentId
        Map<UUID, List<Attendance>> studentAttendanceMap = monthlyAttendances.stream()
                .collect(Collectors.groupingBy(Attendance::getStudentId));

        // 5. Xây dựng ma trận học sinh
        List<StudentMonthlyAttendanceDto> studentDtos = new ArrayList<>();
        for (Student student : students) {
            List<Attendance> studentRecords = studentAttendanceMap.getOrDefault(student.getId(), Collections.emptyList());

            int attendedCount = 0;
            int excusedCount = 0;
            int absentCount = 0;

            Map<LocalDate, AttendanceRecordDto> dailyRecords = new HashMap<>();
            for (Attendance record : studentRecords) {
                if (record.getStatus() == AttendanceStatus.PRESENT) {
                    attendedCount++;
                } else if (record.getStatus() == AttendanceStatus.EXCUSED) {
                    excusedCount++;
                } else if (record.getStatus() == AttendanceStatus.ABSENT) {
                    absentCount++;
                }

                dailyRecords.put(
                        record.getLessonDate(),
                        AttendanceRecordDto.builder()
                                .attendanceId(record.getId())
                                .status(record.getStatus())
                                .note(record.getNote())
                                .build()
                );
            }

            studentDtos.add(StudentMonthlyAttendanceDto.builder()
                    .studentId(student.getId())
                    .fullName(student.getFullName())
                    .nickname(student.getNickname())
                    .attendedCount(attendedCount)
                    .excusedCount(excusedCount)
                    .absentCount(absentCount)
                    .totalLessons(studentRecords.size())
                    .isLocked(lockedStudentIds.contains(student.getId()))
                    .dailyRecords(dailyRecords)
                    .build());
        }

        return MonthlyAttendanceMatrixResponse.builder()
                .classId(classEntity.getId())
                .className(classEntity.getName())
                .month(month)
                .lessonDates(lessonDates)
                .totalClassLessons(lessonDates.size())
                .students(studentDtos)
                .build();
    }

    @Override
    @Transactional
    public AttendanceResponse updateAttendance(UUID id, AttendanceUpdateRequest request) {
        UUID tenantId = TenantContext.getTenantId();

        Attendance attendance = attendanceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Bản ghi điểm danh không tồn tại hoặc không thuộc quyền quản lý của bạn"));

        String billingMonth = attendance.getLessonDate().format(MONTH_FORMATTER);

        // Kiểm tra xem học sinh đã có phiếu thu hợp lệ trong tháng hay chưa
        boolean isLocked = invoiceRepository.existsByTenantIdAndStudentIdAndBillingMonthAndStatusIn(
                tenantId, attendance.getStudentId(), billingMonth, ACTIVE_INVOICE_STATUSES);

        if (isLocked) {
            throw new BusinessException(
                    "ATTENDANCE_LOCKED",
                    String.format("Không thể chỉnh sửa điểm danh vì kỳ học %s đã xuất phiếu thu học phí. Vui lòng hủy phiếu thu trước nếu muốn thay đổi.", billingMonth)
            );
        }

        attendance.setStatus(request.getStatus());
        attendance.setNote(SecuritySanitizer.sanitizeNote(request.getNote()));
        Attendance saved = attendanceRepository.save(attendance);

        log.info("Cập nhật bản ghi điểm danh {} thành công (status: {})", saved.getId(), saved.getStatus());

        return AttendanceResponse.builder()
                .id(saved.getId())
                .classId(saved.getClassId())
                .studentId(saved.getStudentId())
                .lessonDate(saved.getLessonDate())
                .status(saved.getStatus())
                .note(saved.getNote())
                .build();
    }

    @Override
    @Transactional
    public void deleteDailyAttendance(UUID classId, LocalDate date) {
        UUID tenantId = TenantContext.getTenantId();
        findClassOrThrow(classId, tenantId);

        LocalDate targetDate = (date != null) ? date : LocalDate.now();

        // Nếu ngày đó chưa từng điểm danh thì kết thúc sớm
        if (!attendanceRepository.existsByTenantIdAndClassIdAndLessonDate(tenantId, classId, targetDate)) {
            log.info("Không có bản ghi điểm danh nào của lớp {} ngày {} để xóa", classId, targetDate);
            return;
        }

        String billingMonth = targetDate.format(MONTH_FORMATTER);

        // Kiểm tra xem trong lớp có bất kỳ học sinh nào đã xuất phiếu thu trong tháng đó chưa
        Set<UUID> lockedStudentIds = invoiceRepository.findLockedStudentIdsByClassAndMonth(
                tenantId, classId, billingMonth, ACTIVE_INVOICE_STATUSES);

        if (!lockedStudentIds.isEmpty()) {
            throw new BusinessException(
                    "ATTENDANCE_LOCKED",
                    String.format("Không thể xóa buổi học ngày %s vì kỳ học %s đã có học sinh được xuất phiếu thu.", targetDate, billingMonth)
            );
        }

        attendanceRepository.deleteAllByTenantIdAndClassIdAndLessonDate(tenantId, classId, targetDate);
        log.info("Xóa toàn bộ buổi điểm danh của lớp {} ngày {} thành công", classId, targetDate);
    }

    private ClassEntity findClassOrThrow(UUID classId, UUID tenantId) {
        return classRepository.findByIdAndTenantId(classId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Lớp học không tồn tại hoặc không thuộc quyền quản lý của bạn"));
    }

    private void validateMonthFormat(String month) {
        if (month == null || !MONTH_PATTERN.matcher(month).matches()) {
            throw new BusinessException(
                    "INVALID_MONTH_FORMAT",
                    "Định dạng tháng không hợp lệ. Yêu cầu định dạng 'YYYY-MM' (ví dụ: '2026-10')"
            );
        }
        try {
            YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw new BusinessException(
                    "INVALID_MONTH_FORMAT",
                    "Giá trị tháng không hợp lệ trong lịch: " + month
            );
        }
    }
}
