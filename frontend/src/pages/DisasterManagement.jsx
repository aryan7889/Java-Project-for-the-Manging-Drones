import React, { useState, useEffect } from 'react';
import { Flame, Plus, Search, Edit2, Trash2, MapPin, X, Check, AlertCircle } from 'lucide-react';
import AlertBadge from '../components/AlertBadge';
import { api } from '../services/api';

const DISASTER_TYPES = ['Flood', 'Earthquake', 'Cyclone', 'Landslide', 'Fire', 'Other'];
const SEVERITY_LEVELS = ['Low', 'Medium', 'High', 'Critical'];

export default function DisasterManagement() {
  const [disasters, setDisasters] = useState([]);
  const [locations, setLocations] = useState([]);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState('');
  const [filterSeverity, setFilterSeverity] = useState('ALL');

  // Modals state
  const [showAddModal, setShowAddModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [showLocationsModal, setShowLocationsModal] = useState(false);
  const [currentDisaster, setCurrentDisaster] = useState(null);

  // Form state
  const [formData, setFormData] = useState({
    name: '',
    type: 'Flood',
    affectedRegion: '',
    severity: 'Medium',
    description: '',
    date: new Date().toISOString().split('T')[0]
  });
  const [formError, setFormError] = useState('');
  const [notification, setNotification] = useState(null);

  const loadData = async () => {
    setLoading(true);
    try {
      const [disasterList, locationList] = await Promise.all([
        api.getDisasters(),
        api.getLocations()
      ]);
      setDisasters(disasterList || []);
      setLocations(locationList || []);
    } catch (err) {
      showToast('Error loading data: ' + err.message, 'error');
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
      type: 'Flood',
      affectedRegion: '',
      severity: 'Medium',
      description: '',
      date: new Date().toISOString().split('T')[0]
    });
    setFormError('');
    setShowAddModal(true);
  };

  const handleOpenEdit = (d) => {
    setCurrentDisaster(d);
    setFormData({
      name: d.name,
      type: d.type,
      affectedRegion: d.affectedRegion,
      severity: d.severity,
      description: d.description || '',
      date: d.date || ''
    });
    setFormError('');
    setShowEditModal(true);
  };

  const handleOpenLocations = (d) => {
    setCurrentDisaster(d);
    setShowLocationsModal(true);
  };

  const handleAddSubmit = async (e) => {
    e.preventDefault();
    if (!formData.name.trim() || !formData.affectedRegion.trim()) {
      setFormError('Disaster name and affected region are required.');
      return;
    }
    try {
      await api.createDisaster(formData);
      setShowAddModal(false);
      showToast('Disaster registered successfully!');
      loadData();
    } catch (err) {
      setFormError(err.message);
    }
  };

  const handleEditSubmit = async (e) => {
    e.preventDefault();
    if (!formData.name.trim() || !formData.affectedRegion.trim()) {
      setFormError('Disaster name and affected region are required.');
      return;
    }
    try {
      await api.updateDisaster(currentDisaster.id, formData);
      setShowEditModal(false);
      showToast('Disaster updated successfully!');
      loadData();
    } catch (err) {
      setFormError(err.message);
    }
  };

  const handleDelete = async (id, name) => {
    if (!window.confirm(`Are you sure you want to delete disaster: ${name} (${id})?`)) return;
    try {
      await api.deleteDisaster(id);
      showToast(`Disaster ${id} deleted.`);
      loadData();
    } catch (err) {
      showToast('Failed to delete disaster: ' + err.message, 'error');
    }
  };

  const filteredDisasters = disasters.filter((d) => {
    const matchesSearch =
      d.name.toLowerCase().includes(search.toLowerCase()) ||
      d.affectedRegion.toLowerCase().includes(search.toLowerCase()) ||
      d.id.toLowerCase().includes(search.toLowerCase());
    const matchesSeverity = filterSeverity === 'ALL' || d.severity === filterSeverity;
    return matchesSearch && matchesSeverity;
  });

  const associatedLocations = currentDisaster
    ? locations.filter((l) => l.disasterId === currentDisaster.id)
    : [];

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
          <h3 style={{ fontSize: '1.25rem', fontWeight: 700, color: '#0f172a' }}>Disaster Registry & Operations</h3>
          <p style={{ fontSize: '0.82rem', color: '#64748b' }}>
            Register new emergency events, monitor active disaster regions, and view mapped impact locations.
          </p>
        </div>
        <button className="btn btn-danger" onClick={handleOpenAdd}>
          <Plus size={16} />
          <span>Register New Disaster</span>
        </button>
      </div>

      {/* Filter and Search Bar */}
      <div className="card" style={{ padding: '16px 20px', marginBottom: '20px' }}>
        <div style={{ display: 'flex', gap: '16px', alignItems: 'center', flexWrap: 'wrap' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flex: 1, minWidth: '240px', backgroundColor: '#f8fafc', padding: '6px 12px', borderRadius: '6px', border: '1px solid #cbd5e1' }}>
            <Search size={16} color="#94a3b8" />
            <input
              type="text"
              placeholder="Search by name, region, or ID..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              style={{ border: 'none', background: 'transparent', outline: 'none', width: '100%', fontSize: '0.88rem' }}
            />
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ fontSize: '0.82rem', fontWeight: 600, color: '#475569' }}>Severity:</span>
            <select
              value={filterSeverity}
              onChange={(e) => setFilterSeverity(e.target.value)}
              style={{ padding: '6px 10px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '0.85rem' }}
            >
              <option value="ALL">All Severities</option>
              {SEVERITY_LEVELS.map((s) => (
                <option key={s} value={s.toUpperCase()}>{s}</option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {/* Disasters Table */}
      <div className="card">
        <h4 className="card-title">
          <Flame size={18} color="#dc2626" />
          <span>Active Disaster Declarations ({filteredDisasters.length})</span>
        </h4>

        {loading ? (
          <p style={{ padding: '20px', textAlign: 'center', color: '#64748b' }}>Loading disasters from Java backend...</p>
        ) : filteredDisasters.length === 0 ? (
          <p style={{ padding: '20px', textAlign: 'center', color: '#94a3b8' }}>No disaster records found matching criteria.</p>
        ) : (
          <div className="data-table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Disaster Name</th>
                  <th>Type</th>
                  <th>Affected Region</th>
                  <th>Severity</th>
                  <th>Declaration Date</th>
                  <th>Description</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredDisasters.map((d) => (
                  <tr key={d.id}>
                    <td><strong>{d.id}</strong></td>
                    <td style={{ fontWeight: 600 }}>{d.name}</td>
                    <td><span style={{ padding: '2px 8px', backgroundColor: '#f1f5f9', borderRadius: '4px', fontSize: '0.78rem', fontWeight: 600 }}>{d.type}</span></td>
                    <td>{d.affectedRegion}</td>
                    <td><AlertBadge level={d.severity} /></td>
                    <td>{d.date || 'N/A'}</td>
                    <td style={{ maxWidth: '220px', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', color: '#64748b', fontSize: '0.82rem' }}>
                      {d.description || 'No details'}
                    </td>
                    <td>
                      <div style={{ display: 'flex', gap: '6px' }}>
                        <button
                          className="btn btn-secondary"
                          style={{ padding: '4px 8px', fontSize: '0.75rem' }}
                          onClick={() => handleOpenLocations(d)}
                          title="View Associated Locations"
                        >
                          <MapPin size={13} />
                          <span>Locations</span>
                        </button>
                        <button
                          className="btn btn-secondary"
                          style={{ padding: '4px 8px', fontSize: '0.75rem' }}
                          onClick={() => handleOpenEdit(d)}
                          title="Edit Disaster"
                        >
                          <Edit2 size={13} />
                        </button>
                        <button
                          className="btn btn-danger"
                          style={{ padding: '4px 8px', fontSize: '0.75rem' }}
                          onClick={() => handleDelete(d.id, d.name)}
                          title="Delete Disaster"
                        >
                          <Trash2 size={13} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Add Disaster Modal */}
      {showAddModal && (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000, padding: '16px' }}>
          <div style={{ backgroundColor: 'white', borderRadius: '12px', width: '100%', maxWidth: '520px', padding: '24px', boxShadow: '0 20px 25px -5px rgba(0,0,0,0.2)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h3 style={{ fontSize: '1.15rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Flame size={20} color="#dc2626" /> Register New Disaster
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
                <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Disaster Name *</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Flash Flood in North Valley"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Disaster Type</label>
                  <select
                    value={formData.type}
                    onChange={(e) => setFormData({ ...formData, type: e.target.value })}
                    style={{ width: '100%', padding: '8px 10px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                  >
                    {DISASTER_TYPES.map((t) => (
                      <option key={t} value={t}>{t}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Severity Level</label>
                  <select
                    value={formData.severity}
                    onChange={(e) => setFormData({ ...formData, severity: e.target.value })}
                    style={{ width: '100%', padding: '8px 10px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                  >
                    {SEVERITY_LEVELS.map((s) => (
                      <option key={s} value={s.toUpperCase()}>{s}</option>
                    ))}
                  </select>
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Affected Region *</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Sector 4 Hills"
                    value={formData.affectedRegion}
                    onChange={(e) => setFormData({ ...formData, affectedRegion: e.target.value })}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                  />
                </div>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Date</label>
                  <input
                    type="date"
                    value={formData.date}
                    onChange={(e) => setFormData({ ...formData, date: e.target.value })}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                  />
                </div>
              </div>

              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Description</label>
                <textarea
                  rows="3"
                  placeholder="Enter situational details, damaged infrastructure, etc."
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '10px' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setShowAddModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-danger">Register Disaster</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Edit Disaster Modal */}
      {showEditModal && currentDisaster && (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000, padding: '16px' }}>
          <div style={{ backgroundColor: 'white', borderRadius: '12px', width: '100%', maxWidth: '520px', padding: '24px', boxShadow: '0 20px 25px -5px rgba(0,0,0,0.2)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h3 style={{ fontSize: '1.15rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Edit2 size={20} color="#2563eb" /> Edit Disaster ({currentDisaster.id})
              </h3>
              <button onClick={() => setShowEditModal(false)} style={{ background: 'none', border: 'none', cursor: 'pointer' }}>
                <X size={18} />
              </button>
            </div>

            {formError && (
              <div style={{ backgroundColor: '#fee2e2', color: '#991b1b', padding: '8px 12px', borderRadius: '6px', fontSize: '0.82rem', marginBottom: '14px' }}>
                {formError}
              </div>
            )}

            <form onSubmit={handleEditSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Disaster Name *</label>
                <input
                  type="text"
                  required
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Disaster Type</label>
                  <select
                    value={formData.type}
                    onChange={(e) => setFormData({ ...formData, type: e.target.value })}
                    style={{ width: '100%', padding: '8px 10px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                  >
                    {DISASTER_TYPES.map((t) => (
                      <option key={t} value={t}>{t}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Severity Level</label>
                  <select
                    value={formData.severity}
                    onChange={(e) => setFormData({ ...formData, severity: e.target.value })}
                    style={{ width: '100%', padding: '8px 10px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                  >
                    {SEVERITY_LEVELS.map((s) => (
                      <option key={s} value={s.toUpperCase()}>{s}</option>
                    ))}
                  </select>
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Affected Region *</label>
                  <input
                    type="text"
                    required
                    value={formData.affectedRegion}
                    onChange={(e) => setFormData({ ...formData, affectedRegion: e.target.value })}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                  />
                </div>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Date</label>
                  <input
                    type="date"
                    value={formData.date}
                    onChange={(e) => setFormData({ ...formData, date: e.target.value })}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                  />
                </div>
              </div>

              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Description</label>
                <textarea
                  rows="3"
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '10px' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setShowEditModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Save Changes</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Associated Locations Modal */}
      {showLocationsModal && currentDisaster && (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000, padding: '16px' }}>
          <div style={{ backgroundColor: 'white', borderRadius: '12px', width: '100%', maxWidth: '640px', padding: '24px', boxShadow: '0 20px 25px -5px rgba(0,0,0,0.2)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <div>
                <h3 style={{ fontSize: '1.15rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <MapPin size={20} color="#2563eb" /> Locations Associated with {currentDisaster.name}
                </h3>
                <span style={{ fontSize: '0.78rem', color: '#64748b' }}>Disaster ID: {currentDisaster.id}</span>
              </div>
              <button onClick={() => setShowLocationsModal(false)} style={{ background: 'none', border: 'none', cursor: 'pointer' }}>
                <X size={18} />
              </button>
            </div>

            {associatedLocations.length === 0 ? (
              <p style={{ padding: '24px', textAlign: 'center', color: '#94a3b8' }}>
                No affected locations are currently associated with this disaster.
              </p>
            ) : (
              <div className="data-table-container">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Location ID</th>
                      <th>Location Name</th>
                      <th>Coordinates</th>
                      <th>Population</th>
                      <th>Severity</th>
                    </tr>
                  </thead>
                  <tbody>
                    {associatedLocations.map((l) => (
                      <tr key={l.id}>
                        <td><strong>{l.id}</strong></td>
                        <td>{l.name}</td>
                        <td>{l.latitude}°, {l.longitude}°</td>
                        <td>{l.affectedPopulation}</td>
                        <td><AlertBadge level={l.severity} /></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '16px' }}>
              <button className="btn btn-secondary" onClick={() => setShowLocationsModal(false)}>Close</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
