package com.edupay.service;

import com.edupay.dto.request.CreateCenterRequest;
import com.edupay.dto.request.UpdateCenterRequest;
import com.edupay.dto.response.CenterTeacherResponse;
import com.edupay.dto.response.TenantAdminResponse;

import java.util.List;
import java.util.UUID;

public interface AdminCenterService {

    List<TenantAdminResponse> getAllCenters(String status, String search);

    TenantAdminResponse getCenterById(UUID centerId);

    TenantAdminResponse createCenter(CreateCenterRequest request);

    TenantAdminResponse updateCenter(UUID centerId, UpdateCenterRequest request);

    void deleteCenter(UUID centerId);

    List<CenterTeacherResponse> getCenterTeachers(UUID centerId);
}

