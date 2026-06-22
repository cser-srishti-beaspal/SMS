import React, { useEffect, useState } from 'react';
import api from '../api/axiosConfig';
import './Requests.css';

const ManageRequests = () => {
  const [requests, setRequests] = useState([]);
  const [status, setStatus] = useState('');
  const [sortBy, setSortBy] = useState('dateDesc');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  const loadRequests = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await api.get('/api/requests', {
        params: status ? { status } : {},
      });
      setRequests(response.data || []);
    } catch (err) {
      setError('Failed to load requests.');
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
        const nameA = a.studentUsername || '';
        const nameB = b.studentUsername || '';
        return nameA.localeCompare(nameB);
      }
      if (sortBy === 'nameDesc') {
        const nameA = a.studentUsername || '';
        const nameB = b.studentUsername || '';
        return nameB.localeCompare(nameA);
      }
      return 0;
    });
  };

  const updateRequest = async (id, action) => {
    setMessage('');
    setError('');

    let payload = {};
    if (action === 'reject') {
      const reason = window.prompt('Enter the reason for rejection:');
      if (reason === null) return; // User cancelled the prompt
      if (!reason.trim()) {
        setError('A rejection reason is required.');
        return;
      }
      payload = { rejectionReason: reason.trim() };
    }

    try {
      const url = `/api/requests/${id}/${action}`;
      await api.put(url, payload);
      setMessage(`Request ${action}ed successfully.`);
      loadRequests();
    } catch (err) {
      setError(`Failed to ${action} request.`);
    }
  };

  return (
    <div className="page-card">
      <div className="page-header">
        <div>
          <h1>Manage Requests</h1>
          <p className="page-subtitle">Approve, reject, or fulfill student requests.</p>
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
            <option value="nameAsc">Student Username (A-Z)</option>
            <option value="nameDesc">Student Username (Z-A)</option>
          </select>
        </label>
      </div>

      {message && <div className="alert alert-success">{message}</div>}
      {error && <div className="alert alert-error">{error}</div>}

      <div className="table-wrapper">
        <table className="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Request ID</th>
              <th>Student</th>
              <th>Status</th>
              <th>Items</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {requests.length ? (
              getSortedRequests().map((request) => (
                <tr key={request.id}>
                  <td>{request.id}</td>
                  <td>{request.requestId}</td>
                  <td>{request.studentUsername}</td>
                  <td>
                    <span className={`status-badge status-${request.status.toLowerCase()}`}>
                      {request.status}
                    </span>
                  </td>
                  <td>{request.items?.map((item) => `${item.itemName} x${item.quantity}`).join(', ')}</td>
                  <td className="action-cell">
                    {request.status === 'PENDING' && (
                      <>
                        <button className="action-btn approve" onClick={() => updateRequest(request.id, 'approve')}>
                          Approve
                        </button>
                        <button className="action-btn reject" onClick={() => updateRequest(request.id, 'reject')}>
                          Reject
                        </button>
                      </>
                    )}
                    {request.status === 'APPROVED' && (
                      <button className="action-btn fulfill" onClick={() => updateRequest(request.id, 'fulfill')}>
                        Fulfill
                      </button>
                    )}
                    {request.status !== 'PENDING' && request.status !== 'APPROVED' && '—'}
                  </td>
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

export default ManageRequests;
