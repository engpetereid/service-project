package org.serviceproject.academic.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.serviceproject.academic.dto.PromotionMappingRequest;
import org.serviceproject.academic.dto.PromotionMappingResponse;
import org.serviceproject.academic.service.PromotionMappingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for managing promotion mappings.
 */
@RestController
@RequestMapping("/api/promotion-mappings")
@RequiredArgsConstructor
public class PromotionMappingController {

    private final PromotionMappingService promotionMappingService;

    @GetMapping
    public ResponseEntity<List<PromotionMappingResponse>> findAll() {
        return ResponseEntity.ok(promotionMappingService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromotionMappingResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(promotionMappingService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('GENERAL_ADMIN')")
    public ResponseEntity<PromotionMappingResponse> create(
            @Valid @RequestBody PromotionMappingRequest request) {
        PromotionMappingResponse response = promotionMappingService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('GENERAL_ADMIN')")
    public ResponseEntity<PromotionMappingResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PromotionMappingRequest request) {
        return ResponseEntity.ok(promotionMappingService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('GENERAL_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        promotionMappingService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
