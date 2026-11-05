package com.realestate.property;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface PropertyRepository extends JpaRepository<Property, Long>, JpaSpecificationExecutor<Property> {

    @Override
    @EntityGraph(attributePaths = "agent")
    Page<Property> findAll(Specification<Property> spec, Pageable pageable);

    @Query("select p.status as status, count(p) as count from Property p group by p.status")
    List<StatusCount> countByStatus();

    interface StatusCount {
        PropertyStatus getStatus();

        long getCount();
    }
}
