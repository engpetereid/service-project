export interface DashboardStatisticsResponse {
  weekId: number;
  weekStartDate: string;
  weekEndDate: string;
  totalStudents: number;
  visitedStudents: number;
  visitPercentage: number;
  massAttendanceCount: number;
  massAttendancePercentage: number;
  meetingAttendanceCount: number;
  meetingAttendancePercentage: number;
  tasbehaAttendanceCount: number;
  tasbehaAttendancePercentage: number;
  overallFollowupIndex: number;
  totalAbsenceAlerts: number;
}

export interface AbsenceAlertResponse {
  studentId: number;
  studentName: string;
  phone: string;
  guardianPhone?: string | null;
  ministryId?: number | null;
  ministryName?: string | null;
  classId?: number | null;
  className?: string | null;
  servantId?: number | null;
  servantName?: string | null;
  consecutiveWeeksAbsent: number;
  lastContactDate?: string | null;
}

export interface WeeklyTrendDataPoint {
  weekId: number;
  weekLabel: string;
  startDate: string;
  endDate: string;
  totalStudents: number;
  visitedCount: number;
  visitPercentage: number;
  massCount: number;
  massPercentage: number;
  meetingCount: number;
  meetingPercentage: number;
  tasbehaCount: number;
  tasbehaPercentage: number;
  overallFollowupIndex: number;
}

export interface MinistryStatisticsResponse {
  ministryId: number;
  ministryName: string;
  dashboard: DashboardStatisticsResponse;
  classesStats: ClassStatisticsSummary[];
}

export interface ClassStatisticsSummary {
  classId: number;
  className: string;
  totalStudents: number;
  visitedStudents: number;
  visitPercentage: number;
  meetingAttendanceCount: number;
  meetingAttendancePercentage: number;
  overallFollowupIndex: number;
}

export interface ClassStatisticsResponse {
  classId: number;
  className: string;
  ministryId: number;
  ministryName: string;
  dashboard: DashboardStatisticsResponse;
  servantsStats: ServantStatisticsSummary[];
}

export interface ServantStatisticsSummary {
  servantId: number;
  servantName: string;
  phone?: string | null;
  ministryId?: number | null;
  ministryName?: string | null;
  classId?: number | null;
  className?: string | null;
  assignedStudentsCount: number;
  visitedCount: number;
  visitPercentage: number;
  noteScore?: number | null;
  maxNoteScore?: number | null;
  notePercentage?: number | null;
  attendedServiceMeeting?: boolean | null;
  attendedMass?: boolean | null;
  attendedTasbeha?: boolean | null;
  attendedManagementMeeting?: boolean | null;
  recordedSelfFollowUp: boolean;
}

export interface ServantPerformanceResponse {
  totalServants: number;
  recordedFollowUpCount: number;
  followUpSubmissionRate: number;
  averageNotePercentage: number | null;
  massAttendanceRate: number | null;
  meetingAttendanceRate: number | null;
  overallVisitPercentage: number;
  servants: ServantStatisticsSummary[];
}

export interface ServantRecentWeekRecord {
  weekId: number;
  weekStartDate: string;
  weekEndDate: string;
  noteScore: number | null;
  maxNoteScore: number | null;
  notePercentage: number | null;
  attendedMass: boolean | null;
  attendedServiceMeeting: boolean | null;
  visitedCount: number;
  assignedCount: number;
  visitPercentage: number;
  recorded: boolean;
}

export interface ServantStatisticsResponse {
  servantId: number;
  servantName: string;
  phone?: string | null;
  ministryId?: number | null;
  ministryName?: string | null;
  classId?: number | null;
  className?: string | null;
  assignedStudentsCount: number;
  visitedCount: number;
  visitPercentage: number;
  averagePrayerScore: number | null;
  averageReadingScore: number | null;
  averageNoteScore: number | null;
  recordedFollowUp: boolean;
  noteScore?: number | null;
  maxNoteScore?: number | null;
  notePercentage?: number | null;
  attendedMass?: boolean | null;
  attendedServiceMeeting?: boolean | null;
  attendedTasbeha?: boolean | null;
  attendedManagementMeeting?: boolean | null;
  annualRecordingRate?: number | null;
  annualAverageNotePercentage?: number | null;
  annualMassAttendanceRate?: number | null;
  annualMeetingAttendanceRate?: number | null;
  annualVisitPercentage?: number | null;
  recentWeeks: ServantRecentWeekRecord[];
}

export interface StudentRecentVisitSummary {
  visitId: number;
  weekId: number;
  weekStartDate: string;
  method: 'VISIT' | 'CALL';
  prayerScore: number | null;
  readingScore: number | null;
  noteScore: number | null;
  notes: string | null;
  servantName: string | null;
  recordedDate: string;
}

export interface StudentStatisticsResponse {
  studentId: number;
  studentName: string;
  phone: string;
  ministryName: string;
  className: string;
  servantName?: string | null;
  totalWeeksCount: number;
  visitedWeeksCount: number;
  visitPercentage: number;
  massAttendanceCount: number;
  massAttendancePercentage: number;
  meetingAttendanceCount: number;
  meetingAttendancePercentage: number;
  tasbehaAttendanceCount: number;
  tasbehaAttendancePercentage: number;
  averagePrayerScore: number | null;
  averageReadingScore: number | null;
  averageNoteScore: number | null;
  totalConfessionsCount: number;
  lastConfessionDate?: string | null;
  recentVisits: StudentRecentVisitSummary[];
}

export interface ConfigurationWarning {
  code: string;
  title: string;
  message: string;
  severity: 'WARNING' | 'INFO';
  actionLabel: string;
  actionUrl: string;
  count: number;
}

export interface AdminSetupResponse {
  totalMinistries: number;
  totalClasses: number;
  totalServants: number;
  totalStudents: number;
  servantsWithAccount: number;
  servantsWithoutAccount: number;
  ministriesWithoutSecretary: number;
  classesWithoutSecretary: number;
  studentsWithoutServant: number;
  hasMinistry: boolean;
  hasClasses: boolean;
  hasServiceSecretary: boolean;
  hasClassSecretary: boolean;
  hasServants: boolean;
  hasStudents: boolean;
  hasAssignments: boolean;
  setupComplete: boolean;
  warnings: ConfigurationWarning[];
}

