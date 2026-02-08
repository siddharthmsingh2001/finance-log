package com.financelog.backend.service;

import com.financelog.backend.dto.UserDto;
import com.financelog.backend.entity.AuthenticatedUser;

public interface OrchestrationService {

    UserDto getOrCreateUser(AuthenticatedUser authUser);

}
