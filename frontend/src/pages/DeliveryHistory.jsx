import React, { useState, useEffect } from 'react';
import {
  History,
  Search,
  Download,
  IndianRupee,
  Navigation,
  CheckCircle2,
  XCircle,
  Clock,
  Filter
} from 'lucide-react';
import StatCard from '../components/StatCard';
import { api } from '../services/api';

export default function DeliveryHistory() {
  const [history, setHistory] = useState([]);
  const [drones, setDrones] = useState([]);
  const [locations, setLocations] = useState([]);
  const [loading, setLoading] = useState(false);

  // Filters
  const [search, setSearch] = useState('');
  const [filterDrone, setFilterDrone] = useState('ALL');
  const [filterStatus, setFilterStatus] = useState('ALL');
  const [filterDest, setFilterDest] = useState('ALL');

  const loadData = async () => {
    setLoading(true);
    try {
      const [histData, droneList, locList] = await Promise.all([
        fetch('http://localhost:8080/api/history').then((r) => r.json()),
        api.getDrones(),
        api.getLocations()
      ]);
      setHistory(histData || []);
      setDrones(droneList || []);
      setLocations(locList || []);
    } catch (err) {
      console.error('Error loading history:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const getLocationName = (id) => {
    const loc = locations.find((l) => l.id === id);
    return loc ? `${loc.name} (${id})` : id;
  };

  const getDroneName = (id) => {
    const d = drones.find((dr) => dr.id === id);
    return d ? `${d.name} (${id})` : id;
  };

  // Filtered deliveries
  const filteredHistory = history.filter((d) => {
    const matchesSearch =
      d.id.toLowerCase().includes(search.toLowerCase()) ||
      d.requestId.toLowerCase().includes(search.toLowerCase());
    const matchesDrone = filterDrone === 'ALL' || d.droneId === filterDrone;
    const matchesStatus = filterStatus === 'ALL' || d.status === filterStatus;
    const matchesDest = filterDest === 'ALL' || d.destLocationId === filterDest;
    return matchesSearch && matchesDrone && matchesStatus && matchesDest;
  });

  // Calculate historical statistics
  const completedMissions = history.filter((d) => d.status === 'COMPLETED');
  const totalKmFlown = completedMissions.reduce((acc, d) => acc + (d.totalDistanceKm || 0), 0);
  const totalCost = completedMissions.reduce((acc, d) => acc + (d.deliveryCost || 0), 0);
  const avgDistance = completedMissions.length > 0 ? (totalKmFlown / completedMissions.length).toFixed(1) : 0;

  const handleExportCsv = () => {
    if (history.length === 0) return;
    const headers = ['Delivery ID', 'Request ID', 'Drone ID', 'Source', 'Destination', 'Distance (km)', 'Cost (INR)', 'Status', 'Date'];
    const rows = history.map((d) => [
      d.id,
      d.requestId,
      d.droneId,
      d.sourceLocationId,
      d.destLocationId,
      d.totalDistanceKm,
      d.deliveryCost,
      d.status,
      d.dispatchTime ? new Date(d.dispatchTime).toISOString() : 'N/A'
    ]);

    const csvContent = 'data:text/csv;charset=utf-8,' +
      [headers.join(','), ...rows.map((e) => e.join(','))].join('\n');
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `rescuedrone_delivery_history_${Date.now()}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <div>
      {/* Header Bar */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h3 style={{ fontSize: '1.25rem', fontWeight: 700, color: '#0f172a' }}>Delivery History & Mission Logs</h3>
          <p style={{ fontSize: '0.82rem', color: '#64748b' }}>
            Archived logs of completed and cancelled drone relief missions persisted in Java CSV records.
          </p>
        </div>
        <button className="btn btn-secondary" onClick={handleExportCsv} disabled={history.length === 0}>
          <Download size={16} />
          <span>Export History CSV</span>
        </button>
      </div>

      {/* KPI Summary Cards */}
      <div className="stat-grid" style={{ marginBottom: '20px' }}>
        <StatCard
          title="Completed Missions"
          value={completedMissions.length}
          subtext="Relief payloads delivered"
          icon={CheckCircle2}
          color="#16a34a"
          bgLight="#dcfce7"
        />
        <StatCard
          title="Total Kilometres Flown"
          value={`${totalKmFlown.toFixed(1)} km`}
          subtext="Cumulative relief corridor flights"
          icon={Navigation}
          color="#2563eb"
          bgLight="#eff6ff"
        />
        <StatCard
          title="Total Simulated Logistics Cost"
          value={`₹${totalCost.toFixed(0)}`}
          subtext="Formula: Base + Dist*20 + Emergency"
          icon={IndianRupee}
          color="#7c3aed"
          bgLight="#f3e8ff"
        />
        <StatCard
          title="Average Mission Distance"
          value={`${avgDistance} km`}
          subtext="Per completed relief run"
          icon={Clock}
          color="#d97706"
          bgLight="#fef3c7"
        />
      </div>

      {/* Filter and Search Bar */}
      <div className="card" style={{ padding: '16px 20px', marginBottom: '20px' }}>
        <div style={{ display: 'flex', gap: '16px', alignItems: 'center', flexWrap: 'wrap' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flex: 1, minWidth: '220px', backgroundColor: '#f8fafc', padding: '6px 12px', borderRadius: '6px', border: '1px solid #cbd5e1' }}>
            <Search size={16} color="#94a3b8" />
            <input
              type="text"
              placeholder="Search by Delivery ID or Request ID..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              style={{ border: 'none', background: 'transparent', outline: 'none', width: '100%', fontSize: '0.88rem' }}
            />
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ fontSize: '0.82rem', fontWeight: 600, color: '#475569' }}>Drone:</span>
            <select
              value={filterDrone}
              onChange={(e) => setFilterDrone(e.target.value)}
              style={{ padding: '6px 10px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '0.85rem' }}
            >
              <option value="ALL">All Fleet Drones</option>
              {drones.map((d) => (
                <option key={d.id} value={d.id}>{d.name} ({d.id})</option>
              ))}
            </select>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ fontSize: '0.82rem', fontWeight: 600, color: '#475569' }}>Destination:</span>
            <select
              value={filterDest}
              onChange={(e) => setFilterDest(e.target.value)}
              style={{ padding: '6px 10px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '0.85rem' }}
            >
              <option value="ALL">All Destinations</option>
              {locations.map((l) => (
                <option key={l.id} value={l.id}>{l.name} ({l.id})</option>
              ))}
            </select>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ fontSize: '0.82rem', fontWeight: 600, color: '#475569' }}>Status:</span>
            <select
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value)}
              style={{ padding: '6px 10px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '0.85rem' }}
            >
              <option value="ALL">All Outcomes</option>
              <option value="COMPLETED">Completed</option>
              <option value="CANCELLED">Cancelled</option>
            </select>
          </div>
        </div>
      </div>

      {/* History Table */}
      <div className="card">
        <h4 className="card-title">
          <History size={18} color="#2563eb" />
          <span>Historical Mission Records ({filteredHistory.length})</span>
        </h4>

        {loading ? (
          <p style={{ padding: '24px', textAlign: 'center', color: '#64748b' }}>Loading delivery history from backend CSV storage...</p>
        ) : filteredHistory.length === 0 ? (
          <p style={{ padding: '24px', textAlign: 'center', color: '#94a3b8' }}>No delivery history records found matching criteria.</p>
        ) : (
          <div className="data-table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Delivery ID</th>
                  <th>Request ID</th>
                  <th>Dispatched Drone</th>
                  <th>Destination Location</th>
                  <th>Flight Path Hops</th>
                  <th>Distance</th>
                  <th>Calculated Cost</th>
                  <th>Outcome Status</th>
                  <th>Timestamp</th>
                </tr>
              </thead>
              <tbody>
                {filteredHistory.map((d) => {
                  const isCompleted = d.status === 'COMPLETED';
                  const dateStr = d.dispatchTime ? new Date(d.dispatchTime).toLocaleString() : 'Recorded in Seed';
                  const pathJoined = d.pathNodeIds && d.pathNodeIds.length > 0 ? d.pathNodeIds.join(' ➔ ') : 'Direct';

                  return (
                    <tr key={d.id}>
                      <td><strong>{d.id}</strong></td>
                      <td>
                        <span style={{ fontWeight: 600, color: '#2563eb' }}>{d.requestId}</span>
                      </td>
                      <td>{getDroneName(d.droneId)}</td>
                      <td>{getLocationName(d.destLocationId)}</td>
                      <td>
                        <span style={{ fontSize: '0.78rem', color: '#475569', backgroundColor: '#f1f5f9', padding: '2px 8px', borderRadius: '4px' }}>
                          {pathJoined}
                        </span>
                      </td>
                      <td>
                        <strong>{d.totalDistanceKm} km</strong>
                      </td>
                      <td>
                        <strong style={{ color: '#16a34a' }}>₹{d.deliveryCost}</strong>
                      </td>
                      <td>
                        <span
                          className="badge"
                          style={{
                            backgroundColor: isCompleted ? '#dcfce7' : '#fee2e2',
                            color: isCompleted ? '#15803d' : '#991b1b'
                          }}
                        >
                          {d.status}
                        </span>
                      </td>
                      <td style={{ fontSize: '0.78rem', color: '#64748b' }}>
                        {dateStr}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
