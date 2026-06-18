import React, { createContext, useState, useContext, useEffect, useCallback } from 'react';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  // Restore user from localStorage on mount
  useEffect(() => {
    try {
      const token = localStorage.getItem('sms_token');
      const username = localStorage.getItem('sms_user');
      const role = localStorage.getItem('sms_role');

      if (token && username && role) {
        setUser({ token, username, role });
      }
    } catch (err) {
      console.error('Error restoring auth state:', err);
      localStorage.removeItem('sms_token');
      localStorage.removeItem('sms_user');
      localStorage.removeItem('sms_role');
    } finally {
      setLoading(false);
    }
  }, []);

  const login = useCallback((token, username, role) => {
    const normalizedRole = role ? role.toUpperCase().replace('ROLE_', '') : 'STUDENT';
    localStorage.setItem('sms_token', token);
    localStorage.setItem('sms_user', username);
    localStorage.setItem('sms_role', normalizedRole);
    setUser({ token, username, role: normalizedRole });
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('sms_token');
    localStorage.removeItem('sms_user');
    localStorage.removeItem('sms_role');
    setUser(null);
  }, []);

  const isAdmin = useCallback(() => {
    return user?.role === 'ADMIN';
  }, [user]);

  const isStudent = useCallback(() => {
    return user?.role === 'STUDENT';
  }, [user]);

  const isAuthenticated = useCallback(() => {
    return !!user?.token;
  }, [user]);

  const value = {
    user,
    loading,
    login,
    logout,
    isAdmin,
    isStudent,
    isAuthenticated,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export default AuthContext;
