import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { LoginPage } from './auth/LoginPage';
import { ProtectedRoute } from './auth/ProtectedRoute';
import { AppLayout } from './layout/AppLayout';
import { DashboardPage } from './pages/DashboardPage';
import { VisitsPage } from './pages/VisitsPage';
import { AttendancePage } from './pages/AttendancePage';
import { ConfessionsPage } from './pages/ConfessionsPage';
import { StudentsPage } from './pages/StudentsPage';
import { ServantsPage } from './pages/ServantsPage';
import { MinistriesPage } from './pages/MinistriesPage';
import { StatisticsPage } from './pages/StatisticsPage';
import { AuditLogsPage } from './pages/AuditLogsPage';
import { ArchivePage } from './pages/ArchivePage';
import { SettingsPage } from './pages/SettingsPage';
import { UsersPage } from './pages/UsersPage';
import { NotificationsPage } from './pages/NotificationsPage';
import { AboutPage } from './pages/AboutPage';
import { SelfFollowUpPage } from './pages/SelfFollowUpPage';
import { useAuth } from './auth/useAuth';

const AboutPageRoute: React.FC = () => {
  const { isAuthenticated, isLoading } = useAuth();

  if (isLoading) {
    return (
      <div className="flex h-screen items-center justify-center bg-gray-50">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-primary-600 border-t-transparent" />
      </div>
    );
  }

  if (isAuthenticated) {
    return (
      <AppLayout>
        <AboutPage />
      </AppLayout>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50/50">
      <div className="max-w-6xl mx-auto px-4 py-6 sm:px-6 lg:px-8">
        <AboutPage isPublic />
      </div>
    </div>
  );
};

export const App: React.FC = () => {
  return (
    <Routes>
      {/* Public Routes */}
      <Route path="/login" element={<LoginPage />} />
      <Route path="/about" element={<AboutPageRoute />} />

      {/* Protected Routes */}
      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/self-followup" element={
            <ProtectedRoute requiredRoles={['GENERAL_ADMIN', 'CLASS_SECRETARY', 'SERVANT']} />
          }>
            <Route index element={<SelfFollowUpPage />} />
          </Route>
          <Route path="/visits" element={<VisitsPage />} />
          <Route path="/attendance" element={<AttendancePage />} />
          <Route path="/confessions" element={<ConfessionsPage />} />
          <Route path="/students" element={<StudentsPage />} />
          <Route path="/all-students" element={<StudentsPage />} />
          <Route path="/notifications" element={<NotificationsPage />} />
          
          {/* Admin & Secretary Routes */}
          <Route
            path="/servants"
            element={
              <ProtectedRoute requiredRoles={['GENERAL_ADMIN', 'SERVICE_SECRETARY', 'CLASS_SECRETARY']} />
            }
          >
            <Route index element={<ServantsPage />} />
          </Route>

          <Route
            path="/statistics"
            element={
              <ProtectedRoute requiredRoles={['GENERAL_ADMIN', 'SERVICE_SECRETARY', 'CLASS_SECRETARY', 'SERVANT']} />
            }
          >
            <Route index element={<StatisticsPage />} />
          </Route>

          {/* Admin & Service Secretary Routes */}
          <Route path="/ministries" element={<ProtectedRoute requiredRoles={['GENERAL_ADMIN', 'SERVICE_SECRETARY']} />}>
            <Route index element={<MinistriesPage />} />
          </Route>

          <Route path="/users" element={<ProtectedRoute requiredRoles={['GENERAL_ADMIN']} />}>
            <Route index element={<UsersPage />} />
          </Route>

          <Route path="/audit-logs" element={<ProtectedRoute requiredRoles={['GENERAL_ADMIN']} />}>
            <Route index element={<AuditLogsPage />} />
          </Route>

          <Route path="/archive" element={<ArchivePage />} />

          <Route path="/settings" element={<ProtectedRoute requiredRoles={['GENERAL_ADMIN']} />}>
            <Route index element={<SettingsPage />} />
          </Route>
        </Route>
      </Route>

      {/* Fallback */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
};
