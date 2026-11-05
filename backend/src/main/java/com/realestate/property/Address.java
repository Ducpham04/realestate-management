package com.realestate.property;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    @Column(name = "address_street", nullable = false, length = 255)
    private String street;

    @Column(name = "address_ward", length = 100)
    private String ward;

    @Column(name = "address_district", nullable = false, length = 100)
    private String district;

    @Column(name = "address_city", nullable = false, length = 100)
    private String city;
}
