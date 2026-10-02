package org.serviceproject.search.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.serviceproject.common.security.RoleWithScope;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.search.dto.SearchResultResponse;
import org.serviceproject.search.service.SearchService;
import org.serviceproject.users.entity.Role;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchControllerTest {

    @Mock
    private SearchService searchService;

    @InjectMocks
    private SearchController searchController;

    private UserPrincipal principal;
    private SearchResultResponse resultItem;

    @BeforeEach
    void setUp() {
        principal = new UserPrincipal(1L, 10L, "01000000000", "pass", true, 0,
                Set.of(new RoleWithScope(Role.GENERAL_ADMIN, null, null)));

        resultItem = new SearchResultResponse(
                101L, "مارك سامح", "01111111111", "مخدوم",
                10L, "ابتدائي", 100L, "رابعة ابتدائي",
                20L, "مينا جرجس", "نشط"
        );
    }

    @Test
    void searchPeople_returnsOkWithResults() {
        when(searchService.searchPeople("مارك", principal)).thenReturn(List.of(resultItem));

        ResponseEntity<List<SearchResultResponse>> response = searchController.searchPeople("مارك", principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("مارك سامح", response.getBody().get(0).fullName());
    }
}
