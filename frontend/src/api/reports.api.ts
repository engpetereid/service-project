import { apiClient } from './client';

export interface StudentReportFilter {
  ministryId?: number;
  classId?: number;
  servantId?: number;
  academicYearId?: number;
  status?: string;
}

export interface VisitReportFilter {
  weekId?: number;
  ministryId?: number;
  classId?: number;
  servantId?: number;
}

export interface AttendanceReportFilter {
  weekId?: number;
  activityType?: string;
  ministryId?: number;
  classId?: number;
}

export interface ConfessionReportFilter {
  academicYearId?: number;
  ministryId?: number;
  classId?: number;
  studentId?: number;
  startDate?: string;
  endDate?: string;
}

export const downloadBlob = (data: BlobPart, filename: string, mimeType = 'text/csv;charset=utf-8;') => {
  const blob = new Blob([data], { type: mimeType });
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', filename);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
};

export const reportsApi = {
  exportStudents: async (filter?: StudentReportFilter): Promise<void> => {
    const res = await apiClient.get('/reports/export/students', {
      params: filter,
      responseType: 'blob',
    });
    downloadBlob(res.data, `students_report_${new Date().toISOString().slice(0, 10)}.csv`);
  },

  exportVisits: async (filter?: VisitReportFilter): Promise<void> => {
    const res = await apiClient.get('/reports/export/visits', {
      params: filter,
      responseType: 'blob',
    });
    downloadBlob(res.data, `visits_report_${new Date().toISOString().slice(0, 10)}.csv`);
  },

  exportAttendance: async (filter?: AttendanceReportFilter): Promise<void> => {
    const res = await apiClient.get('/reports/export/attendance', {
      params: filter,
      responseType: 'blob',
    });
    downloadBlob(res.data, `attendance_report_${new Date().toISOString().slice(0, 10)}.csv`);
  },

  exportConfessions: async (filter?: ConfessionReportFilter): Promise<void> => {
    const res = await apiClient.get('/reports/export/confession', {
      params: filter,
      responseType: 'blob',
    });
    downloadBlob(res.data, `confessions_report_${new Date().toISOString().slice(0, 10)}.csv`);
  },
};
