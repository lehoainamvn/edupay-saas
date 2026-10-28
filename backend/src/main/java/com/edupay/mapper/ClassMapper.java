package com.edupay.mapper;

import com.edupay.dto.request.ClassRequest;
import com.edupay.dto.response.ClassResponse;
import com.edupay.entity.ClassEntity;
import org.mapstruct.*;

/**
 * MapStruct Mapper chuyển đổi giữa ClassEntity và các DTO.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ClassMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ClassEntity toEntity(ClassRequest request);

    @Mapping(target = "feeTypeLabel", expression = "java(entity.getFeeType() != null ? entity.getFeeType().getLabel() : null)")
    @Mapping(target = "studentCount", ignore = true)
    ClassResponse toResponse(ClassEntity entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(ClassRequest request, @MappingTarget ClassEntity entity);
}
