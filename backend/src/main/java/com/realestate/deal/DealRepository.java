package com.realestate.deal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DealRepository extends JpaRepository<Deal, Long> {

    @EntityGraph(attributePaths = {"property", "customer", "agent"})
    @Query("""
            select d from Deal d
            where (:agentId is null or d.agent.id = :agentId)
              and (:status is null or d.status = :status)
              and (:propertyId is null or d.property.id = :propertyId)
              and (:customerId is null or d.customer.id = :customerId)
              and (:minAmount is null or d.amount >= :minAmount)
              and (:maxAmount is null or d.amount <= :maxAmount)
            """)
    Page<Deal> search(@Param("agentId") Long agentId,
                      @Param("status") DealStatus status,
                      @Param("propertyId") Long propertyId,
                      @Param("customerId") Long customerId,
                      @Param("minAmount") BigDecimal minAmount,
                      @Param("maxAmount") BigDecimal maxAmount,
                      Pageable pageable);

    boolean existsByPropertyIdAndStatusIn(Long propertyId, Collection<DealStatus> statuses);

    boolean existsByPropertyId(Long propertyId);

    boolean existsByCustomerId(Long customerId);

    @Query("""
            select count(d) from Deal d
            where (:agentId is null or d.agent.id = :agentId) and d.status in :statuses
            """)
    long countByStatuses(@Param("agentId") Long agentId, @Param("statuses") Collection<DealStatus> statuses);

    /**
     * Closed-deal revenue grouped by calendar month (UTC), oldest first.
     * Each row is [yyyy-MM, deal count, total amount, total commission].
     */
    @Query(value = """
            select date_format(d.closed_at, '%Y-%m') as period,
                   count(*) as deals,
                   coalesce(sum(d.amount), 0) as revenue,
                   coalesce(sum(d.commission), 0) as commission
            from deals d
            where d.status = 'CLOSED'
              and d.closed_at >= :from and d.closed_at < :to
              and (:agentId is null or d.agent_id = :agentId)
            group by period
            order by period
            """, nativeQuery = true)
    List<MonthlyRevenueRow> revenueByMonth(@Param("from") Instant from,
                                           @Param("to") Instant to,
                                           @Param("agentId") Long agentId);

    interface MonthlyRevenueRow {
        String getPeriod();

        long getDeals();

        BigDecimal getRevenue();

        BigDecimal getCommission();
    }
}
