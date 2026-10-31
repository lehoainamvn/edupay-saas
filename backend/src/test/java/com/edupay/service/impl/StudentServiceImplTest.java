package com.edupay.service.impl;

import com.edupay.config.TenantContext;
import com.edupay.dto.request.StudentRequest;
import com.edupay.dto.response.StudentImportResultResponse;
import com.edupay.dto.response.StudentResponse;
import com.edupay.entity.ClassEntity;
import com.edupay.entity.FeeType;
import com.edupay.entity.Student;
import com.edupay.entity.StudentStatus;
import com.edupay.exception.DuplicateResourceException;
import com.edupay.mapper.StudentMapper;
import com.edupay.repository.ClassRepository;
import com.edupay.repository.StudentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceImplTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ClassRepository classRepository;

    @Mock
    private StudentMapper studentMapper;

    @InjectMocks
    private StudentServiceImpl studentService;

    private UUID tenantId;
    private UUID classId;
    private ClassEntity classEntity;

    @BeforeEach
    void setUp() {
        tenantId = UUID.fromString("a0000000-0000-0000-0000-000000000001");
        classId = UUID.fromString("d0000000-0000-0000-0000-000000000001");
        TenantContext.setTenantId(tenantId);

        classEntity = ClassEntity.builder()
                .name("Toán nâng cao 8A")
                .feeType(FeeType.PER_LESSON)
                .defaultFee(new BigDecimal("150000"))
                .isActive(true)
                .build();
        classEntity.setId(classId);
        classEntity.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Thêm học sinh mới thành công khi SĐT hợp lệ và chưa trùng trong lớp")
    void createStudent_Success() {
        StudentRequest request = StudentRequest.builder()
                .classId(classId)
                .fullName("Nguyễn Minh Anh")
                .nickname("Bé Bống")
                .parentName("Nguyễn Văn Tuấn")
                .parentPhone("0912111222")
                .customFee(new BigDecimal("140000"))
                .build();

        Student entityToSave = Student.builder()
                .classId(classId)
                .fullName("Nguyễn Minh Anh")
                .nickname("Bé Bống")
                .parentPhone("0912111222")
                .customFee(new BigDecimal("140000"))
                .status(StudentStatus.STUDYING)
                .build();

        Student savedEntity = Student.builder()
                .classId(classId)
                .fullName("Nguyễn Minh Anh")
                .nickname("Bé Bống")
                .parentName("Nguyễn Văn Tuấn")
                .parentPhone("0912111222")
                .customFee(new BigDecimal("140000"))
                .status(StudentStatus.STUDYING)
                .build();
        savedEntity.setId(UUID.randomUUID());
        savedEntity.setTenantId(tenantId);

        StudentResponse mapped = StudentResponse.builder()
                .id(savedEntity.getId())
                .classId(classId)
                .fullName("Nguyễn Minh Anh")
                .nickname("Bé Bống")
                .parentPhone("0912111222")
                .customFee(new BigDecimal("140000"))
                .status(StudentStatus.STUDYING)
                .build();

        when(classRepository.findByIdAndTenantId(classId, tenantId)).thenReturn(Optional.of(classEntity));
        when(studentRepository.existsByTenantIdAndClassIdAndParentPhoneAndStatusNot(
                tenantId, classId, "0912111222", StudentStatus.DROPPED)).thenReturn(false);
        when(studentRepository.existsDuplicateNameInClass(tenantId, classId, "Nguyễn Minh Anh", "Bé Bống"))
                .thenReturn(false);
        when(studentMapper.toEntity(request)).thenReturn(entityToSave);
        when(studentRepository.save(any(Student.class))).thenReturn(savedEntity);
        when(studentMapper.toResponse(savedEntity)).thenReturn(mapped);

        StudentResponse actual = studentService.createStudent(request);

        assertNotNull(actual);
        assertEquals("Nguyễn Minh Anh", actual.getFullName());
        assertEquals("Toán nâng cao 8A", actual.getClassName());
        assertEquals(new BigDecimal("140000"), actual.getEffectiveFee());
        verify(studentRepository, times(1)).save(any(Student.class));
    }

    @Test
    @DisplayName("Ném ngoại lệ DuplicateResourceException khi SĐT phụ huynh đã tồn tại trong lớp")
    void createStudent_DuplicatePhoneInClass_ThrowsException() {
        StudentRequest request = StudentRequest.builder()
                .classId(classId)
                .fullName("Trần Gia Huy")
                .nickname("Cún")
                .parentPhone("0913222333")
                .build();

        when(classRepository.findByIdAndTenantId(classId, tenantId)).thenReturn(Optional.of(classEntity));
        when(studentRepository.existsByTenantIdAndClassIdAndParentPhoneAndStatusNot(
                tenantId, classId, "0913222333", StudentStatus.DROPPED)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> studentService.createStudent(request));
        verify(studentRepository, never()).save(any(Student.class));
    }

    @Test
    @DisplayName("Sinh file Excel mẫu bằng Apache POI và import lại thành công 3 học sinh mẫu")
    void generateAndImportExcel_Success() {
        byte[] excelBytes = studentService.generateImportTemplateExcel();
        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 0);

        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "Mau_Import_Hoc_Sinh_EduPay.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                excelBytes
        );

        when(classRepository.findByIdAndTenantId(classId, tenantId)).thenReturn(Optional.of(classEntity));
        when(studentRepository.existsByTenantIdAndClassIdAndParentPhoneAndStatusNot(
                eq(tenantId), eq(classId), anyString(), eq(StudentStatus.DROPPED))).thenReturn(false);
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> {
            Student s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });
        when(studentMapper.toResponse(any(Student.class))).thenAnswer(inv -> {
            Student s = inv.getArgument(0);
            return StudentResponse.builder()
                    .id(s.getId())
                    .classId(s.getClassId())
                    .fullName(s.getFullName())
                    .nickname(s.getNickname())
                    .parentPhone(s.getParentPhone())
                    .customFee(s.getCustomFee())
                    .status(s.getStatus())
                    .build();
        });

        StudentImportResultResponse result = studentService.importStudentsFromFile(classId, multipartFile);

        assertEquals(3, result.getTotalRows());
        assertEquals(3, result.getImportedCount());
        assertEquals(0, result.getSkippedCount());
        verify(studentRepository, times(3)).save(any(Student.class));
    }
}
