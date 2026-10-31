package com.edupay.mapper;

import com.edupay.dto.request.StudentRequest;
import com.edupay.dto.response.StudentResponse;
import com.edupay.entity.Student;
import org.mapstruct.*;

/**
 * MapStruct Mapper chuyển đổi giữa Student Entity và các DTO.
 * Sử dụng builder = @Builder(disableBuilder = true) để tránh xung đột với Lombok @Builder
 * đối với các thuộc tính kế thừa từ BaseEntity (id, tenantId, createdAt, updatedAt).
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        builder = @Builder(disableBuilder = true)
)
public interface StudentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "status", expression = "java(request.getStatus() != null ? request.getStatus() : com.edupay.entity.StudentStatus.STUDYING)")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Student toEntity(StudentRequest request);

    @Mapping(target = "statusLabel", expression = "java(entity.getStatus() != null ? entity.getStatus().getLabel() : null)")
    @Mapping(target = "className", ignore = true)
    @Mapping(target = "classFeeType", ignore = true)
    @Mapping(target = "classDefaultFee", ignore = true)
    @Mapping(target = "effectiveFee", ignore = true)
    StudentResponse toResponse(Student entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(StudentRequest request, @MappingTarget Student entity);
}
