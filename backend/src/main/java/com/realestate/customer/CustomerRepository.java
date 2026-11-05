package com.realestate.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, Long id);

    long countByAgentId(Long agentId);

    @EntityGraph(attributePaths = "agent")
    @Query("""
            select c from Customer c
            where (:agentId is null or c.agent.id = :agentId)
              and (:type is null or c.type = :type)
              and (:keyword is null
                   or lower(c.fullName) like lower(concat('%', :keyword, '%'))
                   or lower(c.email) like lower(concat('%', :keyword, '%'))
                   or c.phone like concat('%', :keyword, '%'))
            """)
    Page<Customer> search(@Param("keyword") String keyword,
                          @Param("type") CustomerType type,
                          @Param("agentId") Long agentId,
                          Pageable pageable);
}
