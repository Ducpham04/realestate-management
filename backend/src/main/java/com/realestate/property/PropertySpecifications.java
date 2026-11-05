package com.realestate.property;

import com.realestate.property.dto.PropertyFilter;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

final class PropertySpecifications {

    private PropertySpecifications() {
    }

    static Specification<Property> withFilter(PropertyFilter f) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(f.keyword())) {
                String like = "%" + f.keyword().trim().toLowerCase() + "%";
                var address = root.get("address");
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(address.get("street")), like),
                        cb.like(cb.lower(address.get("district")), like),
                        cb.like(cb.lower(address.get("city")), like)));
            }
            if (f.type() != null) {
                predicates.add(cb.equal(root.get("type"), f.type()));
            }
            if (f.listingType() != null) {
                predicates.add(cb.equal(root.get("listingType"), f.listingType()));
            }
            if (f.status() != null) {
                predicates.add(cb.equal(root.get("status"), f.status()));
            }
            if (StringUtils.hasText(f.city())) {
                predicates.add(cb.equal(cb.lower(root.get("address").get("city")), f.city().trim().toLowerCase()));
            }
            if (StringUtils.hasText(f.district())) {
                predicates.add(cb.equal(cb.lower(root.get("address").get("district")),
                        f.district().trim().toLowerCase()));
            }
            if (f.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), f.minPrice()));
            }
            if (f.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), f.maxPrice()));
            }
            if (f.minArea() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("area"), f.minArea()));
            }
            if (f.maxArea() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("area"), f.maxArea()));
            }
            if (f.minBedrooms() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("bedrooms"), f.minBedrooms()));
            }
            if (f.agentId() != null) {
                predicates.add(cb.equal(root.get("agent").get("id"), f.agentId()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
