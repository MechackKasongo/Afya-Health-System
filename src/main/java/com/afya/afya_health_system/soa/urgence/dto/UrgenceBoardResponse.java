package com.afya.afya_health_system.soa.urgence.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Paginated urgences board with a flag when the list is empty because of hospital assignment scope
 * (clinical user without « Urgences » service).
 * <p>Français : tableau des urgences paginé + indicateur si la liste est vide pour cause d'affectation.</p>
 */
public record UrgenceBoardResponse(
        boolean scopeRestricted,
        List<UrgenceResponse> content,
        long totalElements,
        int totalPages,
        int size,
        int number,
        boolean first,
        boolean last,
        int numberOfElements,
        boolean empty
) {
    public static UrgenceBoardResponse from(Page<UrgenceResponse> page, boolean scopeRestricted) {
        return new UrgenceBoardResponse(
                scopeRestricted,
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getSize(),
                page.getNumber(),
                page.isFirst(),
                page.isLast(),
                page.getNumberOfElements(),
                page.isEmpty()
        );
    }
}
