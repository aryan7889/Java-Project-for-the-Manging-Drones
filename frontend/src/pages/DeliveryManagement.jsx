import React, { useState, useEffect } from 'react';
import {
  Truck,
  Play,
  CheckSquare,
  XCircle,
  RotateCw,
  Plus,
  Send,
  Navigation,
  IndianRupee,
  Clock,
  X,
  AlertTriangle,
  MapPin,
  CheckCircle2
} from 'lucide-react';
import { api } from '../services/api';

export default function DeliveryManagement() {
  const [deliveries, setDeliveries] = useState([]);
  const [requests, setRequests] = useState([]);
  const [drones, setDrones] = useState([]);
  const [locations, setLocations] = useState([]);
  const [loading, setLoading] = useState(false);

  // Modals state
  const [showPlanModal, setShowPlanModal] = useState(false);
  const [selectedRequestId, setSelectedRequestId] = useState('');
  const [selectedDroneId, setSelectedDroneId] = useState('');
  const [planError, setFormError] = useState('');
  const [notification, setNotification] = useState(null);

  const loadData = async () => {
    setLoading(true);
    try {
      const [delList, reqList, droneList, locList] = await Promise.all([
        api.getDeliveries(),
        api.getRequests(),
        api.getDrones(),
        api.getLocations()
      ]);
      setDeliveries(delList || []);
      setRequests(reqList || []);
      setDrones(droneList || []);
      setLocations(locList || []);

      const pendingReqs = (reqList || []).filter((r) => r.status === 'PENDING');
      if (pendingReqs.length > 0 && !selectedRequestId) {
        setSelectedRequestId(pendingReqs[0].id);
      }
    } catch (err) {
      showToast('Error loading active missions: ' + err.message, 'error');
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

  const handleStartDelivery = async (id) => {
    try {
      await api.startDelivery(id);
      showToast(`Mission ${id} dispatched! Drone is now IN_FLIGHT.`);
      loadData();
    } catch (err) {
      showToast('Failed to start mission: ' + err.message, 'error');
    }
  };

  const handleCompleteDelivery = async (id) => {
    if (!window.confirm(`Confirm delivery completion for mission ${id}? Supplies will be delivered, drone location updated, and battery deducted.`)) return;
    try {
      await api.completeDelivery(id);
      showToast(`Mission ${id} successfully completed! Assets updated & saved to history.`);
      loadData();
    } catch (err) {
      showToast('Failed to complete delivery: ' + err.message, 'error');
    }
  };

  const handleCancelDelivery = async (id) => {
    if (!window.confirm(`Are you sure you want to cancel delivery mission ${id}? Reserved drone and inventory will be restored.`)) return;
    try {
      await fetch('http://localhost:8080/api/deliveries/cancel', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ deliveryId: id })
      });
      showToast(`Mission ${id} cancelled. Assets restored.`);
      loadData();
    } catch (err) {
      showToast('Failed to cancel delivery: ' + err.message, 'error');
    }
  };

  const handlePlanSubmit = async (e) => {
    e.preventDefault();
    if (!selectedRequestId) {
      setFormError('Please select a pending relief request.');
      return;
    }
    try {
      await fetch('http://localhost:8080/api/deliveries', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          requestId: selectedRequestId,
          droneId: selectedDroneId || null
        })
      });
      setShowPlanModal(false);
      showToast('New delivery mission planned with Dijkstra shortest route!');
      loadData();
    } catch (err) {
      setFormError(err.message);
    }
  };

  const getLocationName = (id) => {
    const loc = locations.find((l) => l.id === id);
    return loc ? `${loc.name} (${id})` : id;
  };

  const getDroneName = (id) => {
    const d = drones.find((dr) => dr.id === id);
    return d ? `${d.name} (${id})` : id;
  };

  const pendingRequests = requests.filter((r) => r.status === 'PENDING');
  const availableDrones = drones.filter((d) => d.status === 'AVAILABLE');

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
          <h3 style={{ fontSize: '1.25rem', fontWeight: 700, color: '#0f172a' }}>Live Delivery Operations & Mission Simulation</h3>
          <p style={{ fontSize: '0.82rem', color: '#64748b' }}>
            Control mission dispatch, simulate drone transit, complete deliveries, and deduct inventory.
          </p>
        </div>
        <button
          className="btn btn-primary"
          onClick={() => {
            setFormError('');
            setShowPlanModal(true);
          }}
          disabled={pendingRequests.length === 0}
        >
          <Plus size={16} />
          <span>Plan New Delivery Mission</span>
        </button>
      </div>

      {/* Active Deliveries Table */}
      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <h4 className="card-title" style={{ margin: 0 }}>
            <Truck size={18} color="#2563eb" />
            <span>Active Relief Missions ({deliveries.length})</span>
          </h4>
          <span style={{ fontSize: '0.78rem', color: '#64748b' }}>
            {deliveries.filter((d) => d.status === 'IN_TRANSIT').length} In-Flight | {deliveries.filter((d) => d.status === 'PENDING').length} Pending Dispatch
          </span>
        </div>

        {loading ? (
          <p style={{ padding: '24px', textAlign: 'center', color: '#64748b' }}>Loading active deliveries from Java backend...</p>
        ) : deliveries.length === 0 ? (
          <div style={{ padding: '36px', textAlign: 'center', color: '#94a3b8' }}>
            <Truck size={36} color="#94a3b8" style={{ margin: '0 auto 12px' }} />
            <p style={{ fontWeight: 600, color: '#334155' }}>No Active Deliveries in Progress</p>
            <p style={{ fontSize: '0.82rem' }}>
              {pendingRequests.length > 0 ? 'Click "Plan New Delivery Mission" above to dispatch available drones.' : 'All relief requests have been satisfied.'}
            </p>
          </div>
        ) : (
          <div className="data-table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Delivery ID</th>
                  <th>Request ID</th>
                  <th>Assigned Drone</th>
                  <th>Destination Location</th>
                  <th>Dijkstra Shortest Path</th>
                  <th>Distance</th>
                  <th>Delivery Fee</th>
                  <th>Status</th>
                  <th>Operational Controls</th>
                </tr>
              </thead>
              <tbody>
                {deliveries.map((del) => {
                  const isInTransit = del.status === 'IN_TRANSIT';
                  const pathStr = del.pathNodeIds && del.pathNodeIds.length > 0 ? del.pathNodeIds.join(' ➔ ') : 'Direct';

                  return (
                    <tr key={del.id} style={{ backgroundColor: isInTransit ? '#fffbeb' : 'transparent' }}>
                      <td><strong>{del.id}</strong></td>
                      <td>
                        <span style={{ fontWeight: 600, color: '#2563eb' }}>{del.requestId}</span>
                      </td>
                      <td>
                        <strong>{getDroneName(del.droneId)}</strong>
                      </td>
                      <td>{getLocationName(del.destLocationId)}</td>
                      <td>
                        <div style={{ fontSize: '0.78rem', color: '#1e40af', backgroundColor: '#eff6ff', padding: '4px 8px', borderRadius: '4px', display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                          <Navigation size={12} />
                          <span>{pathStr}</span>
                        </div>
                      </td>
                      <td>{del.totalDistanceKm} km</td>
                      <td>
                        <strong style={{ color: '#16a34a' }}>₹{del.deliveryCost}</strong>
                      </td>
                      <td>
                        <span
                          className="badge"
                          style={{
                            backgroundColor: isInTransit ? '#ffedd5' : '#fef3c7',
                            color: isInTransit ? '#c2410c' : '#b45309'
                          }}
                        >
                          {isInTransit ? 'IN TRANSIT (Flying)' : 'PENDING DISPATCH'}
                        </span>
                      </td>
                      <td>
                        <div style={{ display: 'flex', gap: '6px' }}>
                          {del.status === 'PENDING' && (
                            <button
                              className="btn btn-primary"
                              style={{ padding: '4px 10px', fontSize: '0.75rem' }}
                              onClick={() => handleStartDelivery(del.id)}
                            >
                              <Play size={13} />
                              <span>Dispatch</span>
                            </button>
                          )}
                          {isInTransit && (
                            <button
                              className="btn btn-primary"
                              style={{ padding: '4px 10px', fontSize: '0.75rem', backgroundColor: '#16a34a' }}
                              onClick={() => handleCompleteDelivery(del.id)}
                            >
                              <CheckSquare size={13} />
                              <span>Complete Delivery</span>
                            </button>
                          )}
                          <button
                            className="btn btn-danger"
                            style={{ padding: '4px 8px', fontSize: '0.75rem' }}
                            onClick={() => handleCancelDelivery(del.id)}
                            title="Cancel Mission"
                          >
                            <XCircle size={13} />
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

      {/* Plan New Delivery Modal */}
      {showPlanModal && (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000, padding: '16px' }}>
          <div style={{ backgroundColor: 'white', borderRadius: '12px', width: '100%', maxWidth: '520px', padding: '24px', boxShadow: '0 20px 25px -5px rgba(0,0,0,0.2)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h3 style={{ fontSize: '1.15rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Truck size={20} color="#2563eb" /> Plan Relief Delivery Mission
              </h3>
              <button onClick={() => setShowPlanModal(false)} style={{ background: 'none', border: 'none', cursor: 'pointer' }}>
                <X size={18} />
              </button>
            </div>

            {planError && (
              <div style={{ backgroundColor: '#fee2e2', color: '#991b1b', padding: '8px 12px', borderRadius: '6px', fontSize: '0.82rem', marginBottom: '14px' }}>
                {planError}
              </div>
            )}

            <form onSubmit={handlePlanSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>
                  Select Pending Relief Request *
                </label>
                <select
                  required
                  value={selectedRequestId}
                  onChange={(e) => setSelectedRequestId(e.target.value)}
                  style={{ width: '100%', padding: '8px 10px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                >
                  {pendingRequests.map((req) => (
                    <option key={req.id} value={req.id}>
                      {req.id} – {getLocationName(req.locationId)} ({req.quantity} units, {req.urgency})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>
                  Select Drone (or Auto-Allocate Best Fit)
                </label>
                <select
                  value={selectedDroneId}
                  onChange={(e) => setSelectedDroneId(e.target.value)}
                  style={{ width: '100%', padding: '8px 10px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                >
                  <option value="">Auto-Allocate (Least Excess Capacity)</option>
                  {availableDrones.map((d) => (
                    <option key={d.id} value={d.id}>
                      {d.id} – {d.name} ({d.maxCapacityKg}kg cap, {d.batteryPercentage}% bat)
                    </option>
                  ))}
                </select>
              </div>

              <div style={{ backgroundColor: '#f8fafc', padding: '12px', borderRadius: '6px', border: '1px solid #e2e8f0', fontSize: '0.8rem', color: '#475569' }}>
                <strong>Automated Workflow on Creation:</strong>
                <ul style={{ paddingLeft: '18px', marginTop: '4px', lineHeight: 1.6 }}>
                  <li>Calculates shortest route via Java <strong>Dijkstra Algorithm</strong></li>
                  <li>Calculates cost: Base ₹100 + Distance ₹20/km + Emergency ₹50</li>
                  <li>Sets Drone &amp; Request status to <code>ASSIGNED</code></li>
                  <li>Reserves inventory supplies atomically</li>
                </ul>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '10px' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setShowPlanModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Create Mission Record</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
