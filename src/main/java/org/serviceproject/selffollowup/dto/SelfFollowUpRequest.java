package org.serviceproject.selffollowup.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for upserting a personal weekly self-follow-up record.
 */
public record SelfFollowUpRequest(
        @NotNull(message = "الأسبوع مطلوب")
        Long weekId,

        @Min(value = 0, message = "درجة النوتة لا يمكن أن تكون سالبة")
        Integer noteScore,

        Boolean attendedMass,
        Boolean attendedServiceMeeting,
        Boolean attendedTasbeha,
        Boolean attendedManagementMeeting
) {}
