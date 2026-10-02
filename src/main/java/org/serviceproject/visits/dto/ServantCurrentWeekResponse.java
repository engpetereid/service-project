package org.serviceproject.visits.dto;

import org.serviceproject.weeks.dto.WeekResponse;

import java.util.List;

/**
 * Weekly workflow payload for servants: current active week metadata
 * along with all assigned students and their visit status.
 */
public record ServantCurrentWeekResponse(
        WeekResponse week,
        List<ServantStudentVisitItem> students
) {}
