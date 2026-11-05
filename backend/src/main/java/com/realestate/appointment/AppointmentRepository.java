package com.realestate.appointment;

import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @EntityGraph(attributePaths = {"property", "customer", "agent"})
    @Query("""
            select a from Appointment a
            where (:agentId is null or a.agent.id = :agentId)
              and (:status is null or a.status = :status)
              and (:propertyId is null or a.property.id = :propertyId)
              and (:customerId is null or a.customer.id = :customerId)
              and (:from is null or a.scheduledAt >= :from)
              and (:to is null or a.scheduledAt < :to)
            """)
    Page<Appointment> search(@Param("agentId") Long agentId,
                             @Param("status") AppointmentStatus status,
                             @Param("propertyId") Long propertyId,
                             @Param("customerId") Long customerId,
                             @Param("from") Instant from,
                             @Param("to") Instant to,
                             Pageable pageable);

    /** True when the agent already has a scheduled viewing overlapping [start, end). */
    @Query("""
            select count(a) > 0 from Appointment a
            where a.agent.id = :agentId
              and a.status = com.realestate.appointment.AppointmentStatus.SCHEDULED
              and a.scheduledAt < :end and a.endAt > :start
              and (:excludeId is null or a.id <> :excludeId)
            """)
    boolean existsOverlapping(@Param("agentId") Long agentId,
                              @Param("start") Instant start,
                              @Param("end") Instant end,
                              @Param("excludeId") Long excludeId);

    @Query("""
            select count(a) from Appointment a
            where (:agentId is null or a.agent.id = :agentId)
              and a.status = com.realestate.appointment.AppointmentStatus.SCHEDULED
              and a.scheduledAt >= :from and a.scheduledAt < :to
            """)
    long countScheduledBetween(@Param("agentId") Long agentId, @Param("from") Instant from, @Param("to") Instant to);

    boolean existsByPropertyId(Long propertyId);

    boolean existsByCustomerId(Long customerId);
}
