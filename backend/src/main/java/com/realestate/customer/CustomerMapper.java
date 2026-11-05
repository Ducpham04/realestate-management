package com.realestate.customer;

import com.realestate.customer.dto.CustomerRequest;
import com.realestate.customer.dto.CustomerResponse;
import com.realestate.customer.dto.CustomerSummary;
import com.realestate.user.UserMapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(uses = UserMapper.class)
public interface CustomerMapper {

    CustomerResponse toResponse(Customer customer);

    CustomerSummary toSummary(Customer customer);

    @Mapping(target = "agent", ignore = true)
    Customer toEntity(CustomerRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "agent", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(CustomerRequest request, @MappingTarget Customer customer);
}
