import React, { useState, useEffect } from 'react';
import {
  HeartPulse,
  Plus,
  ArrowUpDown,
  Search,
  Filter,
  CheckCircle2,
  Clock,
  Send,
  X,
  AlertTriangle,
  Info,
  Layers,
  ChevronRight
} from 'lucide-react';
import AlertBadge from '../components/AlertBadge';
import { api } from '../services/api';

const URGENCY_LEVELS = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];
const STATUSES = ['ALL', 'PENDING', 'ASSIGNED', 'IN_TRANSIT', 'DELIVERED', 'CANCELLED'];

export default function ReliefRequests({ onNavigate }) {
  const [activeTab, setActiveTab] = useState('queue'); // 'queue' or 'all'
  const [queueRequests, setQueueRequests] = useState([]);
  const [allRequests, setAllRequests] = useState([]);
  const [locations, setLocations] = useState([]);
  const [resources, setResources] = useState([]);
  const [loading, setLoading] = useState(false);

  // Filters
  const [search, setSearch] = useState('');
  const [filterStatus, setFilterStatus] = useState('ALL');
  const [filterUrgency, setFilterUrgency] = useState('ALL');

  // Modals
  const [showAddModal, setShowAddModal] = useState(false);
  const [showAssignModal, setShowAssignModal] = useState(false);
  const [assigningRequest, setAssigningRequest] = useState(null);
  const [assignResult, setAssignResult] = useState(null);
  const [assignLoading, setAssignLoading] = useState(false);

  const [formData, setFormData] = useState({
    locationId: '',
    resourceId: '',
    quantity: '10',
    urgency: 'HIGH',
    affectedPeople: '150',
    description: ''
  });
  const [formError, setFormError] = useState('');
  const [notification, setNotification] = useState(null);

  const loadData = async () => {
    setLoading(true);
    try {
      const [queueData, allData, locList, resList] = await Promise.all([
        api.getRequestQueue(),
        api.getRequests(),
        api.getLocations(),
        api.getResources()
      ]);
      setQueueRequests(queueData || []);
      setAllRequests(allData || []);
      setLocations(locList || []);
      setResources(resList || []);
    } catch (err) {
      showToast('Error loading requests: ' + err.message, 'error');
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

  const handleOpenAssign = async (req) => {
    setAssigningRequest(req);
    setAssignLoading(true);
    setAssignResult(null);
    setShowAssignModal(true);
    try {
      const res = await api.assignDrone(req.id);
      setAssignResult(res);
      showToast('Drone assigned successfully!');
      loadData();
    } catch (err) {
      setAssignResult({ success: false, message: err.message });
    } finally {
      setAssignLoading(false);
    }
  };

  const handleOpenAdd = () => {
    setFormData({
      locationId: locations.length > 0 ? locations[0].id : '',
      resourceId: resources.length > 0 ? resources[0].id : '',
      quantity: '10',
      urgency: 'HIGH',
      affectedPeople: '150',
      description: ''
    });
    setFormError('');
    setShowAddModal(true);
  };

  const handleAddSubmit = async (e) => {
    e.preventDefault();
    if (!formData.locationId || !formData.resourceId) {
      setFormError('Please select both a target location and required resource.');
      return;
    }
    const qty = parseInt(formData.quantity, 10);
    const people = parseInt(formData.affectedPeople, 10);

    if (isNaN(qty) || qty <= 0) {
      setFormError('Requested quantity must be positive.');
      return;
    }
    if (isNaN(people) || people < 0) {
      setFormError('Affected people must be 0 or greater.');
      return;
    }

    try {
      await api.createRequest({
        locationId: formData.locationId,
        resourceId: formData.resourceId,
        quantity: qty,
        urgency: formData.urgency,
        affectedPeople: people,
        description: formData.description.trim()
      });
      setShowAddModal(false);
      showToast('Relief request submitted and enqueued in PriorityQueue!');
      loadData();
    } catch (err) {
      setFormError(err.message);
    }
  };

  const handleUpdateStatus = async (id, newStatus) => {
    try {
      await fetch(`http://localhost:8080/api/requests/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status: newStatus })
      });
      showToast(`Request ${id} status set to ${newStatus}`);
      loadData();
    } catch (err) {
      showToast('Failed to update status: ' + err.message, 'error');
    }
  };

  const getLocationName = (id) => {
    const loc = locations.find((l) => l.id === id);
    return loc ? `${loc.name} (${id})` : id;
  };

  const getResourceName = (id) => {
    const res = resources.find((r) => r.id === id);
    return res ? `${res.name} [${res.unit}]` : id;
  };

  const filteredAllRequests = allRequests.filter((r) => {
    const matchesSearch =
      r.id.toLowerCase().includes(search.toLowerCase()) ||
      r.locationId.toLowerCase().includes(search.toLowerCase()) ||
      (r.description && r.description.toLowerCase().includes(search.toLowerCase()));
    const matchesStatus = filterStatus === 'ALL' || r.status === filterStatus;
    const matchesUrgency = filterUrgency === 'ALL' || r.urgency === filterUrgency;
    return matchesSearch && matchesStatus && matchesUrgency;
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
          <h3 style={{ fontSize: '1.25rem', fontWeight: 700, color: '#0f172a' }}>Relief Request Dispatch & Prioritization</h3>
          <p style={{ fontSize: '0.82rem', color: '#64748b' }}>
            Emergency aid demands scheduled dynamically via Java <code>PriorityQueue</code> (Min-Heap comparator).
          </p>
        </div>
        <button className="btn btn-primary" onClick={handleOpenAdd}>
          <Plus size={16} />
          <span>New Relief Request</span>
        </button>
      </div>

      {/* Academic PriorityQueue Comparator Explainer Banner */}
      <div style={{ backgroundColor: '#eff6ff', border: '1px solid #bfdbfe', borderRadius: '8px', padding: '14px 18px', marginBottom: '20px', display: 'flex', alignItems: 'flex-start', gap: '12px' }}>
        <Info size={20} color="#2563eb" style={{ flexShrink: 0, marginTop: '2px' }} />
        <div style={{ fontSize: '0.84rem', color: '#1e40af', lineHeight: 1.6 }}>
          <strong>Java PriorityQueue Dispatch Hierarchy:</strong>
          <span style={{ marginLeft: '8px' }}>
            <strong>1. Urgency Level</strong> (<code>CRITICAL [4]</code> &gt; <code>HIGH [3]</code> &gt; <code>MEDIUM [2]</code> &gt; <code>LOW [1]</code>) &rarr;{' '}
            <strong>2. Affected Population</strong> (Higher count breaks ties) &rarr;{' '}
            <strong>3. Arrival Timestamp</strong> (FIFO fairness).
          </span>
        </div>
      </div>

      {/* View Tabs */}
      <div style={{ display: 'flex', gap: '8px', marginBottom: '20px', borderBottom: '1px solid #e2e8f0', paddingBottom: '12px' }}>
        <button
          className={`btn ${activeTab === 'queue' ? 'btn-primary' : 'btn-secondary'}`}
          onClick={() => setActiveTab('queue')}
        >
          <ArrowUpDown size={15} />
          <span>Priority Dispatch Queue ({queueRequests.length} Pending)</span>
        </button>
        <button
          className={`btn ${activeTab === 'all' ? 'btn-primary' : 'btn-secondary'}`}
          onClick={() => setActiveTab('all')}
        >
          <Layers size={15} />
          <span>All Requests Registry ({allRequests.length})</span>
        </button>
      </div>

      {/* Tab 1: Priority Queue */}
      {activeTab === 'queue' && (
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <h4 className="card-title" style={{ margin: 0 }}>
              <ArrowUpDown size={18} color="#dc2626" />
              <span>Pending Requests Ordered by Java PriorityQueue ({queueRequests.length})</span>
            </h4>
            <span style={{ fontSize: '0.78rem', color: '#64748b' }}>
              Highest priority is automatically placed at the root of the heap
            </span>
          </div>

          {loading ? (
            <p style={{ padding: '24px', textAlign: 'center', color: '#64748b' }}>Extracting heap order from Java PriorityQueue...</p>
          ) : queueRequests.length === 0 ? (
            <div style={{ padding: '36px', textAlign: 'center', color: '#94a3b8' }}>
              <CheckCircle2 size={36} color="#16a34a" style={{ margin: '0 auto 12px' }} />
              <p style={{ fontWeight: 600, color: '#334155' }}>Priority Queue is Empty!</p>
              <p style={{ fontSize: '0.82rem' }}>All emergency relief requests have been assigned or serviced.</p>
            </div>
          ) : (
            <div className="data-table-container">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Heap Rank</th>
                    <th>Request ID</th>
                    <th>Target Location</th>
                    <th>Resource Required</th>
                    <th>Quantity</th>
                    <th>Urgency Level</th>
                    <th>Affected Population</th>
                    <th>Waiting Time</th>
                    <th>Dispatch Action</th>
                  </tr>
                </thead>
                <tbody>
                  {queueRequests.map((req, idx) => {
                    const minutesWaiting = Math.round((Date.now() - req.requestTimestamp) / 60000);
                    const isRoot = idx === 0;

                    return (
                      <tr key={req.id} style={{ backgroundColor: isRoot ? '#fef2f2' : 'transparent' }}>
                        <td>
                          <span
                            style={{
                              display: 'inline-flex',
                              alignItems: 'center',
                              justifyContent: 'center',
                              width: '26px',
                              height: '26px',
                              borderRadius: '50%',
                              backgroundColor: isRoot ? '#ef4444' : '#64748b',
                              color: 'white',
                              fontWeight: 700,
                              fontSize: '0.78rem'
                            }}
                          >
                            #{idx + 1}
                          </span>
                          {isRoot && (
                            <span style={{ marginLeft: '6px', fontSize: '0.7rem', color: '#dc2626', fontWeight: 700 }}>
                              TOP ROOT
                            </span>
                          )}
                        </td>
                        <td><strong>{req.id}</strong></td>
                        <td style={{ fontWeight: 600 }}>{getLocationName(req.locationId)}</td>
                        <td>{getResourceName(req.resourceId)}</td>
                        <td><strong>{req.quantity}</strong> units</td>
                        <td><AlertBadge level={req.urgency} /></td>
                        <td>
                          <strong>{req.affectedPeople}</strong> people
                        </td>
                        <td style={{ color: '#64748b', fontSize: '0.8rem' }}>
                          <Clock size={12} style={{ display: 'inline', marginRight: '4px' }} />
                          {minutesWaiting > 0 ? `${minutesWaiting}m ago` : 'Just now'}
                        </td>
                        <td>
                          <button
                            className="btn btn-primary"
                            style={{ padding: '5px 12px', fontSize: '0.78rem', backgroundColor: isRoot ? '#dc2626' : '#2563eb' }}
                            onClick={() => handleOpenAssign(req)}
                          >
                            <Send size={13} />
                            <span>Assign Drone</span>
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* Tab 2: All Requests Registry */}
      {activeTab === 'all' && (
        <div>
          {/* Filter Bar */}
          <div className="card" style={{ padding: '16px 20px', marginBottom: '20px' }}>
            <div style={{ display: 'flex', gap: '16px', alignItems: 'center', flexWrap: 'wrap' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flex: 1, minWidth: '240px', backgroundColor: '#f8fafc', padding: '6px 12px', borderRadius: '6px', border: '1px solid #cbd5e1' }}>
                <Search size={16} color="#94a3b8" />
                <input
                  type="text"
                  placeholder="Search by Request ID, location, or notes..."
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
                  {STATUSES.map((s) => (
                    <option key={s} value={s}>{s}</option>
                  ))}
                </select>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span style={{ fontSize: '0.82rem', fontWeight: 600, color: '#475569' }}>Urgency:</span>
                <select
                  value={filterUrgency}
                  onChange={(e) => setFilterUrgency(e.target.value)}
                  style={{ padding: '6px 10px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '0.85rem' }}
                >
                  <option value="ALL">All Urgencies</option>
                  {URGENCY_LEVELS.map((u) => (
                    <option key={u} value={u}>{u}</option>
                  ))}
                </select>
              </div>
            </div>
          </div>

          {/* Table */}
          <div className="card">
            <h4 className="card-title">
              <Layers size={18} color="#2563eb" />
              <span>All Relief Requests Records ({filteredAllRequests.length})</span>
            </h4>

            {loading ? (
              <p style={{ padding: '20px', textAlign: 'center', color: '#64748b' }}>Loading records from backend...</p>
            ) : filteredAllRequests.length === 0 ? (
              <p style={{ padding: '20px', textAlign: 'center', color: '#94a3b8' }}>No requests found matching search criteria.</p>
            ) : (
              <div className="data-table-container">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Request ID</th>
                      <th>Location</th>
                      <th>Resource Needed</th>
                      <th>Quantity</th>
                      <th>Urgency</th>
                      <th>Affected People</th>
                      <th>Status</th>
                      <th>Description</th>
                      <th>Status Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredAllRequests.map((req) => (
                      <tr key={req.id}>
                        <td><strong>{req.id}</strong></td>
                        <td style={{ fontWeight: 600 }}>{getLocationName(req.locationId)}</td>
                        <td>{getResourceName(req.resourceId)}</td>
                        <td>{req.quantity} units</td>
                        <td><AlertBadge level={req.urgency} /></td>
                        <td>{req.affectedPeople}</td>
                        <td>
                          <span
                            className="badge"
                            style={{
                              backgroundColor:
                                req.status === 'PENDING' ? '#fef3c7' :
                                req.status === 'ASSIGNED' ? '#dbeafe' :
                                req.status === 'IN_TRANSIT' ? '#ffedd5' :
                                req.status === 'DELIVERED' ? '#dcfce7' : '#fee2e2',
                              color:
                                req.status === 'PENDING' ? '#b45309' :
                                req.status === 'ASSIGNED' ? '#1d4ed8' :
                                req.status === 'IN_TRANSIT' ? '#c2410c' :
                                req.status === 'DELIVERED' ? '#15803d' : '#991b1b'
                            }}
                          >
                            {req.status}
                          </span>
                        </td>
                        <td style={{ maxWidth: '180px', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', fontSize: '0.8rem', color: '#64748b' }}>
                          {req.description || 'No description'}
                        </td>
                        <td>
                          {req.status === 'PENDING' && (
                            <button
                              className="btn btn-secondary"
                              style={{ padding: '4px 8px', fontSize: '0.72rem', color: '#dc2626' }}
                              onClick={() => handleUpdateStatus(req.id, 'CANCELLED')}
                            >
                              Cancel
                            </button>
                          )}
                          {req.status === 'IN_TRANSIT' && (
                            <button
                              className="btn btn-secondary"
                              style={{ padding: '4px 8px', fontSize: '0.72rem', color: '#16a34a' }}
                              onClick={() => handleUpdateStatus(req.id, 'DELIVERED')}
                            >
                              Mark Delivered
                            </button>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}

      {/* New Relief Request Modal */}
      {showAddModal && (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000, padding: '16px' }}>
          <div style={{ backgroundColor: 'white', borderRadius: '12px', width: '100%', maxWidth: '520px', padding: '24px', boxShadow: '0 20px 25px -5px rgba(0,0,0,0.2)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h3 style={{ fontSize: '1.15rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '8px' }}>
                <HeartPulse size={20} color="#dc2626" /> Create Emergency Relief Request
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
                <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Target Affected Location *</label>
                <select
                  required
                  value={formData.locationId}
                  onChange={(e) => setFormData({ ...formData, locationId: e.target.value })}
                  style={{ width: '100%', padding: '8px 10px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                >
                  {locations.map((loc) => (
                    <option key={loc.id} value={loc.id}>{loc.id} – {loc.name}</option>
                  ))}
                </select>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Required Resource *</label>
                  <select
                    required
                    value={formData.resourceId}
                    onChange={(e) => setFormData({ ...formData, resourceId: e.target.value })}
                    style={{ width: '100%', padding: '8px 10px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                  >
                    {resources.map((res) => (
                      <option key={res.id} value={res.id}>{res.name} ({res.availableStock} {res.unit} avail)</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Quantity Needed *</label>
                  <input
                    type="number"
                    min="1"
                    required
                    value={formData.quantity}
                    onChange={(e) => setFormData({ ...formData, quantity: e.target.value })}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                  />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Urgency Level *</label>
                  <select
                    value={formData.urgency}
                    onChange={(e) => setFormData({ ...formData, urgency: e.target.value })}
                    style={{ width: '100%', padding: '8px 10px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                  >
                    {URGENCY_LEVELS.map((u) => (
                      <option key={u} value={u}>{u}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Affected People Count *</label>
                  <input
                    type="number"
                    min="0"
                    required
                    value={formData.affectedPeople}
                    onChange={(e) => setFormData({ ...formData, affectedPeople: e.target.value })}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                  />
                </div>
              </div>

              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Situation Description / Notes</label>
                <textarea
                  rows="3"
                  placeholder="e.g. Critical trauma kits required for flash flood evacuees..."
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '10px' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setShowAddModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Submit to Priority Queue</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Drone Assignment Result Modal */}
      {showAssignModal && assigningRequest && (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000, padding: '16px' }}>
          <div style={{ backgroundColor: 'white', borderRadius: '12px', width: '100%', maxWidth: '640px', padding: '24px', boxShadow: '0 20px 25px -5px rgba(0,0,0,0.2)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <div>
                <h3 style={{ fontSize: '1.15rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <Send size={20} color="#2563eb" /> Drone Allocation Engine – {assigningRequest.id}
                </h3>
                <span style={{ fontSize: '0.78rem', color: '#64748b' }}>
                  Target: {getLocationName(assigningRequest.locationId)} | Urgency: {assigningRequest.urgency}
                </span>
              </div>
              <button onClick={() => setShowAssignModal(false)} style={{ background: 'none', border: 'none', cursor: 'pointer' }}>
                <X size={18} />
              </button>
            </div>

            {assignLoading ? (
              <p style={{ padding: '24px', textAlign: 'center', color: '#64748b' }}>
                Evaluating fleet drones (payload capacity, battery buffers, and round-trip flight range)...
              </p>
            ) : assignResult ? (
              <div>
                <div
                  style={{
                    backgroundColor: assignResult.success ? '#f0fdf4' : '#fef2f2',
                    border: `1px solid ${assignResult.success ? '#bbf7d0' : '#fecaca'}`,
                    borderRadius: '8px',
                    padding: '14px',
                    marginBottom: '16px'
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: assignResult.success ? '#15803d' : '#991b1b', fontWeight: 700, fontSize: '0.9rem', marginBottom: '4px' }}>
                    {assignResult.success ? <CheckCircle2 size={18} /> : <AlertTriangle size={18} />}
                    <span>{assignResult.success ? 'Optimal Drone Successfully Selected & Assigned!' : 'Allocation Failed'}</span>
                  </div>
                  <p style={{ fontSize: '0.82rem', color: assignResult.success ? '#166534' : '#b91c1c' }}>
                    {assignResult.message}
                  </p>
                </div>

                {assignResult.selectedDrone && (
                  <div className="card" style={{ padding: '16px', marginBottom: '16px', backgroundColor: '#f8fafc' }}>
                    <div style={{ fontWeight: 600, fontSize: '0.88rem', marginBottom: '8px', color: '#0f172a' }}>
                      Assigned Aerial Asset Details:
                    </div>
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(130px, 1fr))', gap: '10px', fontSize: '0.8rem' }}>
                      <div><strong>Call-Sign:</strong> {assignResult.selectedDrone.name}</div>
                      <div><strong>ID:</strong> {assignResult.selectedDrone.id}</div>
                      <div><strong>Airframe:</strong> {assignResult.selectedDrone.droneType}</div>
                      <div><strong>Max Capacity:</strong> {assignResult.selectedDrone.maxCapacityKg} kg</div>
                      <div><strong>Battery Level:</strong> {assignResult.selectedDrone.batteryPercentage}%</div>
                      <div><strong>Max Range:</strong> {assignResult.selectedDrone.maxFlightRangeKm} km</div>
                    </div>
                  </div>
                )}

                {assignResult.evaluations && assignResult.evaluations.length > 0 && (
                  <div>
                    <h5 style={{ fontSize: '0.82rem', fontWeight: 700, color: '#475569', marginBottom: '8px', textTransform: 'uppercase' }}>
                      Fleet Candidate Evaluation Matrix:
                    </h5>
                    <div className="data-table-container">
                      <table className="data-table" style={{ fontSize: '0.78rem' }}>
                        <thead>
                          <tr>
                            <th>Drone</th>
                            <th>Status</th>
                            <th>Capacity Check</th>
                            <th>Battery Check</th>
                            <th>Result</th>
                          </tr>
                        </thead>
                        <tbody>
                          {assignResult.evaluations.map((ev, i) => (
                            <tr key={i} style={{ backgroundColor: ev.eligible ? '#f0fdf4' : 'transparent' }}>
                              <td><strong>{ev.drone.name}</strong> ({ev.drone.id})</td>
                              <td>{ev.drone.status}</td>
                              <td>{ev.drone.maxCapacityKg} kg (Excess: {ev.excessCapacityKg.toFixed(1)}kg)</td>
                              <td>{ev.drone.batteryPercentage}% (Req: {ev.requiredBattery}%)</td>
                              <td>
                                {ev.eligible ? (
                                  <span style={{ color: '#16a34a', fontWeight: 700 }}>MATCH (Selected)</span>
                                ) : (
                                  <span style={{ color: '#dc2626' }}>{ev.rejectionReason}</span>
                                )}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                )}
              </div>
            ) : null}

            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '16px' }}>
              <button className="btn btn-secondary" onClick={() => setShowAssignModal(false)}>Close Inspector</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
