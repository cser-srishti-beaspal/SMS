import React, { useEffect, useState } from 'react';
import api from '../api/axiosConfig';
import './Requests.css';

const MyRequests = () => {
  const [requests, setRequests] = useState([]);
  const [status, setStatus] = useState('');
  const [sortBy, setSortBy] = useState('dateDesc');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const loadRequests = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await api.get('/api/requests/my', {
        params: status ? { status } : {},
      });
      setRequests(response.data || []);
    } catch (err) {
      setError('Failed to load your requests.');
      setRequests([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadRequests();
  }, [status]);

  const getSortedRequests = () => {
    return [...requests].sort((a, b) => {
      if (sortBy === 'dateDesc') {
        return new Date(b.createdAt || b.updatedAt || 0) - new Date(a.createdAt || a.updatedAt || 0);
      }
      if (sortBy === 'dateAsc') {
        return new Date(a.createdAt || a.updatedAt || 0) - new Date(b.createdAt || b.updatedAt || 0);
      }
      if (sortBy === 'nameAsc') {
        const nameA = (a.items && a.items[0]?.itemName) || '';
        const nameB = (b.items && b.items[0]?.itemName) || '';
        return nameA.localeCompare(nameB);
      }
      if (sortBy === 'nameDesc') {
        const nameA = (a.items && a.items[0]?.itemName) || '';
        const nameB = (b.items && b.items[0]?.itemName) || '';
        return nameB.localeCompare(nameA);
      }
      return 0;
    });
  };

  return (
    <div className="page-card">
      <div className="page-header">
        <div>
          <h1>My Requests</h1>
          <p className="page-subtitle">Track requests you have submitted.</p>
        </div>
      </div>

      <div className="request-filter">
        <label>
          Filter by status
          <select value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="">All</option>
            <option value="PENDING">Pending</option>
            <option value="APPROVED">Approved</option>
            <option value="REJECTED">Rejected</option>
            <option value="FULFILLED">Fulfilled</option>
          </select>
        </label>

        <label>
          Sort by
          <select value={sortBy} onChange={(e) => setSortBy(e.target.value)}>
            <option value="dateDesc">Date (Newest First)</option>
            <option value="dateAsc">Date (Oldest First)</option>
            <option value="nameAsc">Item Name (A-Z)</option>
            <option value="nameDesc">Item Name (Z-A)</option>
          </select>
        </label>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="table-wrapper">
        <table className="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Request ID</th>
              <th>Status</th>
              <th>Items</th>
              <th>Admin</th>
              <th>Remarks / Reason</th>
              <th>Updated</th>
            </tr>
          </thead>
          <tbody>
            {requests.length ? (
              getSortedRequests().map((request) => (
                <tr key={request.id}>
                  <td>{request.id}</td>
                  <td>{request.requestId}</td>
                  <td>
                    <span className={`status-badge status-${request.status.toLowerCase()}`}>
                      {request.status}
                    </span>
                  </td>
                  <td>{request.items?.map((item) => `${item.itemName} x${item.quantity}`).join(', ')}</td>
                  <td>{request.adminUsername || '—'}</td>
                  <td style={{ color: request.status === 'REJECTED' ? '#f87171' : 'var(--text-muted)' }}>
                    {request.status === 'REJECTED' 
                      ? (request.rejectionReason || 'No reason specified') 
                      : '—'}
                  </td>
                  <td>{request.updatedAt ? new Date(request.updatedAt).toLocaleString() : '—'}</td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan="7" className="empty-row">
                  {loading ? 'Loading requests...' : 'No requests found.'}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default MyRequests;
