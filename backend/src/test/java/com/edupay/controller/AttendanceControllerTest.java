package com.edupay.controller;

import com.edupay.dto.request.AttendanceBatchUpsertRequest;
import com.edupay.dto.request.AttendanceItemRequest;
import com.edupay.dto.request.AttendanceUpdateRequest;
import com.edupay.dto.response.AttendanceResponse;
import com.edupay.dto.response.DailyAttendanceItemResponse;
import com.edupay.dto.response.DailyAttendanceResponse;
import com.edupay.dto.response.MonthlyAttendanceMatrixResponse;
import com.edupay.entity.AttendanceStatus;
import com.edupay.security.JwtTokenProvider;
import com.edupay.security.RateLimitingFilter;
import com.edupay.service.AttendanceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AttendanceController.class)
@AutoConfigureMockMvc(addFilters = false)
class AttendanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AttendanceService attendanceService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private RateLimitingFilter rateLimitingFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /attendance/classes/{classId}/daily trả về 200 OK với danh sách học sinh điểm danh")
    void getDailyAttendance_ReturnsOk() throws Exception {
        UUID classId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 10, 31);

        DailyAttendanceResponse response = DailyAttendanceResponse.builder()
                .classId(classId)
                .className("Toán 9A")
                .lessonDate(date)
                .totalStudents(1)
                .markedCount(1)
                .presentCount(1)
                .excusedCount(0)
                .absentCount(0)
                .isClassLocked(false)
                .students(List.of(
                        DailyAttendanceItemResponse.builder()
                                .studentId(UUID.randomUUID())
                                .fullName("Nguyễn Văn An")
                                .status(AttendanceStatus.PRESENT)
                                .isLocked(false)
                                .build()
                ))
                .build();

        when(attendanceService.getDailyAttendance(eq(classId), eq(date))).thenReturn(response);

        mockMvc.perform(get("/attendance/classes/{classId}/daily", classId)
                        .param("date", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.className").value("Toán 9A"))
                .andExpect(jsonPath("$.data.presentCount").value(1))
                .andExpect(jsonPath("$.data.students[0].fullName").value("Nguyễn Văn An"))
                .andExpect(jsonPath("$.data.students[0].status").value("PRESENT"));
    }

    @Test
    @DisplayName("POST /attendance/batch trả về 200 OK khi gửi payload hợp lệ")
    void batchUpsertAttendance_ValidRequest_ReturnsOk() throws Exception {
        UUID classId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 10, 31);

        AttendanceBatchUpsertRequest request = AttendanceBatchUpsertRequest.builder()
                .classId(classId)
                .lessonDate(date)
                .items(List.of(
                        AttendanceItemRequest.builder()
                                .studentId(UUID.randomUUID())
                                .status(AttendanceStatus.PRESENT)
                                .note("Đi học đầy đủ")
                                .build()
                ))
                .build();

        DailyAttendanceResponse response = DailyAttendanceResponse.builder()
                .classId(classId)
                .className("Toán 9A")
                .lessonDate(date)
                .totalStudents(1)
                .markedCount(1)
                .presentCount(1)
                .students(Collections.emptyList())
                .build();

        when(attendanceService.batchUpsertAttendance(any(AttendanceBatchUpsertRequest.class))).thenReturn(response);

        mockMvc.perform(post("/attendance/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.className").value("Toán 9A"));
    }

    @Test
    @DisplayName("POST /attendance/batch trả về 400 Bad Request khi items rỗng hoặc null")
    void batchUpsertAttendance_InvalidRequest_ReturnsBadRequest() throws Exception {
        AttendanceBatchUpsertRequest invalidRequest = AttendanceBatchUpsertRequest.builder()
                .classId(UUID.randomUUID())
                .lessonDate(LocalDate.now())
                .items(Collections.emptyList())
                .build();

        mockMvc.perform(post("/attendance/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /attendance/classes/{classId}/monthly trả về 200 OK với ma trận tháng")
    void getMonthlyAttendanceMatrix_ReturnsOk() throws Exception {
        UUID classId = UUID.randomUUID();
        String month = "2026-10";

        MonthlyAttendanceMatrixResponse response = MonthlyAttendanceMatrixResponse.builder()
                .classId(classId)
                .className("Toán 9A")
                .month(month)
                .lessonDates(List.of(LocalDate.of(2026, 10, 2)))
                .totalClassLessons(1)
                .students(Collections.emptyList())
                .build();

        when(attendanceService.getMonthlyAttendanceMatrix(eq(classId), eq(month))).thenReturn(response);

        mockMvc.perform(get("/attendance/classes/{classId}/monthly", classId)
                        .param("month", month))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.month").value("2026-10"))
                .andExpect(jsonPath("$.data.totalClassLessons").value(1));
    }

    @Test
    @DisplayName("PUT /attendance/{id} trả về 200 OK khi cập nhật thành công")
    void updateAttendance_ReturnsOk() throws Exception {
        UUID attId = UUID.randomUUID();

        AttendanceUpdateRequest request = AttendanceUpdateRequest.builder()
                .status(AttendanceStatus.EXCUSED)
                .note("Nghỉ ốm")
                .build();

        AttendanceResponse response = AttendanceResponse.builder()
                .id(attId)
                .status(AttendanceStatus.EXCUSED)
                .note("Nghỉ ốm")
                .build();

        when(attendanceService.updateAttendance(eq(attId), any(AttendanceUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/attendance/{id}", attId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("EXCUSED"))
                .andExpect(jsonPath("$.data.note").value("Nghỉ ốm"));
    }

    @Test
    @DisplayName("DELETE /attendance/classes/{classId}/daily trả về 200 OK khi hủy buổi học thành công")
    void deleteDailyAttendance_ReturnsOk() throws Exception {
        UUID classId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 10, 31);

        doNothing().when(attendanceService).deleteDailyAttendance(eq(classId), eq(date));

        mockMvc.perform(delete("/attendance/classes/{classId}/daily", classId)
                        .param("date", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
