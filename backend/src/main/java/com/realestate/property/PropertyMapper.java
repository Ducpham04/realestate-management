package com.realestate.property;

import com.realestate.property.dto.PropertyRequest;
import com.realestate.property.dto.PropertyResponse;
import com.realestate.property.dto.PropertySummary;
import com.realestate.user.UserMapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(uses = UserMapper.class)
public interface PropertyMapper {

    PropertyResponse toResponse(Property property);

    PropertySummary toSummary(Property property);

    @Mapping(target = "images", ignore = true)
    @Mapping(target = "agent", ignore = true)
    @Mapping(target = "status", ignore = true)
    Property toEntity(PropertyRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "agent", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(PropertyRequest request, @MappingTarget Property property);
}
