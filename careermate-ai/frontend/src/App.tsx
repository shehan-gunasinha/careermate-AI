import { Navigate, Route, Routes } from 'react-router-dom';
import Layout from './components/Layout';
import ProtectedRoute from './components/ProtectedRoute';
import Applications from './pages/Applications';
import Assistant from './pages/Assistant';
import Dashboard from './pages/Dashboard';
import JobDetail from './pages/JobDetail';
import Jobs from './pages/Jobs';
import Landing from './pages/Landing';
import Login from './pages/Login';
import Notifications from './pages/Notifications';
import Profile from './pages/Profile';
import Recommended from './pages/Recommended';
import Register from './pages/Register';
import ResumeAnalysisPage from './pages/ResumeAnalysis';
import ResumeBuilder from './pages/ResumeBuilder';
import Resumes from './pages/Resumes';
import Settings from './pages/Settings';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Landing />} />
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<Layout />}>
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/jobs" element={<Jobs />} />
          <Route path="/jobs/:id" element={<JobDetail />} />
          <Route path="/recommended" element={<Recommended />} />
          <Route path="/resumes" element={<Resumes />} />
          <Route path="/resumes/:id/analysis" element={<ResumeAnalysisPage />} />
          <Route path="/resume-builder" element={<ResumeBuilder />} />
          <Route path="/applications" element={<Applications />} />
          <Route path="/assistant" element={<Assistant />} />
          <Route path="/notifications" element={<Notifications />} />
          <Route path="/profile" element={<Profile />} />
          <Route path="/settings" element={<Settings />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
