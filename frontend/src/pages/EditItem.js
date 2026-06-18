import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import api from '../api/axiosConfig';
import './FormPage.css';

const EditItem = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [form, setForm] = useState({
    name: '',
    category: '',
    unit: '',
    availableQuantity: '',
    minimumQuantity: '',
    description: '',
  });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const loadItem = async () => {
      setLoading(true);
      setError('');
      try {
        const response = await api.get(`/api/inventory/${id}`);
        const item = response.data;
        setForm({
          name: item.name || '',
          category: item.category || '',
          unit: item.unit || '',
          availableQuantity: item.availableQuantity ?? '',
          minimumQuantity: item.minimumQuantity ?? '',
          description: item.description || '',
        });
      } catch (err) {
        setError('Unable to load item details.');
      } finally {
        setLoading(false);
      }
    };

    loadItem();
  }, [id]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    if (!form.name || !form.category || !form.unit || form.availableQuantity === '' || form.minimumQuantity === '') {
      setError('Please fill in all required fields.');
      return;
    }

    setLoading(true);
    try {
      await api.put(`/api/inventory/${id}`, {
        name: form.name,
        category: form.category,
        unit: form.unit,
        availableQuantity: Number(form.availableQuantity),
        minimumQuantity: Number(form.minimumQuantity),
        description: form.description,
      });
      setSuccess('Item updated successfully.');
      setTimeout(() => navigate('/inventory'), 1000);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update item.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page-card form-card">
      <div className="page-header">
        <div>
          <h1>Edit Inventory Item</h1>
          <p className="page-subtitle">Update the details of your stationery item.</p>
        </div>
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      <form className="form-grid" onSubmit={handleSubmit}>
        <label>
          Name <span className="required">*</span>
          <input name="name" value={form.name} onChange={handleChange} disabled={loading} />
        </label>
        <label>
          Category <span className="required">*</span>
          <input name="category" value={form.category} onChange={handleChange} disabled={loading} />
        </label>
        <label>
          Unit <span className="required">*</span>
          <input name="unit" value={form.unit} onChange={handleChange} disabled={loading} />
        </label>
        <label>
          Available Quantity <span className="required">*</span>
          <input name="availableQuantity" type="number" min="0" value={form.availableQuantity} onChange={handleChange} disabled={loading} />
        </label>
        <label>
          Minimum Quantity <span className="required">*</span>
          <input name="minimumQuantity" type="number" min="0" value={form.minimumQuantity} onChange={handleChange} disabled={loading} />
        </label>
        <label className="full-width">
          Description
          <textarea name="description" value={form.description} onChange={handleChange} disabled={loading} />
        </label>

        <div className="form-actions full-width">
          <button type="submit" className="btn btn-primary" disabled={loading}>
            {loading ? 'Updating...' : 'Update Item'}
          </button>
        </div>
      </form>
    </div>
  );
};

export default EditItem;
