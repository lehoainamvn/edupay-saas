package com.edupay.service;

import com.edupay.dto.request.CreateTutorRequest;
import com.edupay.dto.request.UpdateTutorRequest;
import com.edupay.dto.response.TenantAdminResponse;

import java.util.List;
import java.util.UUID;

public interface AdminTutorService {

    List<TenantAdminResponse> getAllTutors(String status, String search);

    TenantAdminResponse getTutorById(UUID tutorId);

    TenantAdminResponse createTutor(CreateTutorRequest request);

    TenantAdminResponse updateTutor(UUID tutorId, UpdateTutorRequest request);

    void deleteTutor(UUID tutorId);
}

