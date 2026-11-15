import React, { useState, useEffect } from 'react';
import {
  GitFork,
  Navigation,
  Play,
  CheckCircle2,
  AlertTriangle,
  Plus,
  Trash2,
  X,
  IndianRupee,
  MapPin,
  RefreshCw,
  Info
} from 'lucide-react';
import { api } from '../services/api';

export default function RouteOptimization() {
  const [locations, setLocations] = useState([]);
  const [routes, setRoutes] = useState([]);
  const [sourceId, setSourceId] = useState('LOC-001');
  const [destId, setDestId] = useState('LOC-003');
  const [optimizing, setOptimizing] = useState(false);
  const [pathResult, setPathResult] = useState(null);
  const [notification, setNotification] = useState(null);

  // Add Route Corridor Modal
  const [showAddRouteModal, setShowAddRouteModal] = useState(false);
  const [newRouteData, setNewRouteData] = useState({
    sourceLocationId: '',
    destLocationId: '',
    distanceKm: '8.0'
  });
  const [routeFormError, setRouteFormError] = useState('');

  const loadData = async () => {
    try {
      const [locList, routeList] = await Promise.all([
        api.getLocations(),
        api.getRoutes()
      ]);
      setLocations(locList || []);
      setRoutes(routeList || []);
      if (locList && locList.length >= 2) {
        if (!sourceId) setSourceId(locList[0].id);
        if (!destId) setDestId(locList[1].id);
      }
    } catch (err) {
      showToast('Error loading graph topology: ' + err.message, 'error');
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const showToast = (message, type = 'success') => {
    setNotification({ message, type });
    setTimeout(() => setNotification(null), 4000);
  };

  const handleRunDijkstra = async () => {
    if (!sourceId || !destId) {
      showToast('Please select both a source and destination location.', 'error');
      return;
    }
    setOptimizing(true);
    setPathResult(null);
    try {
      const res = await api.optimizeRoute(sourceId, destId);
      setPathResult(res);
      showToast('Shortest path solved via Java Dijkstra algorithm!');
    } catch (err) {
      setPathResult({
        found: false,
        message: err.message || 'No connecting route corridor found between selected locations.'
      });
      showToast('Dijkstra: ' + err.message, 'error');
    } finally {
      setOptimizing(false);
    }
  };

  const handleAddRouteSubmit = async (e) => {
    e.preventDefault();
    if (!newRouteData.sourceLocationId || !newRouteData.destLocationId) {
      setRouteFormError('Please select both source and destination locations.');
      return;
    }
    if (newRouteData.sourceLocationId === newRouteData.destLocationId) {
      setRouteFormError('Source and destination cannot be the same location.');
      return;
    }
    const dist = parseFloat(newRouteData.distanceKm);
    if (isNaN(dist) || dist <= 0) {
      setRouteFormError('Distance must be a positive number.');
      return;
    }

    try {
      await api.createRoute({
        sourceLocationId: newRouteData.sourceLocationId,
        destLocationId: newRouteData.destLocationId,
        distanceKm: dist
      });
      setShowAddRouteModal(false);
      showToast('New route corridor added to graph!');
      loadData();
    } catch (err) {
      setRouteFormError(err.message);
    }
  };

  const handleDeleteRoute = async (id) => {
    if (!window.confirm(`Delete route corridor ${id}?`)) return;
    try {
      await fetch(`http://localhost:8080/api/routes/${id}`, { method: 'DELETE' });
      showToast(`Route ${id} deleted.`);
      loadData();
    } catch (err) {
      showToast('Failed to delete route: ' + err.message, 'error');
    }
  };

  const getLocationName = (id) => {
    const loc = locations.find((l) => l.id === id);
    return loc ? `${loc.name} (${id})` : id;
  };

  // SVG Graph Coordinate Mapping
  // Normalize locations into a 640x360 SVG canvas bounding box
  const getCoordinates = (loc, index) => {
    // Proportional positioning or default distributed layout
    const coordsMap = {
      'LOC-001': { x: 100, y: 180 }, // Central Base Hub
      'LOC-002': { x: 260, y: 90 },  // Camp A
      'LOC-003': { x: 520, y: 160 }, // East Ridge Village
      'LOC-004': { x: 300, y: 270 }, // Relief Post 2
      'LOC-005': { x: 420, y: 70 },  // Riverbank Shelter
      'LOC-006': { x: 160, y: 310 }  // South Clinic
    };

    if (coordsMap[loc.id]) return coordsMap[loc.id];
    // Dynamic circular layout fallback for custom added vertices
    const total = locations.length || 1;
    const angle = (index / total) * 2 * Math.PI;
    return {
      x: Math.round(320 + 200 * Math.cos(angle)),
      y: Math.round(180 + 130 * Math.sin(angle))
    };
  };

  // Helper to determine if an edge is part of the Dijkstra shortest path
  const isEdgeInPath = (u, v) => {
    if (!pathResult || !pathResult.found || !pathResult.pathNodeIds) return false;
    const path = pathResult.pathNodeIds;
    for (let i = 0; i < path.length - 1; i++) {
      if (
        (path[i] === u && path[i + 1] === v) ||
        (path[i] === v && path[i + 1] === u)
      ) {
        return true;
      }
    }
    return false;
  };

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
          <h3 style={{ fontSize: '1.25rem', fontWeight: 700, color: '#0f172a' }}>Dijkstra Route Optimizer & Network Graph</h3>
          <p style={{ fontSize: '0.82rem', color: '#64748b' }}>
            Calculates shortest delivery paths over the transportation adjacency graph using custom Java Dijkstra algorithm.
          </p>
        </div>
        <button className="btn btn-primary" onClick={() => setShowAddRouteModal(true)}>
          <Plus size={16} />
          <span>Add Route Corridor</span>
        </button>
      </div>

      {/* Top Section: Controller & Interactive SVG Graph Visualizer */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))', gap: '24px', marginBottom: '24px' }}>
        {/* Controls Card */}
        <div className="card">
          <h4 className="card-title">
            <Navigation size={18} color="#2563eb" />
            <span>Shortest Route Calculation Engine</span>
          </h4>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div>
              <label style={{ fontSize: '0.82rem', fontWeight: 600, color: '#475569', display: 'block', marginBottom: '6px' }}>
                Source Station (Dispatch Hub):
              </label>
              <select
                value={sourceId}
                onChange={(e) => setSourceId(e.target.value)}
                style={{ width: '100%', padding: '9px 12px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '0.88rem' }}
              >
                {locations.map((loc) => (
                  <option key={loc.id} value={loc.id}>
                    {loc.id} – {loc.name}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label style={{ fontSize: '0.82rem', fontWeight: 600, color: '#475569', display: 'block', marginBottom: '6px' }}>
                Destination (Affected Location):
              </label>
              <select
                value={destId}
                onChange={(e) => setDestId(e.target.value)}
                style={{ width: '100%', padding: '9px 12px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '0.88rem' }}
              >
                {locations.map((loc) => (
                  <option key={loc.id} value={loc.id}>
                    {loc.id} – {loc.name}
                  </option>
                ))}
              </select>
            </div>

            <button
              className="btn btn-primary"
              onClick={handleRunDijkstra}
              disabled={optimizing}
              style={{ width: '100%', padding: '10px' }}
            >
              {optimizing ? <RefreshCw size={16} className="spin" /> : <Play size={16} />}
              <span>{optimizing ? 'Executing Dijkstra Algorithm...' : 'Run Java Dijkstra Algorithm'}</span>
            </button>
          </div>

          {/* Results Display */}
          {pathResult && (
            <div
              style={{
                marginTop: '20px',
                padding: '16px',
                borderRadius: '8px',
                backgroundColor: pathResult.found ? '#f0fdf4' : '#fef2f2',
                border: `1px solid ${pathResult.found ? '#bbf7d0' : '#fecaca'}`
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: pathResult.found ? '#15803d' : '#991b1b', fontWeight: 700, fontSize: '0.9rem', marginBottom: '8px' }}>
                {pathResult.found ? <CheckCircle2 size={18} /> : <AlertTriangle size={18} />}
                <span>{pathResult.found ? 'Optimal Shortest Path Found!' : 'Routing Failed'}</span>
              </div>

              <p style={{ fontSize: '0.82rem', color: '#475569', marginBottom: '10px' }}>
                {pathResult.message}
              </p>

              {pathResult.found && (
                <div>
                  <div style={{ marginBottom: '8px', fontSize: '0.85rem' }}>
                    <strong>Ordered Route:</strong>{' '}
                    <span style={{ color: '#16a34a', fontWeight: 700 }}>
                      {pathResult.pathNodeIds.map(getLocationName).join(' ➔ ')}
                    </span>
                  </div>

                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px', fontSize: '0.82rem', backgroundColor: 'white', padding: '10px', borderRadius: '6px', border: '1px solid #e2e8f0', marginBottom: '10px' }}>
                    <div><strong>Total Distance:</strong> {pathResult.totalDistanceKm} km</div>
                    <div><strong>Hops:</strong> {pathResult.hopCount} intermediate segments</div>
                  </div>

                  {pathResult.costBreakdown && (
                    <div style={{ fontSize: '0.8rem', color: '#1e40af', backgroundColor: '#eff6ff', padding: '10px', borderRadius: '6px', border: '1px solid #bfdbfe' }}>
                      <div style={{ fontWeight: 700, display: 'flex', alignItems: 'center', gap: '4px', marginBottom: '4px' }}>
                        <IndianRupee size={14} /> Estimated Delivery Cost Breakdown:
                      </div>
                      <div>Base Charge: ₹{pathResult.costBreakdown.baseCharge}</div>
                      <div>Distance Charge: {pathResult.totalDistanceKm}km @ ₹20/km = ₹{pathResult.costBreakdown.distanceCharge}</div>
                      <div>Emergency Surcharge: ₹{pathResult.costBreakdown.emergencyCharge}</div>
                      <div style={{ fontWeight: 700, marginTop: '4px', borderTop: '1px solid #bfdbfe', paddingTop: '4px' }}>
                        Total Simulated Cost: ₹{pathResult.costBreakdown.totalCost}
                      </div>
                    </div>
                  )}
                </div>
              )}
            </div>
          )}
        </div>

        {/* Interactive SVG Graph Visualizer */}
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
            <h4 className="card-title" style={{ margin: 0 }}>
              <GitFork size={18} color="#7c3aed" />
              <span>Interactive Transportation Graph</span>
            </h4>
            <span style={{ fontSize: '0.72rem', color: '#64748b' }}>
              Green highlight indicates Dijkstra shortest path
            </span>
          </div>

          <div style={{ width: '100%', height: '360px', backgroundColor: '#0f172a', borderRadius: '8px', position: 'relative', overflow: 'hidden', border: '1px solid #334155' }}>
            <svg width="100%" height="100%" viewBox="0 0 640 360" style={{ display: 'block' }}>
              {/* Edges */}
              {routes.map((route) => {
                const srcLoc = locations.find((l) => l.id === route.sourceLocationId);
                const dstLoc = locations.find((l) => l.id === route.destLocationId);
                if (!srcLoc || !dstLoc) return null;

                const srcIdx = locations.indexOf(srcLoc);
                const dstIdx = locations.indexOf(dstLoc);
                const c1 = getCoordinates(srcLoc, srcIdx);
                const c2 = getCoordinates(dstLoc, dstIdx);

                const inPath = isEdgeInPath(route.sourceLocationId, route.destLocationId);
                const strokeColor = inPath ? '#22c55e' : '#475569';
                const strokeWidth = inPath ? 4 : 1.5;

                // Midpoint for distance badge
                const midX = (c1.x + c2.x) / 2;
                const midY = (c1.y + c2.y) / 2;

                return (
                  <g key={route.id}>
                    <line
                      x1={c1.x}
                      y1={c1.y}
                      x2={c2.x}
                      y2={c2.y}
                      stroke={strokeColor}
                      strokeWidth={strokeWidth}
                      strokeDasharray={inPath ? '6 3' : 'none'}
                    />
                    <circle cx={midX} cy={midY} r={10} fill="#1e293b" stroke={strokeColor} strokeWidth={1} />
                    <text
                      x={midX}
                      y={midY + 3.5}
                      fill="#94a3b8"
                      fontSize="9"
                      fontWeight="bold"
                      textAnchor="middle"
                    >
                      {route.distanceKm}
                    </text>
                  </g>
                );
              })}

              {/* Vertices (Nodes) */}
              {locations.map((loc, idx) => {
                const coords = getCoordinates(loc, idx);
                const isSource = loc.id === sourceId;
                const isDest = loc.id === destId;
                const inShortestPath = pathResult && pathResult.found && pathResult.pathNodeIds.includes(loc.id);

                let nodeColor = '#3b82f6'; // default blue
                if (loc.id === 'LOC-001') nodeColor = '#06b6d4'; // base station cyan
                if (isSource) nodeColor = '#22c55e'; // green source
                if (isDest) nodeColor = '#ef4444'; // red destination
                if (inShortestPath && !isSource && !isDest) nodeColor = '#10b981'; // path waypoint emerald

                return (
                  <g
                    key={loc.id}
                    onClick={() => {
                      if (!sourceId || (sourceId && destId)) {
                        setSourceId(loc.id);
                        setDestId('');
                      } else {
                        setDestId(loc.id);
                      }
                    }}
                    style={{ cursor: 'pointer' }}
                  >
                    <circle
                      cx={coords.x}
                      cy={coords.y}
                      r={inShortestPath ? 18 : 14}
                      fill={nodeColor}
                      stroke="#ffffff"
                      strokeWidth={inShortestPath ? 3 : 2}
                      filter="drop-shadow(0 2px 4px rgba(0,0,0,0.4))"
                    />
                    <text
                      x={coords.x}
                      y={coords.y + 4}
                      fill="white"
                      fontSize="9"
                      fontWeight="bold"
                      textAnchor="middle"
                    >
                      {loc.id.replace('LOC-', 'L')}
                    </text>
                    <text
                      x={coords.x}
                      y={coords.y + 26}
                      fill="#e2e8f0"
                      fontSize="10"
                      fontWeight="600"
                      textAnchor="middle"
                    >
                      {loc.name.length > 18 ? loc.name.substring(0, 16) + '...' : loc.name}
                    </text>
                  </g>
                );
              })}
            </svg>
          </div>
        </div>
      </div>

      {/* Route Corridors Management Table */}
      <div className="card">
        <h4 className="card-title">
          <GitFork size={18} color="#2563eb" />
          <span>Registered Transportation Corridors ({routes.length} Graph Edges)</span>
        </h4>

        <div className="data-table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Route ID</th>
                <th>Source Location</th>
                <th>Destination Location</th>
                <th>Distance (km)</th>
                <th>Connection Type</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {routes.map((route) => (
                <tr key={route.id}>
                  <td><strong>{route.id}</strong></td>
                  <td>{getLocationName(route.sourceLocationId)}</td>
                  <td>{getLocationName(route.destLocationId)}</td>
                  <td>
                    <strong>{route.distanceKm} km</strong>
                  </td>
                  <td>
                    <span style={{ fontSize: '0.78rem', backgroundColor: '#f1f5f9', padding: '2px 8px', borderRadius: '4px', fontWeight: 600 }}>
                      Bidirectional Corridor
                    </span>
                  </td>
                  <td>
                    <button
                      className="btn btn-danger"
                      style={{ padding: '4px 8px', fontSize: '0.75rem' }}
                      onClick={() => handleDeleteRoute(route.id)}
                      title="Remove Route Corridor"
                    >
                      <Trash2 size={13} />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Add Route Corridor Modal */}
      {showAddRouteModal && (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000, padding: '16px' }}>
          <div style={{ backgroundColor: 'white', borderRadius: '12px', width: '100%', maxWidth: '480px', padding: '24px', boxShadow: '0 20px 25px -5px rgba(0,0,0,0.2)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h3 style={{ fontSize: '1.15rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '8px' }}>
                <GitFork size={20} color="#2563eb" /> Add Graph Route Corridor
              </h3>
              <button onClick={() => setShowAddRouteModal(false)} style={{ background: 'none', border: 'none', cursor: 'pointer' }}>
                <X size={18} />
              </button>
            </div>

            {routeFormError && (
              <div style={{ backgroundColor: '#fee2e2', color: '#991b1b', padding: '8px 12px', borderRadius: '6px', fontSize: '0.82rem', marginBottom: '14px' }}>
                {routeFormError}
              </div>
            )}

            <form onSubmit={handleAddRouteSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Source Location *</label>
                <select
                  required
                  value={newRouteData.sourceLocationId}
                  onChange={(e) => setNewRouteData({ ...newRouteData, sourceLocationId: e.target.value })}
                  style={{ width: '100%', padding: '8px 10px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                >
                  <option value="">Select Source Location</option>
                  {locations.map((loc) => (
                    <option key={loc.id} value={loc.id}>{loc.id} – {loc.name}</option>
                  ))}
                </select>
              </div>

              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Destination Location *</label>
                <select
                  required
                  value={newRouteData.destLocationId}
                  onChange={(e) => setNewRouteData({ ...newRouteData, destLocationId: e.target.value })}
                  style={{ width: '100%', padding: '8px 10px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                >
                  <option value="">Select Destination Location</option>
                  {locations.map((loc) => (
                    <option key={loc.id} value={loc.id}>{loc.id} – {loc.name}</option>
                  ))}
                </select>
              </div>

              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155', display: 'block', marginBottom: '4px' }}>Route Distance in Kilometres (Weight) *</label>
                <input
                  type="number"
                  step="0.1"
                  min="0.1"
                  required
                  value={newRouteData.distanceKm}
                  onChange={(e) => setNewRouteData({ ...newRouteData, distanceKm: e.target.value })}
                  style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '10px' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setShowAddRouteModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Add Route Corridor</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
