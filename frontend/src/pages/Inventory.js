import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/axiosConfig';
import './Inventory.css';

const Inventory = () => {
  const [items, setItems] = useState([]);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const loadItems = async () => {
    setLoading(true);
    setError('');

    try {
      const url = search ? '/api/inventory/search' : '/api/inventory';
      const response = search
        ? await api.get(url, { params: { keyword: search } })
        : await api.get(url, { params: { page, size, sortBy: 'name' } });

      if (search) {
        setItems(response.data || []);
        setTotal(response.data?.length ?? 0);
      } else {
        setItems(response.data?.content || []);
        setTotal(response.data?.totalElements ?? 0);
      }
    } catch (err) {
      setError('Failed to load inventory.');
      setItems([]);
      setTotal(0);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadItems();
  }, [page, size]);

  const handleSearch = async (e) => {
    e.preventDefault();
    await loadItems();
  };

  return (
    <div className="page-card">
      <div className="page-header">
        <div>
          <h1>Inventory</h1>
          <p className="page-subtitle">Browse stationery items and search by name.</p>
        </div>
        <div className="page-actions">
          <Link to="/inventory/add" className="btn btn-primary">
            Add New Item
          </Link>
        </div>
      </div>

      <form className="page-search" onSubmit={handleSearch}>
        <input
          type="text"
          placeholder="Search items by name"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="input-search"
        />
        <button type="submit" className="btn btn-secondary">
          Search
        </button>
      </form>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="table-wrapper">
        <table className="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th>Category</th>
              <th>Quantity</th>
              <th>Min Qty</th>
              <th>Unit</th>
              <th>Description</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {items.length ? (
              items.map((item) => (
                <tr key={item.id}>
                  <td>{item.id}</td>
                  <td>{item.name}</td>
                  <td>{item.category}</td>
                  <td>{item.availableQuantity}</td>
                  <td>{item.minimumQuantity}</td>
                  <td>{item.unit}</td>
                  <td>{item.description || '—'}</td>
                  <td>
                    <Link to={`/inventory/edit/${item.id}`} className="action-link">
                      Edit
                    </Link>
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan="8" className="empty-row">
                  {loading ? 'Loading items...' : 'No items found.'}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {!search && (
        <div className="pagination-controls">
          <button
            className="pagination-btn"
            disabled={page === 0}
            onClick={() => setPage((prev) => Math.max(prev - 1, 0))}
          >
            Previous
          </button>
          <span>
            Page {page + 1} • {total} items
          </span>
          <button
            className="pagination-btn"
            disabled={(page + 1) * size >= total}
            onClick={() => setPage((prev) => prev + 1)}
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
};

export default Inventory;
