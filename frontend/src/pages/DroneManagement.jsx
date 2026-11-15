import React, { useState, useEffect } from 'react';
import {
  Navigation,
  Plus,
  BatteryCharging,
  BatteryMedium,
  BatteryWarning,
  ShieldAlert,
  Edit2,
  Trash2,
  X,
  RotateCw,
  CheckCircle2,
  Wrench,
  Search,
  Filter
} from 'lucide-react';
import StatCard from '../components/StatCard';
import { api } from '../services/api';

const DRONE_TYPES = ['Heavy-Lift Hexacopter', 'Medium Quadcopter', 'Fast Medical VTOL', 'Cargo Octocopter', 'Scout Drone'];
const DRONE_STATUSES = ['AVAILABLE', 'ASSIGNED', 'IN_FLIGHT', 'MAINTENANCE'];

export default function DroneManagement() {
  const [drones, setDrones] = useState([]);
  const [locations, setLocations] = useState([]);
  const [loading, setLoading] = useState(false);
  const [filterStatus, setFilterStatus] = useState('ALL');
  const [search, setSearch] = useState('');

  // Modals state
  const [showAddModal, setShowAddModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [currentDrone, setCurrentDrone] = useState(null);

  // Form state
  const [formData, setFormData] = useState({
    name: '',
    droneType: 'Medium Quadcopter',
    maxCapacityKg: '10.0',
    batteryPercentage: '100',
    maxFlightRangeKm: '35.0',
    currentLocationId: 'LOC-001',
    status: 'AVAILABLE'
  });
  const [formError, setFormError] = useState('');
  const [notification, setNotification] = useState(null);

  const loadData = async () => {
    setLoading(true);
    try {
      const [droneList, locList] = await Promise.all([
        api.getDrones(),
        api.getLocations()
      ]);
      setDrones(droneList || []);
      setLocations(locList || []);
    } catch (err) {
      showToast('Error loading drones: ' + err.message, 'error');
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
      droneType: 'Medium Quadcopter',
      maxCapacityKg: '10.0',
      batteryPercentage: '100',
      maxFlightRangeKm: '35.0',
      currentLocationId: locations.length > 0 ? locations[0].id : 'LOC-001',
      status: 'AVAILABLE'
    });
    setFormError('');
    setShowAddModal(true);
  };

  const handleOpenEdit = (d) => {
    setCurrentDrone(d);
    setFormData({
      name: d.name,
      droneType: d.droneType || 'Quadcopter',
      maxCapacityKg: String(d.maxCapacityKg),
      batteryPercentage: String(d.batteryPercentage),
      maxFlightRangeKm: String(d.maxFlightRangeKm),
      currentLocationId: d.currentLocationId || 'LOC-001',
      status: d.status
    });
    setFormError('');
    setShowEditModal(true);
  };

  const handleAddSubmit = async (e) => {
    e.preventDefault();
    if (!formData.name.trim()) {
      setFormError('Drone name is required.');
      return;
    }
    const cap = parseFloat(formData.maxCapacityKg);
    const bat = parseInt(formData.batteryPercentage, 10);
    const range = parseFloat(formData.maxFlightRangeKm);

    if (isNaN(cap) || cap <= 0) {
      setFormError('Max capacity must be positive.');
      return;
    }
    if (isNaN(bat) || bat < 0 || bat > 100) {
      setFormError('Battery percentage must be between 0 and 100.');
      return;
    }
    if (isNaN(range) || range <= 0) {
      setFormError('Flight range must be positive.');
      return;
    }

    try {
      await api.createDrone({
        name: formData.name.trim(),
        droneType: formData.droneType,
        maxCapacityKg: cap,
        batteryPercentage: bat,
        maxFlightRangeKm: range,
        currentLocationId: formData.currentLocationId,
        status: formData.status
      });
      setShowAddModal(false);
      showToast('New drone registered to fleet!');
      loadData();
    } catch (err) {
      setFormError(err.message);
    }
  };

  const handleEditSubmit = async (e) => {
    e.preventDefault();
    try {
      await fetch(`http://localhost:8080/api/drones/${currentDrone.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: formData.name.trim(),
          droneType: formData.droneType,
          maxCapacityKg: parseFloat(formData.maxCapacityKg),
          batteryPercentage: parseInt(formData.batteryPercentage, 10),
          maxFlightRangeKm: parseFloat(formData.maxFlightRangeKm),
          currentLocationId: formData.currentLocationId,
          status: formData.status
        })
      });
      setShowEditModal(false);
      showToast(`Drone ${currentDrone.id} updated successfully!`);
      loadData();
    } catch (err) {
      setFormError(err.message);
    }
  };

  const handleRecharge = async (id, name) => {
    try {
      await fetch(`http://localhost:8080/api/drones/${id}/recharge`, { method: 'PUT' });
      showToast(`Drone ${name} (${id}) recharged to 100%! Status restored to AVAILABLE.`);
      loadData();
    } catch (err) {
      showToast('Recharge failed: ' + err.message, 'error');
    }
  };

  const handleDelete = async (id, name) => {
    if (!window.confirm(`Are you sure you want to retire and remove drone ${name} (${id}) from the fleet?`)) return;
    try {
      await fetch(`http://localhost:8080/api/drones/${id}`, { method: 'DELETE' });
      showToast(`Drone ${id} retired from fleet.`);
      loadData();
    } catch (err) {
      showToast('Failed to delete drone: ' + err.message, 'error');
    }
  };

  // Fleet summary stats
  const totalDrones = drones.length;
  const availableCount = drones.filter((d) => d.status === 'AVAILABLE').length;
  const activeCount = drones.filter((d) => d.status === 'ASSIGNED' || d.status === 'IN_FLIGHT').length;
  const maintCount = drones.filter((d) => d.status === 'MAINTENANCE').length;

  const filteredDrones = drones.filter((d) => {
    const matchesSearch =
      d.name.toLowerCase().includes(search.toLowerCase()) ||
      d.id.toLowerCase().includes(search.toLowerCase()) ||
      (d.droneType && d.droneType.toLowerCase().includes(search.toLowerCase()));
    const matchesStatus = filterStatus === 'ALL' || d.status === filterStatus;
    return matchesSearch && matchesStatus;
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
          <h3 style={{ fontSize: '1.25rem', fontWeight: 700, color: '#0f172a' }}>Drone Fleet Management</h3>
          <p style={{ fontSize: '0.82rem', color: '#64748b' }}>
            Monitor flight ranges, payload limits, battery telemetry, and manage automated dispatch readiness.
          </p>
        </div>
        <button className="btn btn-primary" onClick={handleOpenAdd}>
          <Plus size={16} />
          <span>Register New Drone</span>
        </button>
      </div>

      {/* KPI Cards */}
      <div className="stat-grid" style={{ marginBottom: '20px' }}>
        <StatCard
          title="Total Fleet Size"
          value={totalDrones}
          subtext="Registered aerial assets"
          icon={Navigation}
          color="#2563eb"
          bgLight="#eff6ff"
        />
        <StatCard
          title="Ready For Dispatch"
          value={availableCount}
          subtext="Status: AVAILABLE"
          icon={CheckCircle2}
          color="#16a34a"
          bgLight="#dcfce7"
        />
        <StatCard
          title="Active Missions"
          value={activeCount}
          subtext="Assigned or In Flight"
          icon={RotateCw}
          color="#ea580c"
          bgLight="#ffedd5"
        />
        <StatCard
          title="Under Maintenance"
          value={maintCount}
          subtext="Recharging / Inspection"
          icon={Wrench}
          color="#dc2626"
          bgLight="#fee2e2"
        />
      </div>

      {/* Filter and Search Bar */}
      <div className="card" style={{ padding: '16px 20px', marginBottom: '20px' }}>
        <div style={{ display: 'flex', gap: '16px', alignItems: 'center', flexWrap: 'wrap' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flex: 1, minWidth: '240px', backgroundColor: '#f8fafc', padding: '6px 12px', borderRadius: '6px', border: '1px solid #cbd5e1' }}>
            <Search size={16} color="#94a3b8" />
            <input
              type="text"
              placeholder="Search by drone name, type, or ID..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              style={{ border: 'none', background: 'transparent', outline: 'none', width: '100%', fontSize: '0.88rem' }}
            />
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ fontSize: '0.82rem', fontWeight: 600, color: '#475569' }}>Status:</span>
            <select
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value)}
              style={{ padding: '6px 10px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '0.85rem' }}
            >
              <option value="ALL">All Fleet Statuses</option>
              {DRONE_STATUSES.map((s) => (
                <option key={s} value={s}>{s}</option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {/* Drones Table */}
      <div className="card">
        <h4 className="card-title">
          <Navigation size={18} color="#2563eb" />
          <span>Active Fleet Assets ({filteredDrones.length})</span>
        </h4>

        {loading ? (
          <p style={{ padding: '20px', textAlign: 'center', color: '#64748b' }}>Loading fleet telemetry from Java backend...</p>
        ) : filteredDrones.length === 0 ? (
          <p style={{ padding: '20px', textAlign: 'center', color: '#94a3b8' }}>No drones found matching criteria.</p>
        ) : (
          <div className="data-table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Drone ID</th>
                  <th>Drone Name</th>
                  <th>Type</th>
                  <th>Max Capacity</th>
                  <th>Battery Gauge</th>
                  <th>Max Flight Range</th>
                  <th>Base Station</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredDrones.map((drone) => {
                  const bat = drone.batteryPercentage;
                  const batColor = bat >= 70 ? '#16a34a' : bat >= 30 ? '#ea580c' : '#dc2626';

                  return (
                    <tr key={drone.id}>
                      <td><strong>{drone.id}</strong></td>
                      <td style={{ fontWeight: 600 }}>{drone.name}</td>
                      <td>
                        <span style={{ padding: '2px 8px', backgroundColor: '#f1f5f9', borderRadius: '4px', fontSize: '0.75rem', fontWeight: 600 }}>
                          {drone.droneType}
                        </span>
                      </td>
                      <td>
                        <strong>{drone.maxCapacityKg}</strong> kg
                      </td>
                      <td style={{ minWidth: '140px' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                          <div style={{ flex: 1, height: '8px', backgroundColor: '#e2e8f0', borderRadius: '4px', overflow: 'hidden' }}>
                            <div style={{ width: `${bat}%`, height: '100%', backgroundColor: batColor }} />
                          </div>
                          <span style={{ fontSize: '0.8rem', fontWeight: 700, color: batColor, minWidth: '34px' }}>
                            {bat}%
                          </span>
                        </div>
                      </td>
                      <td>{drone.maxFlightRangeKm} km</td>
                      <td>{drone.currentLocationId || 'LOC-001'}</td>
                      <td>
                        <span
                          className="badge"
                          style={{
                            backgroundColor:
                              drone.status === 'AVAILABLE' ? '#dcfce7' :
                              drone.status === 'ASSIGNED' ? '#dbeafe' :
                              drone.status === 'IN_FLIGHT' ? '#ffedd5' : '#fee2e2',
                            color:
                              drone.status === 'AVAILABLE' ? '#15803d' :
                              drone.status === 'ASSIGNED' ? '#1d4ed8' :
                              drone.status === 'IN_FLIGHT' ? '#c2410c' : '#991b1b'
                          }}
                        >
                          {drone.status}
                        </span>
                      </td>
                      <td>
                        <div style={{ display: 'flex', gap: '6px' }}>
                          <button
                            className="btn btn-secondary"
                            style={{ padding: '4px 8px', fontSize: '0.75rem', color: '#16a34a' }}
                            onClick={() => handleRecharge(drone.id, drone.name)}
                            title="Recharge to 100%"
                          >
                            <BatteryCharging size={13} />
                            <span>Recharge</span>
                          </button>
                          <button
                            className="btn btn-secondary"
                            style={{ padding: '4px 8px', fontSize: '0.75rem' }}
                            onClick={() => handleOpenEdit(drone)}
                            title="Edit Drone"
                          >
                            <Edit2 size={13} />
                          </button>
                          <button
                            className="btn btn-danger"
                            style={{ padding: '4px 8px', fontSize: '0.75rem' }}
                            onClick={() => handleDelete(drone.id, drone.name)}
                            title="Retire Drone"
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

      {/* Register Drone Modal */}
      {showAddModal && (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000, padding: '16px' }}>
          <div style={{ backgroundColor: 'white', borderRadius: '12px', width: '100%', maxWidth: '520px', padding: '24px', boxShadow: '0 20px 25px -5px rgba(0,0,0,0.2)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h3 style={{ fontSize: '1.15rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Navigation size={20} color="#2563eb" /> Register Drone into Fleet
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
                <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Drone Model / Call-Sign *</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. SkyLifter Alpha"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Drone Airframe Type</label>
                  <select
                    value={formData.droneType}
                    onChange={(e) => setFormData({ ...formData, droneType: e.target.value })}
                    style={{ width: '100%', padding: '8px 10px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                  >
                    {DRONE_TYPES.map((t) => (
                      <option key={t} value={t}>{t}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Max Payload (kg) *</label>
                  <input
                    type="number"
                    step="0.5"
                    min="1"
                    required
                    value={formData.maxCapacityKg}
                    onChange={(e) => setFormData({ ...formData, maxCapacityKg: e.target.value })}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                  />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Max Range (km) *</label>
                  <input
                    type="number"
                    step="1"
                    min="1"
                    required
                    value={formData.maxFlightRangeKm}
                    onChange={(e) => setFormData({ ...formData, maxFlightRangeKm: e.target.value })}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                  />
                </div>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Battery % (0-100)</label>
                  <input
                    type="number"
                    min="0"
                    max="100"
                    required
                    value={formData.batteryPercentage}
                    onChange={(e) => setFormData({ ...formData, batteryPercentage: e.target.value })}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                  />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Base Station Location</label>
                  <select
                    value={formData.currentLocationId}
                    onChange={(e) => setFormData({ ...formData, currentLocationId: e.target.value })}
                    style={{ width: '100%', padding: '8px 10px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                  >
                    {locations.map((l) => (
                      <option key={l.id} value={l.id}>{l.id} – {l.name}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Initial Fleet Status</label>
                  <select
                    value={formData.status}
                    onChange={(e) => setFormData({ ...formData, status: e.target.value })}
                    style={{ width: '100%', padding: '8px 10px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                  >
                    {DRONE_STATUSES.map((s) => (
                      <option key={s} value={s}>{s}</option>
                    ))}
                  </select>
                </div>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '10px' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setShowAddModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Register Asset</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Edit Drone Modal */}
      {showEditModal && currentDrone && (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000, padding: '16px' }}>
          <div style={{ backgroundColor: 'white', borderRadius: '12px', width: '100%', maxWidth: '520px', padding: '24px', boxShadow: '0 20px 25px -5px rgba(0,0,0,0.2)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h3 style={{ fontSize: '1.15rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Edit2 size={20} color="#2563eb" /> Edit Drone Specs ({currentDrone.id})
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
                <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Drone Model / Call-Sign *</label>
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
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Drone Airframe Type</label>
                  <select
                    value={formData.droneType}
                    onChange={(e) => setFormData({ ...formData, droneType: e.target.value })}
                    style={{ width: '100%', padding: '8px 10px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                  >
                    {DRONE_TYPES.map((t) => (
                      <option key={t} value={t}>{t}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Max Payload (kg) *</label>
                  <input
                    type="number"
                    step="0.5"
                    min="1"
                    required
                    value={formData.maxCapacityKg}
                    onChange={(e) => setFormData({ ...formData, maxCapacityKg: e.target.value })}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                  />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Max Range (km) *</label>
                  <input
                    type="number"
                    step="1"
                    min="1"
                    required
                    value={formData.maxFlightRangeKm}
                    onChange={(e) => setFormData({ ...formData, maxFlightRangeKm: e.target.value })}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                  />
                </div>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Battery % (0-100)</label>
                  <input
                    type="number"
                    min="0"
                    max="100"
                    required
                    value={formData.batteryPercentage}
                    onChange={(e) => setFormData({ ...formData, batteryPercentage: e.target.value })}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                  />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Base Station Location</label>
                  <select
                    value={formData.currentLocationId}
                    onChange={(e) => setFormData({ ...formData, currentLocationId: e.target.value })}
                    style={{ width: '100%', padding: '8px 10px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                  >
                    {locations.map((l) => (
                      <option key={l.id} value={l.id}>{l.id} – {l.name}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Operational Status</label>
                  <select
                    value={formData.status}
                    onChange={(e) => setFormData({ ...formData, status: e.target.value })}
                    style={{ width: '100%', padding: '8px 10px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                  >
                    {DRONE_STATUSES.map((s) => (
                      <option key={s} value={s}>{s}</option>
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
