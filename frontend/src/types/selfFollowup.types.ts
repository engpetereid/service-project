import { WeekResponse } from './visit.types';

export interface SelfFollowUpRequest {
  weekId: number;
  noteScore: number | null;
  attendedMass: boolean | null;
  attendedServiceMeeting: boolean | null;
  attendedTasbeha: boolean | null;
  attendedManagementMeeting: boolean | null;
}

export interface SelfFollowUpResponse {
  id: number;
  weekId: number;
  weekStartDate: string;
  weekEndDate: string;
  weekLocked: boolean;
  academicYearId: number;
  academicYearName: string;
  noteScore: number | null;
  maxNoteScoreSnapshot: number;
  attendedMass: boolean | null;
  attendedServiceMeeting: boolean | null;
  attendedTasbeha: boolean | null;
  attendedManagementMeeting: boolean | null;
  overallPercentage: number;
  createdAt: string;
  updatedAt: string;
}

export interface SelfFollowUpCurrentWeekResponse {
  week: WeekResponse;
  record: SelfFollowUpResponse | null;
  maxNoteScore: number;
}

export interface WeeklyTrendPoint {
  weekId: number;
  weekStartDate: string;
  weekEndDate: string;
  overallPercentage: number | null;
}

export interface SelfFollowUpStatsResponse {
  totalWeeks: number;
  recordedWeeks: number;
  recordingRate: number;
  avgNotePercentage: number | null;
  avgMassRate: number | null;
  avgServiceMeetingRate: number | null;
  avgTasbehaRate: number | null;
  avgManagementMeetingRate: number | null;
  avgOverallPercentage: number | null;
  weeklyTrend: WeeklyTrendPoint[];
}
