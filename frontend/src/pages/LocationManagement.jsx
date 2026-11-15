import React, { useState, useEffect } from 'react';
import { MapPin, Plus, Search, Edit2, Trash2, Network, X } from 'lucide-react';
import AlertBadge from '../components/AlertBadge';
import { api } from '../services/api';

const SEVERITY_LEVELS = ['Low', 'Medium', 'High', 'Critical'];

export default function LocationManagement() {
  const [locations, setLocations] = useState([]);
  const [disasters, setDisasters] = useState([]);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState('');
  const [filterDisaster, setFilterDisaster] = useState('ALL');

  // Modals state
  const [showAddModal, setShowAddModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [currentLocation, setCurrentLocation] = useState(null);

  // Form state
  const [formData, setFormData] = useState({
    name: '',
    latitude: '28.6139',
    longitude: '77.2090',
    disasterId: '',
    affectedPopulation: '100',
    severity: 'Medium'
  });
  const [formError, setFormError] = useState('');
  const [notification, setNotification] = useState(null);

  const loadData = async () => {
    setLoading(true);
    try {
      const [locList, disasterList] = await Promise.all([
        api.getLocations(),
        api.getDisasters()
      ]);
      setLocations(locList || []);
      setDisasters(disasterList || []);
    } catch (err) {
      showToast('Error loading locations: ' + err.message, 'error');
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
      latitude: '28.6200',
      longitude: '77.2300',
      disasterId: disasters.length > 0 ? disasters[0].id : '',
      affectedPopulation: '100',
      severity: 'Medium'
    });
    setFormError('');
    setShowAddModal(true);
  };

  const handleOpenEdit = (loc) => {
    setCurrentLocation(loc);
    setFormData({
      name: loc.name,
      latitude: String(loc.latitude),
      longitude: String(loc.longitude),
      disasterId: loc.disasterId || '',
      affectedPopulation: String(loc.affectedPopulation),
      severity: loc.severity
    });
    setFormError('');
    setShowEditModal(true);
  };

  const handleAddSubmit = async (e) => {
    e.preventDefault();
    if (!formData.name.trim()) {
      setFormError('Location name is required.');
      return;
    }
    const lat = parseFloat(formData.latitude);
    const lon = parseFloat(formData.longitude);
    const pop = parseInt(formData.affectedPopulation, 10);

    if (isNaN(lat) || lat < -90 || lat > 90) {
      setFormError('Latitude must be a valid number between -90 and 90.');
      return;
    }
    if (isNaN(lon) || lon < -180 || lon > 180) {
      setFormError('Longitude must be a valid number between -180 and 180.');
      return;
    }
    if (isNaN(pop) || pop < 0) {
      setFormError('Affected population must be 0 or greater.');
      return;
    }

    try {
      await api.createLocation({
        name: formData.name.trim(),
        latitude: lat,
        longitude: lon,
        disasterId: formData.disasterId,
        affectedPopulation: pop,
        severity: formData.severity
      });
      setShowAddModal(false);
      showToast('Location registered successfully as a graph vertex!');
      loadData();
    } catch (err) {
      setFormError(err.message);
    }
  };

  const handleEditSubmit = async (e) => {
    e.preventDefault();
    if (!formData.name.trim()) {
      setFormError('Location name is required.');
      return;
    }
    const lat = parseFloat(formData.latitude);
    const lon = parseFloat(formData.longitude);
    const pop = parseInt(formData.affectedPopulation, 10);

    try {
      await api.createLocation(formData); // Will be routed to update if id present
      setShowEditModal(false);
      showToast('Location updated successfully!');
      loadData();
    } catch (err) {
      setFormError(err.message);
    }
  };

  const handleDelete = async (id, name) => {
    if (!window.confirm(`Are you sure you want to delete location ${name} (${id})? Any connected route corridors will also be removed.`)) return;
    try {
      await api.deleteLocation(id);
      showToast(`Location ${id} deleted.`);
      loadData();
    } catch (err) {
      showToast('Failed to delete location: ' + err.message, 'error');
    }
  };

  const filteredLocations = locations.filter((l) => {
    const matchesSearch =
      l.name.toLowerCase().includes(search.toLowerCase()) ||
      l.id.toLowerCase().includes(search.toLowerCase());
    const matchesDisaster = filterDisaster === 'ALL' || l.disasterId === filterDisaster;
    return matchesSearch && matchesDisaster;
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
          <h3 style={{ fontSize: '1.25rem', fontWeight: 700, color: '#0f172a' }}>Affected Locations & Graph Vertices</h3>
          <p style={{ fontSize: '0.82rem', color: '#64748b' }}>
            Map disaster zones, relief camps, and base hospitals as nodes in the transportation graph network.
          </p>
        </div>
        <button className="btn btn-primary" onClick={handleOpenAdd}>
          <Plus size={16} />
          <span>Add Affected Location</span>
        </button>
      </div>

      {/* Filter and Search Bar */}
      <div className="card" style={{ padding: '16px 20px', marginBottom: '20px' }}>
        <div style={{ display: 'flex', gap: '16px', alignItems: 'center', flexWrap: 'wrap' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flex: 1, minWidth: '240px', backgroundColor: '#f8fafc', padding: '6px 12px', borderRadius: '6px', border: '1px solid #cbd5e1' }}>
            <Search size={16} color="#94a3b8" />
            <input
              type="text"
              placeholder="Search by location name or ID..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              style={{ border: 'none', background: 'transparent', outline: 'none', width: '100%', fontSize: '0.88rem' }}
            />
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ fontSize: '0.82rem', fontWeight: 600, color: '#475569' }}>Disaster:</span>
            <select
              value={filterDisaster}
              onChange={(e) => setFilterDisaster(e.target.value)}
              style={{ padding: '6px 10px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '0.85rem' }}
            >
              <option value="ALL">All Disasters</option>
              {disasters.map((d) => (
                <option key={d.id} value={d.id}>{d.id} – {d.name}</option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {/* Locations Table */}
      <div className="card">
        <h4 className="card-title">
          <Network size={18} color="#2563eb" />
          <span>Registered Network Vertices ({filteredLocations.length})</span>
        </h4>

        {loading ? (
          <p style={{ padding: '20px', textAlign: 'center', color: '#64748b' }}>Loading locations from Java backend...</p>
        ) : filteredLocations.length === 0 ? (
          <p style={{ padding: '20px', textAlign: 'center', color: '#94a3b8' }}>No locations found matching search filters.</p>
        ) : (
          <div className="data-table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Vertex ID</th>
                  <th>Location Name</th>
                  <th>Coordinates (Lat, Lon)</th>
                  <th>Associated Disaster</th>
                  <th>Affected Population</th>
                  <th>Severity</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredLocations.map((loc) => (
                  <tr key={loc.id}>
                    <td><strong>{loc.id}</strong></td>
                    <td style={{ fontWeight: 600 }}>{loc.name}</td>
                    <td>
                      <code style={{ fontSize: '0.8rem', backgroundColor: '#f1f5f9', padding: '2px 6px', borderRadius: '4px' }}>
                        {loc.latitude.toFixed(4)}°, {loc.longitude.toFixed(4)}°
                      </code>
                    </td>
                    <td>
                      {loc.disasterId ? (
                        <span style={{ fontWeight: 500, color: '#2563eb' }}>{loc.disasterId}</span>
                      ) : (
                        <span style={{ color: '#94a3b8' }}>Unassigned</span>
                      )}
                    </td>
                    <td>
                      <strong>{loc.affectedPopulation}</strong> people
                    </td>
                    <td><AlertBadge level={loc.severity} /></td>
                    <td>
                      <div style={{ display: 'flex', gap: '6px' }}>
                        <button
                          className="btn btn-secondary"
                          style={{ padding: '4px 8px', fontSize: '0.75rem' }}
                          onClick={() => handleOpenEdit(loc)}
                          title="Edit Location"
                        >
                          <Edit2 size={13} />
                        </button>
                        <button
                          className="btn btn-danger"
                          style={{ padding: '4px 8px', fontSize: '0.75rem' }}
                          onClick={() => handleDelete(loc.id, loc.name)}
                          title="Delete Location"
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

      {/* Add Location Modal */}
      {showAddModal && (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000, padding: '16px' }}>
          <div style={{ backgroundColor: 'white', borderRadius: '12px', width: '100%', maxWidth: '500px', padding: '24px', boxShadow: '0 20px 25px -5px rgba(0,0,0,0.2)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h3 style={{ fontSize: '1.15rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '8px' }}>
                <MapPin size={20} color="#2563eb" /> Add Location / Vertex
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
                <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Location Name *</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Relief Camp Bravo"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Latitude (-90 to 90) *</label>
                  <input
                    type="number"
                    step="any"
                    required
                    value={formData.latitude}
                    onChange={(e) => setFormData({ ...formData, latitude: e.target.value })}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                  />
                </div>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Longitude (-180 to 180) *</label>
                  <input
                    type="number"
                    step="any"
                    required
                    value={formData.longitude}
                    onChange={(e) => setFormData({ ...formData, longitude: e.target.value })}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                  />
                </div>
              </div>

              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Associated Disaster</label>
                <select
                  value={formData.disasterId}
                  onChange={(e) => setFormData({ ...formData, disasterId: e.target.value })}
                  style={{ width: '100%', padding: '8px 10px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                >
                  <option value="">None / Base Station</option>
                  {disasters.map((d) => (
                    <option key={d.id} value={d.id}>{d.id} – {d.name}</option>
                  ))}
                </select>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Affected Population</label>
                  <input
                    type="number"
                    min="0"
                    value={formData.affectedPopulation}
                    onChange={(e) => setFormData({ ...formData, affectedPopulation: e.target.value })}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                  />
                </div>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Location Severity</label>
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

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '10px' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setShowAddModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Add Location</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Edit Location Modal */}
      {showEditModal && currentLocation && (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000, padding: '16px' }}>
          <div style={{ backgroundColor: 'white', borderRadius: '12px', width: '100%', maxWidth: '500px', padding: '24px', boxShadow: '0 20px 25px -5px rgba(0,0,0,0.2)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h3 style={{ fontSize: '1.15rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Edit2 size={20} color="#2563eb" /> Edit Location ({currentLocation.id})
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
                <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Location Name *</label>
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
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Latitude *</label>
                  <input
                    type="number"
                    step="any"
                    required
                    value={formData.latitude}
                    onChange={(e) => setFormData({ ...formData, latitude: e.target.value })}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                  />
                </div>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Longitude *</label>
                  <input
                    type="number"
                    step="any"
                    required
                    value={formData.longitude}
                    onChange={(e) => setFormData({ ...formData, longitude: e.target.value })}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                  />
                </div>
              </div>

              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Associated Disaster</label>
                <select
                  value={formData.disasterId}
                  onChange={(e) => setFormData({ ...formData, disasterId: e.target.value })}
                  style={{ width: '100%', padding: '8px 10px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                >
                  <option value="">None / Base Station</option>
                  {disasters.map((d) => (
                    <option key={d.id} value={d.id}>{d.id} – {d.name}</option>
                  ))}
                </select>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Affected Population</label>
                  <input
                    type="number"
                    min="0"
                    value={formData.affectedPopulation}
                    onChange={(e) => setFormData({ ...formData, affectedPopulation: e.target.value })}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                  />
                </div>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Location Severity</label>
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

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '10px' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setShowEditModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Save Changes</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
