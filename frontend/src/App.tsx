import { Navigate, Route, Routes } from 'react-router-dom'
import { ProtectedRoute } from './auth/ProtectedRoute'
import { AdminRoute } from './auth/AdminRoute'
import { AppLayout } from './layouts/AppLayout'
import { AdminLayout } from './layouts/AdminLayout'
import { ProjectLayout } from './layouts/ProjectLayout'
import { LoginPage } from './pages/LoginPage'
import { HomePage } from './pages/HomePage'
import { ProjectsIndexPage } from './pages/ProjectsIndexPage'
import { ProjectTasksPage } from './pages/ProjectTasksPage'
import { ProjectReleasesPage } from './pages/ProjectReleasesPage'
import { AdminProjectsTab } from './pages/admin/AdminProjectsTab'
import { AdminUsersTab } from './pages/admin/AdminUsersTab'
import { AdminSettingsTab } from './pages/admin/AdminSettingsTab'
import { MonitoringPage } from './pages/MonitoringPage'

export function App() {
  return (
    <Routes>
      {/* 공개 회원가입 화면은 없다. 계정은 관리자가 /admin/users 에서 만든다. */}
      <Route path="/login" element={<LoginPage />} />

      {/* TaskBoard 앱 */}
      <Route
        element={
          <ProtectedRoute>
            <AppLayout />
          </ProtectedRoute>
        }
      >
        <Route path="/home" element={<HomePage />} />
        <Route path="/projects" element={<ProjectsIndexPage />} />
        <Route path="/projects/:workspaceId" element={<ProjectLayout />}>
          <Route index element={<ProjectTasksPage />} />
          <Route path="releases" element={<ProjectReleasesPage />} />
        </Route>
      </Route>

      {/* 관리자 앱 — TaskBoard와 셸을 공유하지 않는다 */}
      <Route
        element={
          <AdminRoute>
            <AdminLayout />
          </AdminRoute>
        }
      >
        <Route path="/admin" element={<AdminProjectsTab />} />
        <Route path="/admin/users" element={<AdminUsersTab />} />
        <Route path="/admin/settings" element={<AdminSettingsTab />} />
        <Route path="/admin/monitoring" element={<MonitoringPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/home" replace />} />
    </Routes>
  )
}
