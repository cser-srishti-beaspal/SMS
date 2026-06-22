import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../api/axiosConfig';

const Dashboard = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [lowStockItems, setLowStockItems] = useState([]);
  const [showLowStock, setShowLowStock] = useState(false);
  const [auditLogs, setAuditLogs] = useState([]);
  const [showAuditLogs, setShowAuditLogs] = useState(false);
  const [stats, setStats] = useState({
    totalItems: 0,
    lowStock: 0,
    totalRequests: 0,
    pendingRequests: 0,
    myRequests: 0,
  });

  useEffect(() => {
    const loadStats = async () => {
      setLoading(true);
      setError('');

      try {
        const inventoryResponse = await api.get('/api/inventory', {
          params: { page: 0, size: 1, sortBy: 'name' },
        });

        const totalItems = inventoryResponse.data?.totalElements ?? 0;
        let lowStock = 0;
        let totalRequests = 0;
        let pendingRequests = 0;
        let myRequests = 0;

        if (user?.role === 'ADMIN') {
          const lowResponse = await api.get('/api/inventory/low-stock');
          const lowList = lowResponse.data ?? [];
          lowStock = Number(lowList.length);
          setLowStockItems(lowList);

          const allRequests = await api.get('/api/requests');
          totalRequests = Number(allRequests.data?.length ?? 0);
          pendingRequests = Number(
            allRequests.data?.filter((request) => request.status === 'PENDING')?.length ?? 0
          );

          try {
            const auditResponse = await api.get('/api/requests/audit-logs');
            setAuditLogs(auditResponse.data ?? []);
          } catch (auditErr) {
            console.error('Failed to load audit logs:', auditErr);
          }
        } else {
          const myResponse = await api.get('/api/requests/my');
          myRequests = Number(myResponse.data?.length ?? 0);
          pendingRequests = Number(
            myResponse.data?.filter((request) => request.status === 'PENDING')?.length ?? 0
          );
        }

        setStats({ totalItems, lowStock, totalRequests, pendingRequests, myRequests });
      } catch (err) {
        setError('Unable to load dashboard stats. Please try again.');
      } finally {
        setLoading(false);
      }
    };

    loadStats();
  }, [user?.role]);

  return (
    <div className="page-card">
      <div className="page-header">
        <div>
          <h1>Dashboard</h1>
          <p className="page-subtitle">
            {user?.role === 'ADMIN'
              ? 'Admin overview of inventory and request activity.'
              : 'Student overview of inventory and your requests.'}
          </p>
        </div>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="card-grid">
        {/* Inventory Items card - goes to /inventory */}
        <div 
          className="stat-card"
          onClick={() => navigate('/inventory')}
          style={{ cursor: 'pointer' }}
          title="Click to view full inventory"
        >
          <div className="stat-label">Inventory Items</div>
          <div className="stat-value">{stats.totalItems}</div>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: 'auto', display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
            ▶ View Inventory
          </span>
        </div>

        {user?.role === 'ADMIN' ? (
          <>
            {/* Low Stock Items card - clicks toggle details list in dashboard */}
            <div 
              className="stat-card" 
              onClick={() => setShowLowStock(!showLowStock)}
              style={{
                cursor: 'pointer',
                borderColor: showLowStock ? 'var(--accent-primary)' : '',
                boxShadow: showLowStock ? 'var(--shadow-glow-primary)' : '',
                background: showLowStock ? 'var(--bg-card-hover)' : ''
              }}
              title="Click to toggle details of low stock items"
            >
              <div className="stat-label">Low Stock Items ⚠️</div>
              <div className="stat-value" style={{ background: 'linear-gradient(135deg, #f87171, #ef4444)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
                {stats.lowStock}
              </div>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: 'auto', display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                {showLowStock ? '▼ Hide Details' : '▶ View Details'}
              </span>
            </div>

            {/* Total Requests card - goes to /requests/manage */}
            <div 
              className="stat-card"
              onClick={() => navigate('/requests/manage')}
              style={{ cursor: 'pointer' }}
              title="Click to manage all requests"
            >
              <div className="stat-label">Total Requests</div>
              <div className="stat-value">{stats.totalRequests}</div>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: 'auto', display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                ▶ Manage Requests
              </span>
            </div>

            {/* System Audit Logs card */}
            <div 
              className="stat-card" 
              onClick={() => setShowAuditLogs(!showAuditLogs)}
              style={{
                cursor: 'pointer',
                borderColor: showAuditLogs ? 'var(--accent-primary)' : '',
                boxShadow: showAuditLogs ? 'var(--shadow-glow-primary)' : '',
                background: showAuditLogs ? 'var(--bg-card-hover)' : ''
              }}
              title="Click to toggle details of system audit logs"
            >
              <div className="stat-label">System Audit Logs 📋</div>
              <div className="stat-value" style={{ color: 'var(--text-primary)' }}>
                {auditLogs.length}
              </div>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: 'auto', display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                {showAuditLogs ? '▼ Hide Logs' : '▶ View Logs'}
              </span>
            </div>
          </>
        ) : (
          <>
            {/* My Requests card - goes to /requests/my */}
            <div 
              className="stat-card"
              onClick={() => navigate('/requests/my')}
              style={{ cursor: 'pointer' }}
              title="Click to view your requests"
            >
              <div className="stat-label">My Requests</div>
              <div className="stat-value">{stats.myRequests}</div>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: 'auto', display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                ▶ View My Requests
              </span>
            </div>
          </>
        )}

        {/* Pending Requests card - goes to /requests/manage (Admin) or /requests/my (Student) */}
        <div 
          className="stat-card"
          onClick={() => navigate(user?.role === 'ADMIN' ? '/requests/manage' : '/requests/my')}
          style={{ cursor: 'pointer' }}
          title="Click to view pending requests"
        >
          <div className="stat-label">Pending Requests</div>
          <div className="stat-value">{stats.pendingRequests}</div>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: 'auto', display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
            ▶ View Pending
          </span>
        </div>
      </div>

      {showLowStock && user?.role === 'ADMIN' && (
        <div className="low-stock-detail-panel fade-in" style={{
          marginTop: '1.5rem',
          padding: '1.5rem',
          background: 'rgba(255, 255, 255, 0.02)',
          border: '1px solid rgba(255, 255, 255, 0.08)',
          borderRadius: 'var(--radius-lg)',
          backdropFilter: 'blur(16px)',
          animation: 'fadeIn 0.3s ease'
        }}>
          <h2 style={{ fontSize: '1.2rem', marginBottom: '1rem', color: '#f87171', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            ⚠️ Low Stock Alert Details
          </h2>
          {lowStockItems.length === 0 ? (
            <p style={{ color: 'var(--text-muted)' }}>No items are currently below their minimum quantities.</p>
          ) : (
            <div className="table-wrapper">
              <table className="data-table" style={{ minWidth: '100%' }}>
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Item Name</th>
                    <th>Category</th>
                    <th>Available Quantity</th>
                    <th>Minimum Quantity</th>
                    <th>Unit</th>
                  </tr>
                </thead>
                <tbody>
                  {lowStockItems.map((item) => (
                    <tr key={item.id}>
                      <td>{item.id}</td>
                      <td style={{ fontWeight: 'bold', color: 'var(--text-primary)' }}>{item.name}</td>
                      <td><span className="category-badge">{item.category}</span></td>
                      <td style={{ color: '#f87171', fontWeight: 'bold' }}>{item.availableQuantity}</td>
                      <td>{item.minimumQuantity}</td>
                      <td>{item.unit}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {showAuditLogs && user?.role === 'ADMIN' && (
        <div className="audit-logs-detail-panel fade-in" style={{
          marginTop: '1.5rem',
          padding: '1.5rem',
          background: 'rgba(255, 255, 255, 0.02)',
          border: '1px solid rgba(255, 255, 255, 0.08)',
          borderRadius: 'var(--radius-lg)',
          backdropFilter: 'blur(16px)',
          animation: 'fadeIn 0.3s ease'
        }}>
          <h2 style={{ fontSize: '1.2rem', marginBottom: '1rem', color: 'var(--accent-primary)', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            📋 System Audit Logs
          </h2>
          {auditLogs.length === 0 ? (
            <p style={{ color: 'var(--text-muted)' }}>No audit logs available.</p>
          ) : (
            <div className="table-wrapper" style={{ maxHeight: '500px', overflowY: 'auto' }}>
              <table className="data-table" style={{ minWidth: '100%' }}>
                <thead>
                  <tr>
                    <th>Time</th>
                    <th>Action (What)</th>
                    <th>Performed By (Who)</th>
                    <th>Details (Why/What changed)</th>
                  </tr>
                </thead>
                <tbody>
                  {auditLogs.map((log) => (
                    <tr key={log.id}>
                      <td style={{ whiteSpace: 'nowrap', fontSize: '0.85rem' }}>
                        {log.createdTime ? new Date(log.createdTime).toLocaleString() : '—'}
                      </td>
                      <td>
                        <span className={`status-badge status-${log.action.toLowerCase()}`} style={{
                          padding: '0.25rem 0.5rem',
                          borderRadius: '4px',
                          fontSize: '0.75rem',
                          fontWeight: 'bold',
                          textTransform: 'uppercase',
                          background: log.action.includes('APPROVED') ? 'rgba(52, 211, 153, 0.1)' : 
                                      log.action.includes('REJECTED') ? 'rgba(248, 113, 113, 0.1)' : 
                                      log.action.includes('CREATED') ? 'rgba(96, 165, 250, 0.1)' : 'rgba(251, 191, 36, 0.1)',
                          color: log.action.includes('APPROVED') ? '#34d399' : 
                                 log.action.includes('REJECTED') ? '#f87171' : 
                                 log.action.includes('CREATED') ? '#60a5fa' : '#fbbf24'
                        }}>
                          {log.action}
                        </span>
                      </td>
                      <td style={{ fontWeight: 'bold' }}>
                        {log.performedBy} <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 'normal' }}>({log.userRole})</span>
                      </td>
                      <td style={{ color: 'var(--text-secondary)', fontSize: '0.85rem' }}>{log.details}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {loading && <div className="page-loading">Loading dashboard...</div>}
    </div>
  );
};

export default Dashboard;
