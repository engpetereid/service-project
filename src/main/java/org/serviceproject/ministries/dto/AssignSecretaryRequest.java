package org.serviceproject.ministries.dto;

/**
 * Request payload for assigning or removing a secretary for a ministry or class.
 * If both personId and userId are null, the secretary is removed.
 */
public record AssignSecretaryRequest(
        Long personId,
        Long userId
) {}
