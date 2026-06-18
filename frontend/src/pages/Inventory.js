import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/axiosConfig';
import { useAuth } from '../context/AuthContext';
import './Inventory.css';

const Inventory = () => {
  const { user, isAdmin } = useAuth();
  const [items, setItems] = useState([]);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [lowStockItems, setLowStockItems] = useState([]);

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

  const loadLowStock = async () => {
    if (isAdmin()) {
      try {
        const response = await api.get('/api/inventory/low-stock');
        setLowStockItems(response.data || []);
      } catch (err) {
        console.error('Failed to load low stock items:', err);
      }
    }
  };

  useEffect(() => {
    loadItems();
  }, [page, size]);

  useEffect(() => {
    loadLowStock();
  }, [user]);

  const handleSearch = async (e) => {
    e.preventDefault();
    await loadItems();
  };

  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this item?')) {
      setError('');
      setSuccess('');
      try {
        await api.delete(`/api/inventory/${id}`);
        setSuccess('Item deleted successfully.');
        loadItems();
        loadLowStock();
      } catch (err) {
        setError('Failed to delete item.');
      }
    }
  };

  const addToCart = (item) => {
    setError('');
    setSuccess('');
    try {
      const existingCart = JSON.parse(localStorage.getItem('sms_cart') || '[]');
      const existingItemIdx = existingCart.findIndex(cartItem => String(cartItem.itemId) === String(item.id));
      
      if (existingItemIdx > -1) {
        existingCart[existingItemIdx].quantity += 1;
      } else {
        existingCart.push({
          itemId: item.id,
          itemName: item.name,
          availableQuantity: item.availableQuantity,
          quantity: 1
        });
      }
      
      localStorage.setItem('sms_cart', JSON.stringify(existingCart));
      setSuccess(`${item.name} added to cart! Go to 'New Request' to submit.`);
      setTimeout(() => setSuccess(''), 3000);
    } catch (err) {
      setError('Failed to add item to cart.');
    }
  };

  return (
    <div className="page-card">
      <div className="page-header">
        <div>
          <h1>Inventory</h1>
          <p className="page-subtitle">Browse stationery items and search by name.</p>
        </div>
        {isAdmin() && (
          <div className="page-actions">
            <Link to="/inventory/add" className="btn btn-primary">
              Add New Item
            </Link>
          </div>
        )}
      </div>

      {/* Low stock alert section for administrators */}
      {isAdmin() && lowStockItems.length > 0 && (
        <div className="alert alert-error low-stock-alert-section" style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1.5rem', background: 'rgba(239, 68, 68, 0.12)', borderColor: 'rgba(239, 68, 68, 0.25)', color: '#fca5a5' }}>
          <span style={{ fontSize: '1.25rem' }}>⚠️</span>
          <div>
            <strong>Low Stock Warning:</strong> The following items are at or below minimum quantities: {' '}
            {lowStockItems.map(item => `${item.name} (${item.availableQuantity} left)`).join(', ')}
          </div>
        </div>
      )}

      {error && <div className="alert alert-error">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

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

      <div className="table-wrapper">
        <table className="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th>Category</th>
              <th>Quantity</th>
              {isAdmin() && <th>Min Qty</th>}
              <th>Unit</th>
              <th>Description</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {items.length ? (
              items.map((item) => (
                <tr 
                  key={item.id} 
                  className={isAdmin() && item.availableQuantity <= item.minimumQuantity ? 'low-stock-row' : ''}
                >
                  <td>{item.id}</td>
                  <td>{item.name}</td>
                  <td>{item.category}</td>
                  <td>{item.availableQuantity}</td>
                  {isAdmin() && <td>{item.minimumQuantity}</td>}
                  <td>{item.unit}</td>
                  <td>{item.description || '—'}</td>
                  <td>
                    {isAdmin() ? (
                      <div style={{ display: 'flex', gap: '0.75rem' }}>
                        <Link to={`/inventory/edit/${item.id}`} className="action-link">
                          Edit
                        </Link>
                        <button 
                          onClick={() => handleDelete(item.id)} 
                          className="action-link btn-link-danger"
                          style={{ background: 'none', border: 'none', padding: 0, font: 'inherit', cursor: 'pointer', fontWeight: 600 }}
                        >
                          Delete
                        </button>
                      </div>
                    ) : (
                      <button 
                        onClick={() => addToCart(item)} 
                        className="btn btn-secondary"
                        style={{ padding: '0.45rem 0.85rem', fontSize: '0.8125rem' }}
                        disabled={item.availableQuantity < 1}
                      >
                        {item.availableQuantity < 1 ? 'Out of Stock' : 'Add to Cart'}
                      </button>
                    )}
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan={isAdmin() ? "8" : "7"} className="empty-row">
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
