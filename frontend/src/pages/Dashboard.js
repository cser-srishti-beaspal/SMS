import React, { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import api from '../api/axiosConfig';

const Dashboard = () => {
  const { user } = useAuth();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
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
          lowStock = Number(lowResponse.data?.length ?? 0);

          const allRequests = await api.get('/api/requests');
          totalRequests = Number(allRequests.data?.length ?? 0);
          pendingRequests = Number(
            allRequests.data?.filter((request) => request.status === 'PENDING')?.length ?? 0
          );
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
        <div className="stat-card">
          <div className="stat-label">Inventory Items</div>
          <div className="stat-value">{stats.totalItems}</div>
        </div>

        {user?.role === 'ADMIN' ? (
          <>
            <div className="stat-card">
              <div className="stat-label">Low Stock Items</div>
              <div className="stat-value">{stats.lowStock}</div>
            </div>
            <div className="stat-card">
              <div className="stat-label">Total Requests</div>
              <div className="stat-value">{stats.totalRequests}</div>
            </div>
          </>
        ) : (
          <>
            <div className="stat-card">
              <div className="stat-label">My Requests</div>
              <div className="stat-value">{stats.myRequests}</div>
            </div>
          </>
        )}

        <div className="stat-card">
          <div className="stat-label">Pending Requests</div>
          <div className="stat-value">{stats.pendingRequests}</div>
        </div>
      </div>

      {loading && <div className="page-loading">Loading dashboard...</div>}
    </div>
  );
};

export default Dashboard;
