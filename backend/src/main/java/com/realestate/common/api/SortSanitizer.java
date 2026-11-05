package com.realestate.common.api;

import com.realestate.common.exception.AppException;
import com.realestate.common.exception.ErrorCode;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Restricts client-provided sort fields to an allow-list and caps the page size,
 * so arbitrary entity paths can never be injected through the {@code sort} parameter.
 */
public final class SortSanitizer {

    public static final int MAX_PAGE_SIZE = 100;

    private SortSanitizer() {
    }

    public static Pageable sanitize(Pageable pageable, Set<String> allowed, Sort defaultSort) {
        for (Sort.Order order : pageable.getSort()) {
            if (!allowed.contains(order.getProperty())) {
                throw new AppException(ErrorCode.BAD_REQUEST,
                        "Invalid sort property '%s'. Allowed: %s".formatted(order.getProperty(), allowed));
            }
        }
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : defaultSort;
        int size = Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE);
        return PageRequest.of(Math.max(pageable.getPageNumber(), 0), size, sort);
    }
}
