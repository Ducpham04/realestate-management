package com.realestate.deal;

import com.realestate.customer.CustomerMapper;
import com.realestate.deal.dto.DealResponse;
import com.realestate.property.PropertyMapper;
import com.realestate.user.UserMapper;
import org.mapstruct.Mapper;

@Mapper(uses = {PropertyMapper.class, CustomerMapper.class, UserMapper.class})
public interface DealMapper {

    DealResponse toResponse(Deal deal);
}
