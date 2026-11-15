import React, { useState, useEffect } from 'react';
import { Boxes, Plus, AlertTriangle, ArrowUpRight, ArrowDownRight, Edit3, Trash2, X } from 'lucide-react';
import { api } from '../services/api';

const RESOURCE_CATEGORIES = ['FOOD', 'WATER', 'MEDICINES', 'FIRST_AID', 'EMERGENCY_SUPPLIES'];

export default function ResourceManagement() {
  const [resources, setResources] = useState([]);
  const [loading, setLoading] = useState(false);
  const [filterCategory, setFilterCategory] = useState('ALL');

  // Modals state
  const [showAddModal, setShowAddModal] = useState(false);
  const [showStockModal, setShowStockModal] = useState(false);
  const [currentResource, setCurrentResource] = useState(null);

  // Form state
  const [formData, setFormData] = useState({
    name: '',
    category: 'FOOD',
    availableStock: '50',
    unit: 'packs',
    unitWeightKg: '1.0'
  });
  const [stockDelta, setStockDelta] = useState(10);
  const [formError, setFormError] = useState('');
  const [notification, setNotification] = useState(null);

  const loadData = async () => {
    setLoading(true);
    try {
      const list = await api.getResources();
      setResources(list || []);
    } catch (err) {
      showToast('Error loading resources: ' + err.message, 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const showToast = (message, type = 'success') => {
    setNotification({ message, type });
    setTimeout(() => setNotification(null), 4000);
  };

  const handleOpenAdd = () => {
    setFormData({
      name: '',
      category: 'FOOD',
      availableStock: '50',
      unit: 'packs',
      unitWeightKg: '1.0'
    });
    setFormError('');
    setShowAddModal(true);
  };

  const handleOpenStockAdjust = (res) => {
    setCurrentResource(res);
    setStockDelta(10);
    setFormError('');
    setShowStockModal(true);
  };

  const handleAddSubmit = async (e) => {
    e.preventDefault();
    if (!formData.name.trim()) {
      setFormError('Resource name is required.');
      return;
    }
    const stock = parseInt(formData.availableStock, 10);
    const weight = parseFloat(formData.unitWeightKg);

    if (isNaN(stock) || stock < 0) {
      setFormError('Stock quantity must be non-negative.');
      return;
    }
    if (isNaN(weight) || weight <= 0) {
      setFormError('Unit weight must be positive.');
      return;
    }

    try {
      await api.createResource({
        name: formData.name.trim(),
        category: formData.category,
        availableStock: stock,
        unit: formData.unit.trim(),
        unitWeightKg: weight
      });
      setShowAddModal(false);
      showToast('Resource added to inventory!');
      loadData();
    } catch (err) {
      setFormError(err.message);
    }
  };

  const handleStockAdjustSubmit = async (e) => {
    e.preventDefault();
    try {
      const delta = parseInt(stockDelta, 10);
      if (isNaN(delta) || delta === 0) {
        setFormError('Please enter a non-zero adjustment amount.');
        return;
      }
      // Send PUT to /api/resources/{id} with stockAdjustment
      await fetch(`http://localhost:8080/api/resources/${currentResource.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ stockAdjustment: delta })
      });
      setShowStockModal(false);
      showToast(`Stock updated for ${currentResource.name}`);
      loadData();
    } catch (err) {
      setFormError(err.message);
    }
  };

  const handleDelete = async (id, name) => {
    if (!window.confirm(`Are you sure you want to delete resource ${name} (${id})?`)) return;
    try {
      await fetch(`http://localhost:8080/api/resources/${id}`, { method: 'DELETE' });
      showToast(`Resource ${id} deleted.`);
      loadData();
    } catch (err) {
      showToast('Failed to delete resource: ' + err.message, 'error');
    }
  };

  const lowStockItems = resources.filter((r) => r.availableStock <= 20);

  const filteredResources = resources.filter((r) => {
    return filterCategory === 'ALL' || r.category === filterCategory;
  });

  return (
    <div>
      {/* Toast Notification */}
      {notification && (
        <div
          style={{
            position: 'fixed',
            top: '20px',
            right: '20px',
            zIndex: 9999,
            backgroundColor: notification.type === 'error' ? '#ef4444' : '#10b981',
            color: 'white',
            padding: '12px 20px',
            borderRadius: '8px',
            boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
            fontWeight: 600,
            fontSize: '0.88rem'
          }}
        >
          {notification.message}
        </div>
      )}

      {/* Header Bar */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h3 style={{ fontSize: '1.25rem', fontWeight: 700, color: '#0f172a' }}>Relief Resource Inventory</h3>
          <p style={{ fontSize: '0.82rem', color: '#64748b' }}>
            Maintain essential disaster supplies stock: medicines, food, drinking water, first-aid kits.
          </p>
        </div>
        <button className="btn btn-primary" onClick={handleOpenAdd}>
          <Plus size={16} />
          <span>Add Resource Item</span>
        </button>
      </div>

      {/* Low-Stock Warning Alert Banner */}
      {lowStockItems.length > 0 && (
        <div style={{ backgroundColor: '#fffbeb', border: '1px solid #fde68a', borderRadius: '8px', padding: '14px 18px', marginBottom: '20px', display: 'flex', alignItems: 'center', gap: '12px' }}>
          <AlertTriangle size={22} color="#d97706" style={{ flexShrink: 0 }} />
          <div style={{ fontSize: '0.85rem', color: '#92400e' }}>
            <strong>Low Inventory Warning:</strong> {lowStockItems.length} resource item(s) are at or below critical threshold (&le; 20 units available):{' '}
            <strong>{lowStockItems.map((i) => `${i.name} (${i.availableStock} ${i.unit})`).join(', ')}</strong>. Consider restocking promptly.
          </div>
        </div>
      )}

      {/* Filter Bar */}
      <div className="card" style={{ padding: '14px 20px', marginBottom: '20px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flexWrap: 'wrap' }}>
          <span style={{ fontSize: '0.82rem', fontWeight: 600, color: '#475569' }}>Filter by Category:</span>
          <button
            className={`btn ${filterCategory === 'ALL' ? 'btn-primary' : 'btn-secondary'}`}
            style={{ padding: '4px 12px', fontSize: '0.78rem' }}
            onClick={() => setFilterCategory('ALL')}
          >
            All Categories ({resources.length})
          </button>
          {RESOURCE_CATEGORIES.map((cat) => (
            <button
              key={cat}
              className={`btn ${filterCategory === cat ? 'btn-primary' : 'btn-secondary'}`}
              style={{ padding: '4px 12px', fontSize: '0.78rem' }}
              onClick={() => setFilterCategory(cat)}
            >
              {cat}
            </button>
          ))}
        </div>
      </div>

      {/* Resources Table */}
      <div className="card">
        <h4 className="card-title">
          <Boxes size={18} color="#2563eb" />
          <span>Inventory Stock & Allocations ({filteredResources.length})</span>
        </h4>

        {loading ? (
          <p style={{ padding: '20px', textAlign: 'center', color: '#64748b' }}>Loading inventory from Java backend...</p>
        ) : filteredResources.length === 0 ? (
          <p style={{ padding: '20px', textAlign: 'center', color: '#94a3b8' }}>No resources registered in this category.</p>
        ) : (
          <div className="data-table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Resource ID</th>
                  <th>Item Name</th>
                  <th>Category</th>
                  <th>Available Stock</th>
                  <th>Reserved Stock</th>
                  <th>Unit Weight</th>
                  <th>Stock Health</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredResources.map((res) => {
                  const isLow = res.availableStock <= 20;
                  const total = res.availableStock + res.reservedStock;
                  const pct = total > 0 ? (res.availableStock / total) * 100 : 0;

                  return (
                    <tr key={res.id}>
                      <td><strong>{res.id}</strong></td>
                      <td style={{ fontWeight: 600 }}>{res.name}</td>
                      <td>
                        <span style={{ padding: '2px 8px', backgroundColor: '#f1f5f9', borderRadius: '4px', fontSize: '0.75rem', fontWeight: 600 }}>
                          {res.category}
                        </span>
                      </td>
                      <td>
                        <span style={{ fontSize: '1rem', fontWeight: 700, color: isLow ? '#dc2626' : '#16a34a' }}>
                          {res.availableStock}
                        </span>{' '}
                        <span style={{ fontSize: '0.78rem', color: '#64748b' }}>{res.unit}</span>
                      </td>
                      <td>
                        <span style={{ fontWeight: 600, color: '#f97316' }}>{res.reservedStock}</span>{' '}
                        <span style={{ fontSize: '0.78rem', color: '#64748b' }}>{res.unit}</span>
                      </td>
                      <td>{res.unitWeightKg} kg</td>
                      <td style={{ minWidth: '130px' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                          <div style={{ flex: 1, height: '8px', backgroundColor: '#e2e8f0', borderRadius: '4px', overflow: 'hidden' }}>
                            <div style={{ width: `${pct}%`, height: '100%', backgroundColor: isLow ? '#ef4444' : '#22c55e' }} />
                          </div>
                          <span style={{ fontSize: '0.72rem', fontWeight: 600, color: isLow ? '#dc2626' : '#16a34a' }}>
                            {isLow ? 'LOW' : 'OK'}
                          </span>
                        </div>
                      </td>
                      <td>
                        <div style={{ display: 'flex', gap: '6px' }}>
                          <button
                            className="btn btn-secondary"
                            style={{ padding: '4px 8px', fontSize: '0.75rem' }}
                            onClick={() => handleOpenStockAdjust(res)}
                            title="Adjust Stock (+ / -)"
                          >
                            <Edit3 size={13} />
                            <span>Stock</span>
                          </button>
                          <button
                            className="btn btn-danger"
                            style={{ padding: '4px 8px', fontSize: '0.75rem' }}
                            onClick={() => handleDelete(res.id, res.name)}
                            title="Delete Resource"
                          >
                            <Trash2 size={13} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Add Resource Modal */}
      {showAddModal && (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000, padding: '16px' }}>
          <div style={{ backgroundColor: 'white', borderRadius: '12px', width: '100%', maxWidth: '480px', padding: '24px', boxShadow: '0 20px 25px -5px rgba(0,0,0,0.2)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h3 style={{ fontSize: '1.15rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Boxes size={20} color="#2563eb" /> Add Resource Item
              </h3>
              <button onClick={() => setShowAddModal(false)} style={{ background: 'none', border: 'none', cursor: 'pointer' }}>
                <X size={18} />
              </button>
            </div>

            {formError && (
              <div style={{ backgroundColor: '#fee2e2', color: '#991b1b', padding: '8px 12px', borderRadius: '6px', fontSize: '0.82rem', marginBottom: '14px' }}>
                {formError}
              </div>
            )}

            <form onSubmit={handleAddSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Resource Item Name *</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Glucose Saline Packs"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Category</label>
                  <select
                    value={formData.category}
                    onChange={(e) => setFormData({ ...formData, category: e.target.value })}
                    style={{ width: '100%', padding: '8px 10px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                  >
                    {RESOURCE_CATEGORIES.map((c) => (
                      <option key={c} value={c}>{c}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Unit Label</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. bottles, kits"
                    value={formData.unit}
                    onChange={(e) => setFormData({ ...formData, unit: e.target.value })}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                  />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Initial Stock</label>
                  <input
                    type="number"
                    min="0"
                    required
                    value={formData.availableStock}
                    onChange={(e) => setFormData({ ...formData, availableStock: e.target.value })}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                  />
                </div>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Weight per Unit (kg)</label>
                  <input
                    type="number"
                    step="0.1"
                    min="0.1"
                    required
                    value={formData.unitWeightKg}
                    onChange={(e) => setFormData({ ...formData, unitWeightKg: e.target.value })}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                  />
                </div>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '10px' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setShowAddModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Add Item</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Adjust Stock Modal */}
      {showStockModal && currentResource && (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000, padding: '16px' }}>
          <div style={{ backgroundColor: 'white', borderRadius: '12px', width: '100%', maxWidth: '420px', padding: '24px', boxShadow: '0 20px 25px -5px rgba(0,0,0,0.2)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h3 style={{ fontSize: '1.1rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Edit3 size={18} color="#2563eb" /> Adjust Stock: {currentResource.name}
              </h3>
              <button onClick={() => setShowStockModal(false)} style={{ background: 'none', border: 'none', cursor: 'pointer' }}>
                <X size={18} />
              </button>
            </div>

            <p style={{ fontSize: '0.82rem', color: '#64748b', marginBottom: '14px' }}>
              Current Available Stock: <strong>{currentResource.availableStock} {currentResource.unit}</strong>
            </p>

            {formError && (
              <div style={{ backgroundColor: '#fee2e2', color: '#991b1b', padding: '8px 12px', borderRadius: '6px', fontSize: '0.82rem', marginBottom: '14px' }}>
                {formError}
              </div>
            )}

            <form onSubmit={handleStockAdjustSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>
                  Adjustment Amount (positive to add, negative to reduce):
                </label>
                <input
                  type="number"
                  required
                  value={stockDelta}
                  onChange={(e) => setStockDelta(e.target.value)}
                  style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                />
              </div>

              <div style={{ display: 'flex', gap: '8px' }}>
                <button
                  type="button"
                  className="btn btn-secondary"
                  style={{ flex: 1 }}
                  onClick={() => setStockDelta(25)}
                >
                  <ArrowUpRight size={14} color="#16a34a" /> +25 Restock
                </button>
                <button
                  type="button"
                  className="btn btn-secondary"
                  style={{ flex: 1 }}
                  onClick={() => setStockDelta(50)}
                >
                  <ArrowUpRight size={14} color="#16a34a" /> +50 Restock
                </button>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '10px' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setShowStockModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Apply Stock Update</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
