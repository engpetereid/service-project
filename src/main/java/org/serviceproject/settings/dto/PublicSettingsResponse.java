package org.serviceproject.settings.dto;

/**
 * Publicly visible settings payload available to all authenticated users.
 */
public record PublicSettingsResponse(
        int maxNoteScore
) {}
