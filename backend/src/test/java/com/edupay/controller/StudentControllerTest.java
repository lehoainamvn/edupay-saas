package com.edupay.controller;

import com.edupay.dto.request.StudentRequest;
import com.edupay.dto.response.StudentResponse;
import com.edupay.entity.StudentStatus;
import com.edupay.service.StudentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StudentController.class)
@AutoConfigureMockMvc(addFilters = false)
class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StudentService studentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /students trả về 201 Created khi thông tin học sinh và SĐT phụ huynh hợp lệ")
    void createStudent_ValidRequest_ReturnsCreated() throws Exception {
        UUID classId = UUID.fromString("d0000000-0000-0000-0000-000000000001");
        StudentRequest request = StudentRequest.builder()
                .classId(classId)
                .fullName("Nguyễn Minh Anh")
                .nickname("Bé Bống")
                .parentName("Nguyễn Văn Tuấn")
                .parentPhone("0912111222")
                .customFee(new BigDecimal("150000"))
                .build();

        StudentResponse response = StudentResponse.builder()
                .id(UUID.randomUUID())
                .classId(classId)
                .className("Toán nâng cao 8A")
                .fullName("Nguyễn Minh Anh")
                .nickname("Bé Bống")
                .parentName("Nguyễn Văn Tuấn")
                .parentPhone("0912111222")
                .customFee(new BigDecimal("150000"))
                .effectiveFee(new BigDecimal("150000"))
                .status(StudentStatus.STUDYING)
                .build();

        when(studentService.createStudent(any(StudentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Nguyễn Minh Anh"))
                .andExpect(jsonPath("$.data.nickname").value("Bé Bống"));
    }

    @Test
    @DisplayName("POST /students trả về 400 Bad Request khi SĐT phụ huynh sai định dạng")
    void createStudent_InvalidPhone_ReturnsBadRequest() throws Exception {
        StudentRequest invalidRequest = StudentRequest.builder()
                .classId(UUID.randomUUID())
                .fullName("Nguyễn Minh Anh")
                .parentPhone("12345")
                .build();

        mockMvc.perform(post("/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("GET /students/import/template trả về file Excel mẫu (.xlsx)")
    void downloadImportTemplate_ReturnsExcelBytes() throws Exception {
        byte[] sampleBytes = new byte[]{1, 2, 3, 4};
        when(studentService.generateImportTemplateExcel()).thenReturn(sampleBytes);

        mockMvc.perform(get("/students/import/template"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"Mau_Import_Hoc_Sinh_EduPay.xlsx\""))
                .andExpect(content().bytes(sampleBytes));
    }
}
