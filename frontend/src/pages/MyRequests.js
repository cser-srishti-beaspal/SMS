import React, { useEffect, useState } from 'react';
import api from '../api/axiosConfig';
import './Requests.css';

const MyRequests = () => {
  const [requests, setRequests] = useState([]);
  const [status, setStatus] = useState('');
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
              <th>Updated</th>
            </tr>
          </thead>
          <tbody>
            {requests.length ? (
              requests.map((request) => (
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
                  <td>{request.updatedAt ? new Date(request.updatedAt).toLocaleString() : '—'}</td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan="6" className="empty-row">
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
