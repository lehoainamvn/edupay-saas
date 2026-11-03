package com.edupay.service.impl;

import com.edupay.config.TenantContext;
import com.edupay.dto.request.AttendanceBatchUpsertRequest;
import com.edupay.dto.request.AttendanceItemRequest;
import com.edupay.dto.request.AttendanceUpdateRequest;
import com.edupay.dto.response.AttendanceResponse;
import com.edupay.dto.response.DailyAttendanceResponse;
import com.edupay.dto.response.MonthlyAttendanceMatrixResponse;
import com.edupay.entity.*;
import com.edupay.exception.BusinessException;
import com.edupay.exception.ResourceNotFoundException;
import com.edupay.repository.AttendanceRepository;
import com.edupay.repository.ClassRepository;
import com.edupay.repository.InvoiceRepository;
import com.edupay.repository.StudentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceImplTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private ClassRepository classRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private AttendanceServiceImpl attendanceService;

    private UUID tenantId;
    private UUID classId;
    private UUID studentId1;
    private UUID studentId2;
    private ClassEntity classEntity;
    private Student student1;
    private Student student2;

    @BeforeEach
    void setUp() {
        tenantId = UUID.fromString("a0000000-0000-0000-0000-000000000001");
        classId = UUID.fromString("d0000000-0000-0000-0000-000000000001");
        studentId1 = UUID.fromString("e0000000-0000-0000-0000-000000000001");
        studentId2 = UUID.fromString("e0000000-0000-0000-0000-000000000002");

        TenantContext.setTenantId(tenantId);

        classEntity = ClassEntity.builder()
                .name("Toán nâng cao 9A")
                .feeType(FeeType.PER_LESSON)
                .build();
        classEntity.setId(classId);
        classEntity.setTenantId(tenantId);

        student1 = Student.builder()
                .classId(classId)
                .fullName("Nguyễn Văn An")
                .nickname("Bắp")
                .parentPhone("0912111222")
                .status(StudentStatus.STUDYING)
                .build();
        student1.setId(studentId1);
        student1.setTenantId(tenantId);

        student2 = Student.builder()
                .classId(classId)
                .fullName("Trần Thị Bình")
                .nickname("Bống")
                .parentPhone("0913333444")
                .status(StudentStatus.STUDYING)
                .build();
        student2.setId(studentId2);
        student2.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Lấy điểm danh ngày: Thành công và trả về đúng số lượng học sinh, số buổi điểm danh")
    void getDailyAttendance_Success() {
        LocalDate date = LocalDate.of(2026, 10, 31);

        when(classRepository.findByIdAndTenantId(classId, tenantId)).thenReturn(Optional.of(classEntity));
        when(invoiceRepository.findLockedStudentIdsByClassAndMonth(eq(tenantId), eq(classId), eq("2026-10"), any()))
                .thenReturn(Collections.emptySet());
        when(studentRepository.findAllByTenantIdAndClassIdAndStatusOrderByFullNameAsc(tenantId, classId, StudentStatus.STUDYING))
                .thenReturn(List.of(student1, student2));

        Attendance att1 = Attendance.builder()
                .classId(classId)
                .studentId(studentId1)
                .lessonDate(date)
                .status(AttendanceStatus.PRESENT)
                .note("Đúng giờ")
                .build();
        att1.setId(UUID.randomUUID());
        att1.setTenantId(tenantId);

        when(attendanceRepository.findAllByTenantIdAndClassIdAndLessonDate(tenantId, classId, date))
                .thenReturn(List.of(att1));

        DailyAttendanceResponse response = attendanceService.getDailyAttendance(classId, date);

        assertNotNull(response);
        assertEquals(classId, response.getClassId());
        assertEquals(date, response.getLessonDate());
        assertEquals(2, response.getTotalStudents());
        assertEquals(1, response.getMarkedCount());
        assertEquals(1, response.getPresentCount());
        assertEquals(0, response.getExcusedCount());
        assertEquals(0, response.getAbsentCount());
        assertFalse(response.isClassLocked());
        assertEquals(2, response.getStudents().size());

        // Học sinh 1 đã điểm danh
        var item1 = response.getStudents().stream().filter(s -> s.getStudentId().equals(studentId1)).findFirst().orElseThrow();
        assertEquals(AttendanceStatus.PRESENT, item1.getStatus());
        assertEquals("Đúng giờ", item1.getNote());
        assertFalse(item1.isLocked());

        // Học sinh 2 chưa điểm danh
        var item2 = response.getStudents().stream().filter(s -> s.getStudentId().equals(studentId2)).findFirst().orElseThrow();
        assertNull(item2.getStatus());
        assertNull(item2.getAttendanceId());
    }

    @Test
    @DisplayName("Lấy điểm danh ngày: Đánh dấu isLocked = true nếu học sinh đã có phiếu thu trong tháng")
    void getDailyAttendance_WithLockedStudent() {
        LocalDate date = LocalDate.of(2026, 10, 31);

        when(classRepository.findByIdAndTenantId(classId, tenantId)).thenReturn(Optional.of(classEntity));
        when(invoiceRepository.findLockedStudentIdsByClassAndMonth(eq(tenantId), eq(classId), eq("2026-10"), any()))
                .thenReturn(Set.of(studentId1));
        when(studentRepository.findAllByTenantIdAndClassIdAndStatusOrderByFullNameAsc(tenantId, classId, StudentStatus.STUDYING))
                .thenReturn(List.of(student1, student2));
        when(attendanceRepository.findAllByTenantIdAndClassIdAndLessonDate(tenantId, classId, date))
                .thenReturn(Collections.emptyList());

        DailyAttendanceResponse response = attendanceService.getDailyAttendance(classId, date);

        assertNotNull(response);
        var item1 = response.getStudents().stream().filter(s -> s.getStudentId().equals(studentId1)).findFirst().orElseThrow();
        assertTrue(item1.isLocked());

        var item2 = response.getStudents().stream().filter(s -> s.getStudentId().equals(studentId2)).findFirst().orElseThrow();
        assertFalse(item2.isLocked());
    }

    @Test
    @DisplayName("Batch Upsert: Thành công lưu mới và cập nhật điểm danh cả lớp trong 1 request")
    void batchUpsertAttendance_Success() {
        LocalDate date = LocalDate.of(2026, 10, 31);

        AttendanceBatchUpsertRequest request = AttendanceBatchUpsertRequest.builder()
                .classId(classId)
                .lessonDate(date)
                .items(List.of(
                        AttendanceItemRequest.builder().studentId(studentId1).status(AttendanceStatus.PRESENT).note("").build(),
                        AttendanceItemRequest.builder().studentId(studentId2).status(AttendanceStatus.EXCUSED).note("Ốm").build()
                ))
                .build();

        when(classRepository.findByIdAndTenantId(classId, tenantId)).thenReturn(Optional.of(classEntity));
        when(studentRepository.findAllByTenantIdAndClassIdOrderByFullNameAsc(tenantId, classId))
                .thenReturn(List.of(student1, student2));
        when(invoiceRepository.findLockedStudentIds(eq(tenantId), any(), eq("2026-10"), any()))
                .thenReturn(Collections.emptySet());

        Attendance existingAtt1 = Attendance.builder()
                .classId(classId)
                .studentId(studentId1)
                .lessonDate(date)
                .status(AttendanceStatus.ABSENT)
                .build();
        existingAtt1.setId(UUID.randomUUID());
        existingAtt1.setTenantId(tenantId);

        when(attendanceRepository.findAllByTenantIdAndClassIdAndLessonDate(tenantId, classId, date))
                .thenReturn(List.of(existingAtt1));
        when(studentRepository.findAllByTenantIdAndClassIdAndStatusOrderByFullNameAsc(tenantId, classId, StudentStatus.STUDYING))
                .thenReturn(List.of(student1, student2));
        when(invoiceRepository.findLockedStudentIdsByClassAndMonth(eq(tenantId), eq(classId), eq("2026-10"), any()))
                .thenReturn(Collections.emptySet());

        DailyAttendanceResponse response = attendanceService.batchUpsertAttendance(request);

        assertNotNull(response);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Attendance>> captor = ArgumentCaptor.forClass(List.class);
        verify(attendanceRepository, times(1)).saveAll(captor.capture());

        List<Attendance> savedList = captor.getValue();
        assertEquals(2, savedList.size());

        // Record 1 được cập nhật từ ABSENT sang PRESENT
        Attendance saved1 = savedList.stream().filter(a -> a.getStudentId().equals(studentId1)).findFirst().orElseThrow();
        assertEquals(AttendanceStatus.PRESENT, saved1.getStatus());

        // Record 2 được insert mới với EXCUSED
        Attendance saved2 = savedList.stream().filter(a -> a.getStudentId().equals(studentId2)).findFirst().orElseThrow();
        assertEquals(AttendanceStatus.EXCUSED, saved2.getStatus());
        assertEquals("Ốm", saved2.getNote());
    }

    @Test
    @DisplayName("Batch Upsert: Ném BusinessException DUPLICATE_STUDENT_IN_PAYLOAD khi có học sinh bị gửi lặp")
    void batchUpsertAttendance_ThrowsException_WhenDuplicateStudents() {
        LocalDate date = LocalDate.of(2026, 10, 31);

        AttendanceBatchUpsertRequest request = AttendanceBatchUpsertRequest.builder()
                .classId(classId)
                .lessonDate(date)
                .items(List.of(
                        AttendanceItemRequest.builder().studentId(studentId1).status(AttendanceStatus.PRESENT).build(),
                        AttendanceItemRequest.builder().studentId(studentId1).status(AttendanceStatus.EXCUSED).build()
                ))
                .build();

        when(classRepository.findByIdAndTenantId(classId, tenantId)).thenReturn(Optional.of(classEntity));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                attendanceService.batchUpsertAttendance(request)
        );

        assertEquals("DUPLICATE_STUDENT_IN_PAYLOAD", exception.getErrorCode());
        verify(attendanceRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Batch Upsert: Ném BusinessException STUDENT_NOT_IN_CLASS khi học sinh không thuộc lớp")
    void batchUpsertAttendance_ThrowsException_WhenStudentNotInClass() {
        LocalDate date = LocalDate.of(2026, 10, 31);
        UUID foreignStudentId = UUID.randomUUID();

        AttendanceBatchUpsertRequest request = AttendanceBatchUpsertRequest.builder()
                .classId(classId)
                .lessonDate(date)
                .items(List.of(
                        AttendanceItemRequest.builder().studentId(foreignStudentId).status(AttendanceStatus.PRESENT).build()
                ))
                .build();

        when(classRepository.findByIdAndTenantId(classId, tenantId)).thenReturn(Optional.of(classEntity));
        when(studentRepository.findAllByTenantIdAndClassIdOrderByFullNameAsc(tenantId, classId))
                .thenReturn(List.of(student1, student2));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                attendanceService.batchUpsertAttendance(request)
        );

        assertEquals("STUDENT_NOT_IN_CLASS", exception.getErrorCode());
        verify(attendanceRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Batch Upsert: Ném BusinessException ATTENDANCE_LOCKED nếu học sinh đã xuất phiếu thu mà bị sửa điểm danh")
    void batchUpsertAttendance_ThrowsException_WhenLocked() {
        LocalDate date = LocalDate.of(2026, 10, 31);

        AttendanceBatchUpsertRequest request = AttendanceBatchUpsertRequest.builder()
                .classId(classId)
                .lessonDate(date)
                .items(List.of(
                        AttendanceItemRequest.builder().studentId(studentId1).status(AttendanceStatus.ABSENT).note("").build()
                ))
                .build();

        when(classRepository.findByIdAndTenantId(classId, tenantId)).thenReturn(Optional.of(classEntity));
        when(studentRepository.findAllByTenantIdAndClassIdOrderByFullNameAsc(tenantId, classId))
                .thenReturn(List.of(student1));
        when(invoiceRepository.findLockedStudentIds(eq(tenantId), any(), eq("2026-10"), any()))
                .thenReturn(Set.of(studentId1));

        Attendance existingAtt1 = Attendance.builder()
                .classId(classId)
                .studentId(studentId1)
                .lessonDate(date)
                .status(AttendanceStatus.PRESENT)
                .build();
        existingAtt1.setId(UUID.randomUUID());
        existingAtt1.setTenantId(tenantId);

        when(attendanceRepository.findAllByTenantIdAndClassIdAndLessonDate(tenantId, classId, date))
                .thenReturn(List.of(existingAtt1));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                attendanceService.batchUpsertAttendance(request)
        );

        assertEquals("ATTENDANCE_LOCKED", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("Nguyễn Văn An"));
        verify(attendanceRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Ma trận lịch sử tháng: Tính đúng số buổi có mặt, nghỉ phép, không phép cho từng em")
    void getMonthlyAttendanceMatrix_Success() {
        String month = "2026-10";
        LocalDate d1 = LocalDate.of(2026, 10, 2);
        LocalDate d2 = LocalDate.of(2026, 10, 5);

        when(classRepository.findByIdAndTenantId(classId, tenantId)).thenReturn(Optional.of(classEntity));
        when(invoiceRepository.findLockedStudentIdsByClassAndMonth(eq(tenantId), eq(classId), eq(month), any()))
                .thenReturn(Set.of(studentId1));
        when(studentRepository.findAllByTenantIdAndClassIdOrderByFullNameAsc(tenantId, classId))
                .thenReturn(List.of(student1, student2));

        Attendance att1_d1 = Attendance.builder().classId(classId).studentId(studentId1).lessonDate(d1).status(AttendanceStatus.PRESENT).build();
        Attendance att1_d2 = Attendance.builder().classId(classId).studentId(studentId1).lessonDate(d2).status(AttendanceStatus.PRESENT).build();
        Attendance att2_d1 = Attendance.builder().classId(classId).studentId(studentId2).lessonDate(d1).status(AttendanceStatus.EXCUSED).note("Nghỉ phép").build();
        Attendance att2_d2 = Attendance.builder().classId(classId).studentId(studentId2).lessonDate(d2).status(AttendanceStatus.ABSENT).build();

        when(attendanceRepository.findAllByTenantIdAndClassIdAndLessonDateBetweenOrderByLessonDateAsc(
                eq(tenantId), eq(classId), eq(LocalDate.of(2026, 10, 1)), eq(LocalDate.of(2026, 10, 31))))
                .thenReturn(List.of(att1_d1, att2_d1, att1_d2, att2_d2));

        MonthlyAttendanceMatrixResponse response = attendanceService.getMonthlyAttendanceMatrix(classId, month);

        assertNotNull(response);
        assertEquals("2026-10", response.getMonth());
        assertEquals(2, response.getTotalClassLessons());
        assertEquals(List.of(d1, d2), response.getLessonDates());

        // Học sinh 1: 2 có mặt, 0 phép, 0 vắng, bị lock do có phiếu thu
        var s1 = response.getStudents().stream().filter(s -> s.getStudentId().equals(studentId1)).findFirst().orElseThrow();
        assertEquals(2, s1.getAttendedCount());
        assertEquals(0, s1.getExcusedCount());
        assertEquals(0, s1.getAbsentCount());
        assertEquals(2, s1.getTotalLessons());
        assertTrue(s1.isLocked());

        // Học sinh 2: 0 có mặt, 1 phép, 1 vắng, không bị lock
        var s2 = response.getStudents().stream().filter(s -> s.getStudentId().equals(studentId2)).findFirst().orElseThrow();
        assertEquals(0, s2.getAttendedCount());
        assertEquals(1, s2.getExcusedCount());
        assertEquals(1, s2.getAbsentCount());
        assertEquals(2, s2.getTotalLessons());
        assertFalse(s2.isLocked());
    }

    @Test
    @DisplayName("Ma trận lịch sử tháng: Báo lỗi khi định dạng tháng không phải YYYY-MM")
    void getMonthlyAttendanceMatrix_InvalidMonthFormat_ThrowsException() {
        when(classRepository.findByIdAndTenantId(classId, tenantId)).thenReturn(Optional.of(classEntity));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                attendanceService.getMonthlyAttendanceMatrix(classId, "2026-13")
        );
        assertEquals("INVALID_MONTH_FORMAT", ex.getErrorCode());
    }

    @Test
    @DisplayName("Sửa lẻ 1 lượt điểm danh: Thành công khi học sinh chưa có phiếu thu trong kỳ")
    void updateAttendance_Success() {
        UUID attId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 10, 15);

        Attendance attendance = Attendance.builder()
                .classId(classId)
                .studentId(studentId1)
                .lessonDate(date)
                .status(AttendanceStatus.PRESENT)
                .build();
        attendance.setId(attId);
        attendance.setTenantId(tenantId);

        when(attendanceRepository.findByIdAndTenantId(attId, tenantId)).thenReturn(Optional.of(attendance));
        when(invoiceRepository.existsByTenantIdAndStudentIdAndBillingMonthAndStatusIn(eq(tenantId), eq(studentId1), eq("2026-10"), any()))
                .thenReturn(false);
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AttendanceUpdateRequest request = AttendanceUpdateRequest.builder()
                .status(AttendanceStatus.EXCUSED)
                .note("Bổ sung giấy phép")
                .build();

        AttendanceResponse response = attendanceService.updateAttendance(attId, request);

        assertNotNull(response);
        assertEquals(AttendanceStatus.EXCUSED, response.getStatus());
        assertEquals("Bổ sung giấy phép", response.getNote());
        verify(attendanceRepository, times(1)).save(attendance);
    }

    @Test
    @DisplayName("Sửa lẻ 1 lượt điểm danh: Bị chặn khi học sinh đã có phiếu thu trong kỳ")
    void updateAttendance_ThrowsException_WhenLocked() {
        UUID attId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 10, 15);

        Attendance attendance = Attendance.builder()
                .classId(classId)
                .studentId(studentId1)
                .lessonDate(date)
                .status(AttendanceStatus.PRESENT)
                .build();
        attendance.setId(attId);
        attendance.setTenantId(tenantId);

        when(attendanceRepository.findByIdAndTenantId(attId, tenantId)).thenReturn(Optional.of(attendance));
        when(invoiceRepository.existsByTenantIdAndStudentIdAndBillingMonthAndStatusIn(eq(tenantId), eq(studentId1), eq("2026-10"), any()))
                .thenReturn(true);

        AttendanceUpdateRequest request = AttendanceUpdateRequest.builder()
                .status(AttendanceStatus.EXCUSED)
                .note("Bổ sung giấy phép")
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () ->
                attendanceService.updateAttendance(attId, request)
        );

        assertEquals("ATTENDANCE_LOCKED", ex.getErrorCode());
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Xóa buổi điểm danh ngày: Thành công khi cả lớp chưa xuất phiếu thu trong tháng")
    void deleteDailyAttendance_Success() {
        LocalDate date = LocalDate.of(2026, 10, 20);

        when(classRepository.findByIdAndTenantId(classId, tenantId)).thenReturn(Optional.of(classEntity));
        when(attendanceRepository.existsByTenantIdAndClassIdAndLessonDate(tenantId, classId, date)).thenReturn(true);
        when(invoiceRepository.findLockedStudentIdsByClassAndMonth(eq(tenantId), eq(classId), eq("2026-10"), any()))
                .thenReturn(Collections.emptySet());

        attendanceService.deleteDailyAttendance(classId, date);

        verify(attendanceRepository, times(1)).deleteAllByTenantIdAndClassIdAndLessonDate(tenantId, classId, date);
    }

    @Test
    @DisplayName("Xóa buổi điểm danh ngày: Bị chặn nếu trong lớp đã có học sinh xuất phiếu thu trong tháng")
    void deleteDailyAttendance_ThrowsException_WhenLocked() {
        LocalDate date = LocalDate.of(2026, 10, 20);

        when(classRepository.findByIdAndTenantId(classId, tenantId)).thenReturn(Optional.of(classEntity));
        when(attendanceRepository.existsByTenantIdAndClassIdAndLessonDate(tenantId, classId, date)).thenReturn(true);
        when(invoiceRepository.findLockedStudentIdsByClassAndMonth(eq(tenantId), eq(classId), eq("2026-10"), any()))
                .thenReturn(Set.of(studentId1));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                attendanceService.deleteDailyAttendance(classId, date)
        );

        assertEquals("ATTENDANCE_LOCKED", ex.getErrorCode());
        verify(attendanceRepository, never()).deleteAllByTenantIdAndClassIdAndLessonDate(any(), any(), any());
    }
}
