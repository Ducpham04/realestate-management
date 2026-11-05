package com.realestate.user;

import com.realestate.user.dto.CreateUserRequest;
import com.realestate.user.dto.UserResponse;
import com.realestate.user.dto.UserSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface UserMapper {

    UserResponse toResponse(User user);

    UserSummary toSummary(User user);

    @Mapping(target = "password", ignore = true)
    @Mapping(target = "enabled", constant = "true")
    User toEntity(CreateUserRequest request);
}
