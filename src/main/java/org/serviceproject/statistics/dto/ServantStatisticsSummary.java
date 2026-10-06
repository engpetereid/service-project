package org.serviceproject.statistics.dto;

/**
 * Servant-level summary for class, ministry, and global statistics views.
 * Covers spiritual note, meeting attendance, mass attendance, and student visitation.
 */
public record ServantStatisticsSummary(
        Long servantId,
        String servantName,
        Long ministryId,
        String ministryName,
        Long classId,
        String className,
        int assignedStudentsCount,
        int visitedCount,
        double visitPercentage,
        Integer noteScore,
        Integer maxNoteScore,
        Double notePercentage,
        Boolean attendedServiceMeeting,
        Boolean attendedMass,
        Boolean attendedTasbeha,
        Boolean attendedManagementMeeting,
        boolean recordedSelfFollowUp
) {
    /**
     * Backward-compatible 5-argument constructor for legacy calls.
     */
    public ServantStatisticsSummary(Long servantId, String servantName, int assignedStudentsCount,
                                    int visitedCount, double visitPercentage) {
        this(servantId, servantName, null, null, null, null, assignedStudentsCount, visitedCount,
                visitPercentage, null, null, null, null, null, null, null, false);
    }
}
