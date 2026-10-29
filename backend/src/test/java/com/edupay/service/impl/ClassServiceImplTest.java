package com.edupay.service.impl;

import com.edupay.config.TenantContext;
import com.edupay.dto.request.ClassRequest;
import com.edupay.dto.response.ClassResponse;
import com.edupay.entity.ClassEntity;
import com.edupay.entity.FeeType;
import com.edupay.exception.DuplicateResourceException;
import com.edupay.exception.ResourceNotFoundException;
import com.edupay.mapper.ClassMapper;
import com.edupay.repository.ClassRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClassServiceImplTest {

    @Mock
    private ClassRepository classRepository;

    @Mock
    private ClassMapper classMapper;

    @InjectMocks
    private ClassServiceImpl classService;

    private UUID tenantId;
    private UUID classId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.fromString("a0000000-0000-0000-0000-000000000001");
        classId = UUID.fromString("d0000000-0000-0000-0000-000000000001");
        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Lấy danh sách lớp đang hoạt động lọc đúng theo tenant_id")
    void getClasses_ActiveOnly_Success() {
        ClassEntity entity = ClassEntity.builder()
                .name("Toán nâng cao 8A")
                .feeType(FeeType.PER_LESSON)
                .defaultFee(new BigDecimal("150000"))
                .isActive(true)
                .build();
        entity.setId(classId);
        entity.setTenantId(tenantId);

        ClassResponse mapped = ClassResponse.builder()
                .id(classId)
                .name("Toán nâng cao 8A")
                .feeType(FeeType.PER_LESSON)
                .feeTypeLabel("Theo buổi")
                .defaultFee(new BigDecimal("150000"))
                .isActive(true)
                .build();

        when(classRepository.findAllByTenantIdAndIsActiveTrueOrderByCreatedAtDesc(tenantId))
                .thenReturn(List.of(entity));
        when(classMapper.toResponse(entity)).thenReturn(mapped);
        when(classRepository.countActiveStudentsByClassIdAndTenantId(classId, tenantId)).thenReturn(4L);

        List<ClassResponse> result = classService.getClasses(false);

        assertEquals(1, result.size());
        assertEquals("Toán nâng cao 8A", result.get(0).getName());
        assertEquals(4L, result.get(0).getStudentCount());
        verify(classRepository, times(1)).findAllByTenantIdAndIsActiveTrueOrderByCreatedAtDesc(tenantId);
    }

    @Test
    @DisplayName("Tạo lớp học mới thành công và tự động gán tenant_id từ TenantContext")
    void createClass_Success() {
        ClassRequest request = ClassRequest.builder()
                .name("Luyện thi vào 10 - Nhóm A")
                .description("Thứ 2 - Thứ 5 (18:00 - 19:30)")
                .feeType(FeeType.PER_LESSON)
                .defaultFee(new BigDecimal("180000"))
                .build();

        ClassEntity entityToSave = ClassEntity.builder()
                .name("Luyện thi vào 10 - Nhóm A")
                .description("Thứ 2 - Thứ 5 (18:00 - 19:30)")
                .feeType(FeeType.PER_LESSON)
                .defaultFee(new BigDecimal("180000"))
                .build();

        ClassEntity savedEntity = ClassEntity.builder()
                .name("Luyện thi vào 10 - Nhóm A")
                .description("Thứ 2 - Thứ 5 (18:00 - 19:30)")
                .feeType(FeeType.PER_LESSON)
                .defaultFee(new BigDecimal("180000"))
                .isActive(true)
                .build();
        savedEntity.setId(classId);
        savedEntity.setTenantId(tenantId);

        ClassResponse expectedResponse = ClassResponse.builder()
                .id(classId)
                .name("Luyện thi vào 10 - Nhóm A")
                .feeType(FeeType.PER_LESSON)
                .feeTypeLabel("Theo buổi")
                .defaultFee(new BigDecimal("180000"))
                .isActive(true)
                .build();

        when(classRepository.existsByTenantIdAndNameIgnoreCaseAndIsActiveTrue(tenantId, "Luyện thi vào 10 - Nhóm A"))
                .thenReturn(false);
        when(classMapper.toEntity(request)).thenReturn(entityToSave);
        when(classRepository.save(any(ClassEntity.class))).thenReturn(savedEntity);
        when(classMapper.toResponse(savedEntity)).thenReturn(expectedResponse);

        ClassResponse actual = classService.createClass(request);

        assertNotNull(actual);
        assertEquals("Luyện thi vào 10 - Nhóm A", actual.getName());
        assertEquals(tenantId, entityToSave.getTenantId());
        assertTrue(entityToSave.getIsActive());
        verify(classRepository, times(1)).save(entityToSave);
    }

    @Test
    @DisplayName("Ném DuplicateResourceException khi tạo lớp trùng tên trong cùng Tenant")
    void createClass_DuplicateName_ThrowsException() {
        ClassRequest request = ClassRequest.builder()
                .name("Toán nâng cao 8A")
                .feeType(FeeType.PER_LESSON)
                .defaultFee(new BigDecimal("150000"))
                .build();

        when(classRepository.existsByTenantIdAndNameIgnoreCaseAndIsActiveTrue(tenantId, "Toán nâng cao 8A"))
                .thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> classService.createClass(request));
        verify(classRepository, never()).save(any(ClassEntity.class));
    }

    @Test
    @DisplayName("Soft Delete lớp học chỉ cập nhật is_active = false để giữ lịch sử điểm danh và học phí")
    void softDeleteClass_SetsIsActiveFalse() {
        ClassEntity entity = ClassEntity.builder()
                .name("Toán nâng cao 8A")
                .feeType(FeeType.PER_LESSON)
                .defaultFee(new BigDecimal("150000"))
                .isActive(true)
                .build();
        entity.setId(classId);
        entity.setTenantId(tenantId);

        when(classRepository.findByIdAndTenantId(classId, tenantId)).thenReturn(Optional.of(entity));

        classService.softDeleteClass(classId);

        assertFalse(entity.getIsActive());
        verify(classRepository, times(1)).save(entity);
        verify(classRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Ném ResourceNotFoundException khi truy cập lớp học không thuộc Tenant hiện tại")
    void getClassById_DifferentTenant_ThrowsResourceNotFound() {
        UUID otherTenantClassId = UUID.randomUUID();
        when(classRepository.findByIdAndTenantId(otherTenantClassId, tenantId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> classService.getClassById(otherTenantClassId));
    }
}
