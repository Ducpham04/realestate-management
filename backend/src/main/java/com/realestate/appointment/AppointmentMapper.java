package com.realestate.appointment;

import com.realestate.appointment.dto.AppointmentResponse;
import com.realestate.customer.CustomerMapper;
import com.realestate.property.PropertyMapper;
import com.realestate.user.UserMapper;
import org.mapstruct.Mapper;

@Mapper(uses = {PropertyMapper.class, CustomerMapper.class, UserMapper.class})
public interface AppointmentMapper {

    AppointmentResponse toResponse(Appointment appointment);
}
