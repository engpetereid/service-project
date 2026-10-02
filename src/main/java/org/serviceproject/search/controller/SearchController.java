package org.serviceproject.search.controller;

import lombok.RequiredArgsConstructor;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.search.dto.SearchResultResponse;
import org.serviceproject.search.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for global search across servants and students.
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    /**
     * Searches active people by name or phone within the user's authorized scope.
     */
    @GetMapping
    public ResponseEntity<List<SearchResultResponse>> searchPeople(
            @RequestParam(name = "q", defaultValue = "") String query,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<SearchResultResponse> results = searchService.searchPeople(query, principal);
        return ResponseEntity.ok(results);
    }
}
