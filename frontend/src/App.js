import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import Navbar from './components/Navbar';
import ProtectedRoute from './components/ProtectedRoute';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import Inventory from './pages/Inventory';
import AddItem from './pages/AddItem';
import EditItem from './pages/EditItem';
import CreateRequest from './pages/CreateRequest';
import MyRequests from './pages/MyRequests';
import ManageRequests from './pages/ManageRequests';
import './App.css';

function App() {
  return (
    <AuthProvider>
      <Router>
        <div className="app">
          <Routes>
            {/* Public Routes */}
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />

            {/* Protected Routes */}
            <Route
              path="/dashboard"
              element={
                <ProtectedRoute>
                  <Navbar />
                  <main className="main-content">
                    <Dashboard />
                  </main>
                </ProtectedRoute>
              }
            />
            <Route
              path="/inventory"
              element={
                <ProtectedRoute>
                  <Navbar />
                  <main className="main-content">
                    <Inventory />
                  </main>
                </ProtectedRoute>
              }
            />
            <Route
              path="/inventory/add"
              element={
                <ProtectedRoute requiredRole="ADMIN">
                  <Navbar />
                  <main className="main-content">
                    <AddItem />
                  </main>
                </ProtectedRoute>
              }
            />
            <Route
              path="/inventory/edit/:id"
              element={
                <ProtectedRoute requiredRole="ADMIN">
                  <Navbar />
                  <main className="main-content">
                    <EditItem />
                  </main>
                </ProtectedRoute>
              }
            />
            <Route
              path="/requests/new"
              element={
                <ProtectedRoute requiredRole="STUDENT">
                  <Navbar />
                  <main className="main-content">
                    <CreateRequest />
                  </main>
                </ProtectedRoute>
              }
            />
            <Route
              path="/requests/my"
              element={
                <ProtectedRoute requiredRole="STUDENT">
                  <Navbar />
                  <main className="main-content">
                    <MyRequests />
                  </main>
                </ProtectedRoute>
              }
            />
            <Route
              path="/requests/manage"
              element={
                <ProtectedRoute requiredRole="ADMIN">
                  <Navbar />
                  <main className="main-content">
                    <ManageRequests />
                  </main>
                </ProtectedRoute>
              }
            />

            {/* Default redirect */}
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </div>
      </Router>
    </AuthProvider>
  );
}

export default App;
