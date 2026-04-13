import { useState } from 'react';
import { useAuth } from './context/AuthContext';
import Login from './pages/Login';
import Layout from './components/Layout';
import StudentDashboard from './pages/StudentDashboard';
import InstructorDashboard from './pages/InstructorDashboard';
import AdminDashboard from './pages/AdminDashboard';

const DEFAULT_PAGE = {
  student:    'war',
  instructor: 'section-report',
  admin:      'sections',
};

export default function App() {
  const { user } = useAuth();
  const [page, setPage] = useState(null);

  // Reset page to role default when user changes
  const currentPage = page ?? (user ? DEFAULT_PAGE[user.role] : null);

  if (!user) return <Login />;

  function renderContent() {
    if (user.role === 'student')    return <StudentDashboard page={currentPage} />;
    if (user.role === 'instructor') return <InstructorDashboard page={currentPage} />;
    if (user.role === 'admin')      return <AdminDashboard page={currentPage} />;
    return null;
  }

  return (
    <Layout currentPage={currentPage} onNavigate={setPage}>
      {renderContent()}
    </Layout>
  );
}
