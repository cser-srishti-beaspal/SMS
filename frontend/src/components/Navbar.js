import React, { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './Navbar.css';

const Navbar = () => {
  const { user, logout, isAdmin } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [mobileOpen, setMobileOpen] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const toggleMobile = () => {
    setMobileOpen(!mobileOpen);
  };

  const closeMobile = () => {
    setMobileOpen(false);
  };

  const isActive = (path) => location.pathname === path;

  const adminLinks = [
    { path: '/dashboard', label: 'Dashboard', icon: '📊' },
    { path: '/inventory', label: 'Inventory', icon: '📦' },
    { path: '/requests/manage', label: 'All Requests', icon: '📋' },
  ];

  const studentLinks = [
    { path: '/dashboard', label: 'Dashboard', icon: '📊' },
    { path: '/inventory', label: 'Inventory', icon: '📦' },
    { path: '/requests/new', label: 'New Request', icon: '✏️' },
    { path: '/requests/my', label: 'My Requests', icon: '📄' },
  ];

  const navLinks = isAdmin() ? adminLinks : studentLinks;

  return (
    <nav className="navbar">
      <div className="navbar-container">
        {/* Brand */}
        <Link to="/dashboard" className="navbar-brand" onClick={closeMobile}>
          <span className="brand-icon">📦</span>
          <div className="brand-text">
            <span className="brand-name">SMS</span>
            <span className="brand-subtitle">Stationery Management</span>
          </div>
        </Link>

        {/* Desktop Navigation */}
        <div className={`navbar-links ${mobileOpen ? 'mobile-open' : ''}`}>
          {navLinks.map((link) => (
            <Link
              key={link.path}
              to={link.path}
              className={`nav-link ${isActive(link.path) ? 'active' : ''}`}
              onClick={closeMobile}
            >
              <span className="nav-link-icon">{link.icon}</span>
              <span className="nav-link-label">{link.label}</span>
              {isActive(link.path) && <span className="active-indicator" />}
            </Link>
          ))}
        </div>

        {/* User Section */}
        <div className="navbar-user">
          <div className="user-info">
            <span className="user-avatar">
              {user?.username?.charAt(0).toUpperCase() || 'U'}
            </span>
            <div className="user-details">
              <span className="user-name">{user?.username}</span>
              <span className={`role-badge ${user?.role?.toLowerCase()}`}>
                {user?.role}
              </span>
            </div>
          </div>
          <button className="logout-btn" onClick={handleLogout} title="Logout">
            <span className="logout-icon">⏻</span>
            <span className="logout-text">Logout</span>
          </button>
        </div>

        {/* Mobile Hamburger */}
        <button
          className={`hamburger ${mobileOpen ? 'open' : ''}`}
          onClick={toggleMobile}
          aria-label="Toggle navigation"
        >
          <span className="hamburger-line" />
          <span className="hamburger-line" />
          <span className="hamburger-line" />
        </button>
      </div>

      {/* Mobile Overlay */}
      {mobileOpen && <div className="mobile-overlay" onClick={closeMobile} />}
    </nav>
  );
};

export default Navbar;
