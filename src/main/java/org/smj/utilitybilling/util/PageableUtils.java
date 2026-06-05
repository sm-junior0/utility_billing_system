package org.smj.utilitybilling.util;

import java.util.Set;
import org.smj.utilitybilling.exception.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Validates pageable query parameters before they reach Spring Data JPA.
 */

public final class PageableUtils {

    private PageableUtils() {
    }

    public static Pageable validateSort(Pageable pageable, Set<String> allowedSortProperties, String defaultSortProperty) {
        if (pageable.getSort().isUnsorted()) {
            return pageable;
        }

        for (Sort.Order order : pageable.getSort()) {
            if (!allowedSortProperties.contains(order.getProperty())) {
                throw new BadRequestException(
                        "Invalid sort field '" + order.getProperty() + "'. Allowed values: "
                                + String.join(", ", allowedSortProperties));
            }
        }
        return pageable;
    }

    public static Pageable withDefaultSort(Pageable pageable, String defaultSortProperty) {
        if (!pageable.getSort().isUnsorted()) {
            return pageable;
        }
        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(defaultSortProperty).ascending());
    }
}
