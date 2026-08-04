package com.octileinstall.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Matches the { data: [...], total: N } shape used by every list endpoint in BACKEND_PLAN.md §5. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {
    private List<T> data;
    private long total;
}
