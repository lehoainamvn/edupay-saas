package com.edupay.controller;

import com.edupay.dto.request.ClassRequest;
import com.edupay.dto.response.ClassResponse;
import com.edupay.entity.FeeType;
import com.edupay.service.ClassService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClassController.class)
@AutoConfigureMockMvc(addFilters = false)
class ClassControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClassService classService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /classes trả về 201 Created khi dữ liệu hợp lệ")
    void createClass_ValidRequest_ReturnsCreated() throws Exception {
        UUID classId = UUID.randomUUID();
        ClassRequest request = ClassRequest.builder()
                .name("Toán nâng cao 8A")
                .description("Thứ 3 - Thứ 7")
                .feeType(FeeType.PER_LESSON)
                .defaultFee(new BigDecimal("150000"))
                .build();

        ClassResponse response = ClassResponse.builder()
                .id(classId)
                .name("Toán nâng cao 8A")
                .description("Thứ 3 - Thứ 7")
                .feeType(FeeType.PER_LESSON)
                .feeTypeLabel("Theo buổi")
                .defaultFee(new BigDecimal("150000"))
                .isActive(true)
                .studentCount(0L)
                .build();

        when(classService.createClass(any(ClassRequest.class))).thenReturn(response);

        mockMvc.perform(post("/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Toán nâng cao 8A"))
                .andExpect(jsonPath("$.data.feeType").value("PER_LESSON"));
    }

    @Test
    @DisplayName("POST /classes trả về 400 Bad Request khi tên lớp học bị để trống")
    void createClass_BlankName_ReturnsBadRequest() throws Exception {
        ClassRequest invalidRequest = ClassRequest.builder()
                .name("")
                .feeType(FeeType.PER_LESSON)
                .defaultFee(new BigDecimal("150000"))
                .build();

        mockMvc.perform(post("/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("DELETE /classes/{id} thực hiện Soft Delete thành công")
    void softDeleteClass_ReturnsOk() throws Exception {
        UUID classId = UUID.randomUUID();

        mockMvc.perform(delete("/classes/{id}", classId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
