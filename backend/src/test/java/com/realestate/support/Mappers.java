package com.realestate.support;

import com.realestate.appointment.AppointmentMapper;
import com.realestate.appointment.AppointmentMapperImpl;
import com.realestate.customer.CustomerMapper;
import com.realestate.customer.CustomerMapperImpl;
import com.realestate.deal.DealMapper;
import com.realestate.deal.DealMapperImpl;
import com.realestate.property.PropertyMapper;
import com.realestate.property.PropertyMapperImpl;
import com.realestate.user.UserMapper;
import com.realestate.user.UserMapperImpl;

/** Real MapStruct implementations wired by hand, so unit tests exercise actual mapping code. */
public final class Mappers {

    public static final UserMapper USER = new UserMapperImpl();
    public static final PropertyMapper PROPERTY = new PropertyMapperImpl(USER);
    public static final CustomerMapper CUSTOMER = new CustomerMapperImpl(USER);
    public static final AppointmentMapper APPOINTMENT = new AppointmentMapperImpl(PROPERTY, CUSTOMER, USER);
    public static final DealMapper DEAL = new DealMapperImpl(PROPERTY, CUSTOMER, USER);

    private Mappers() {
    }
}
