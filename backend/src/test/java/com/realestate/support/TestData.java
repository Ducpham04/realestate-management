package com.realestate.support;

import com.realestate.appointment.Appointment;
import com.realestate.appointment.AppointmentStatus;
import com.realestate.customer.Customer;
import com.realestate.customer.CustomerType;
import com.realestate.deal.Deal;
import com.realestate.deal.DealStatus;
import com.realestate.property.Address;
import com.realestate.property.ListingType;
import com.realestate.property.Property;
import com.realestate.property.PropertyStatus;
import com.realestate.property.PropertyType;
import com.realestate.security.UserPrincipal;
import com.realestate.user.Role;
import com.realestate.user.User;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;

/** Builders for domain objects used by unit tests. */
public final class TestData {

    public static final Long ADMIN_ID = 1L;
    public static final Long AGENT_ID = 2L;
    public static final Long OTHER_AGENT_ID = 3L;

    private TestData() {
    }

    public static User user(Long id, Role role) {
        User user = User.builder()
                .fullName("User " + id)
                .email("user" + id + "@test.com")
                .password("hash")
                .role(role)
                .enabled(true)
                .build();
        user.setId(id);
        return user;
    }

    public static UserPrincipal admin() {
        return UserPrincipal.from(user(ADMIN_ID, Role.ADMIN));
    }

    public static UserPrincipal agent() {
        return UserPrincipal.from(user(AGENT_ID, Role.AGENT));
    }

    public static UserPrincipal otherAgent() {
        return UserPrincipal.from(user(OTHER_AGENT_ID, Role.AGENT));
    }

    public static Property property(Long id, User agent, PropertyStatus status, ListingType listingType) {
        Property property = Property.builder()
                .title("Property " + id)
                .type(PropertyType.APARTMENT)
                .listingType(listingType)
                .status(status)
                .price(new BigDecimal("1000000000"))
                .area(new BigDecimal("80"))
                .address(new Address("1 Street", "Ward", "District", "City"))
                .images(new ArrayList<>())
                .agent(agent)
                .build();
        property.setId(id);
        return property;
    }

    public static Customer customer(Long id, User agent) {
        Customer customer = Customer.builder()
                .fullName("Customer " + id)
                .phone("09000000" + String.format("%02d", id))
                .type(CustomerType.BUYER)
                .agent(agent)
                .build();
        customer.setId(id);
        return customer;
    }

    public static Deal deal(Long id, Property property, Customer customer, User agent, DealStatus status) {
        Deal deal = Deal.builder()
                .property(property)
                .customer(customer)
                .agent(agent)
                .amount(new BigDecimal("950000000"))
                .commission(new BigDecimal("19000000"))
                .status(status)
                .build();
        deal.setId(id);
        return deal;
    }

    public static Appointment appointment(Long id, Property property, Customer customer, User agent,
                                          Instant start, AppointmentStatus status) {
        Appointment appointment = Appointment.builder()
                .property(property)
                .customer(customer)
                .agent(agent)
                .scheduledAt(start)
                .endAt(start.plusSeconds(3600))
                .status(status)
                .build();
        appointment.setId(id);
        return appointment;
    }
}
