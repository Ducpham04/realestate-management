package com.realestate.deal;

import com.realestate.common.entity.BaseEntity;
import com.realestate.customer.Customer;
import com.realestate.property.Property;
import com.realestate.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "deals")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Deal extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id", nullable = false)
    private User agent;

    /** Agreed transaction value (VND). */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    /** Agency commission earned when the deal closes (VND). */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal commission;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DealStatus status;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
