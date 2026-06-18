import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import LoadingSpinner from './LoadingSpinner';

const ProtectedRoute = ({ children, requiredRole }) => {
  const { user, loading, isAuthenticated } = useAuth();

  if (loading) {
    return <LoadingSpinner />;
  }

  if (!isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }

  if (requiredRole) {
    const userRole = user?.role;
    const hasRole = userRole === requiredRole || 
                    userRole === `ROLE_${requiredRole}` ||
                    (requiredRole === 'ADMIN' && userRole === 'ROLE_ADMIN') ||
                    (requiredRole === 'STUDENT' && userRole === 'ROLE_STUDENT');
    if (!hasRole) {
      return <Navigate to="/dashboard" replace />;
    }
  }

  return children;
};

export default ProtectedRoute;
