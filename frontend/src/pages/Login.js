import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../api/axiosConfig';
import './Login.css';

const Login = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();

  // Redirect if already logged in
  if (isAuthenticated()) {
    navigate('/dashboard', { replace: true });
    return null;
  }

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!email.trim() || !password.trim()) {
      setError('Please fill in all fields');
      return;
    }

    setLoading(true);
    try {
      const response = await api.post('/api/auth/login', {
        email: email.trim(),
        password,
      });

      const data = response.data;
      const token = data.token || data.jwt || data.accessToken;
      const role = data.role || 'STUDENT';
      const user = data.username || email.trim();

      if (token) {
        login(token, user, role);
        navigate('/dashboard', { replace: true });
      } else {
        setError('Invalid response from server');
      }
    } catch (err) {
      if (err.response) {
        const msg = err.response.data?.message || err.response.data?.error;
        setError(msg || `Login failed (${err.response.status})`);
      } else if (err.request) {
        setError('Unable to reach server. Please check your connection.');
      } else {
        setError('An unexpected error occurred');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      {/* Background decoration */}
      <div className="login-bg-decor">
        <div className="bg-orb bg-orb-1" />
        <div className="bg-orb bg-orb-2" />
        <div className="bg-orb bg-orb-3" />
      </div>

      <div className="login-card">
        <div className="login-header">
          <div className="login-logo">📦</div>
          <h1 className="login-title">Welcome Back</h1>
          <p className="login-subtitle">Sign in to your SMS account</p>
        </div>

        {error && (
          <div className="login-error">
            <span className="error-icon">⚠️</span>
            <span>{error}</span>
          </div>
        )}

        <form className="login-form" onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label" htmlFor="email">
              <span className="label-icon">✉️</span>
              Email Address
            </label>
            <input
              id="email"
              type="email"
              className="form-input"
              placeholder="Enter your email address"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              disabled={loading}
              autoComplete="email"
              autoFocus
            />
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="password">
              <span className="label-icon">🔒</span>
              Password
            </label>
            <div className="password-wrapper">
              <input
                id="password"
                type={showPassword ? 'text' : 'password'}
                className="form-input"
                placeholder="Enter your password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                disabled={loading}
                autoComplete="current-password"
              />
              <button
                type="button"
                className="password-toggle"
                onClick={() => setShowPassword(!showPassword)}
                tabIndex={-1}
              >
                {showPassword ? '🙈' : '👁️'}
              </button>
            </div>
          </div>

          <button
            type="submit"
            className={`login-btn ${loading ? 'loading' : ''}`}
            disabled={loading}
          >
            {loading ? (
              <span className="btn-spinner" />
            ) : (
              <>
                <span>Sign In</span>
                <span className="btn-arrow">→</span>
              </>
            )}
          </button>
        </form>

        <div className="login-footer">
          <p>
            Don't have an account?{' '}
            <Link to="/register" className="register-link">
              Create one
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
};

export default Login;
