import React from 'react';
import {
  Flame,
  MapPin,
  HeartPulse,
  Clock,
  RotateCw,
  CheckCircle2,
  Navigation,
  AlertTriangle,
  Send,
  PlusCircle,
  Eye,
  BatteryCharging,
  Zap,
  ArrowRight
} from 'lucide-react';
import StatCard from '../components/StatCard';
import AlertBadge from '../components/AlertBadge';

export default function Dashboard({ stats, onNavigate }) {
  const s = stats || {};

  return (
    <div>
      {/* 9 Core Metric Cards required by Section 3 */}
      <div className="stat-grid">
        <StatCard
          title="Total Disasters"
          value={s.totalDisasters}
          subtext="Active emergency events"
          icon={Flame}
          color="#dc2626"
          bgLight="#fee2e2"
        />
        <StatCard
          title="Affected Locations"
          value={s.totalLocations}
          subtext="Graph vertices mapped"
          icon={MapPin}
          color="#f97316"
          bgLight="#ffedd5"
        />
        <StatCard
          title="Total Relief Requests"
          value={s.totalRequests}
          subtext="Submitted mission needs"
          icon={HeartPulse}
          color="#2563eb"
          bgLight="#eff6ff"
        />
        <StatCard
          title="Pending Requests"
          value={s.pendingRequests}
          subtext="Queued in PriorityQueue"
          icon={Clock}
          color="#d97706"
          bgLight="#fef3c7"
        />
        <StatCard
          title="In Transit / Processing"
          value={s.processingRequests}
          subtext="Missions currently flying"
          icon={RotateCw}
          color="#7c3aed"
          bgLight="#f3e8ff"
        />
        <StatCard
          title="Completed Deliveries"
          value={s.completedDeliveries}
          subtext="Relief payloads delivered"
          icon={CheckCircle2}
          color="#16a34a"
          bgLight="#dcfce7"
        />
        <StatCard
          title="Available Drones"
          value={s.availableDrones}
          subtext="Ready for assignment"
          icon={Navigation}
          color="#059669"
          bgLight="#d1fae5"
        />
        <StatCard
          title="Busy / Active Drones"
          value={s.busyDrones}
          subtext="Assigned or in-flight"
          icon={Navigation}
          color="#ea580c"
          bgLight="#ffedd5"
        />
        <StatCard
          title="Emergency Requests"
          value={s.emergencyRequests}
          subtext="High & Critical priority"
          icon={AlertTriangle}
          color="#b91c1c"
          bgLight="#fef2f2"
        />
      </div>

      {/* Quick Operational Actions */}
      <div className="card" style={{ marginBottom: '24px' }}>
        <h3 className="card-title">
          <Send size={18} color="#2563eb" />
          <span>Quick Operational Actions</span>
        </h3>
        <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
          <button className="btn btn-primary" onClick={() => onNavigate('requests')}>
            <PlusCircle size={16} />
            <span>Create Relief Request</span>
          </button>
          <button className="btn btn-secondary" onClick={() => onNavigate('routes')}>
            <Navigation size={16} />
            <span>Run Dijkstra Route Optimizer</span>
          </button>
          <button className="btn btn-secondary" onClick={() => onNavigate('drones')}>
            <Eye size={16} />
            <span>View Drone Fleet Status</span>
          </button>
          <button className="btn btn-secondary" onClick={() => onNavigate('deliveries')}>
            <RotateCw size={16} />
            <span>Simulate Active Missions</span>
          </button>
          <button className="btn btn-secondary" onClick={() => onNavigate('reports')}>
            <Zap size={16} />
            <span>Executive Analytics</span>
          </button>
        </div>
      </div>

      {/* Two-Column Overview: Priority Queue & Recent Activities */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(460px, 1fr))', gap: '24px', marginBottom: '24px' }}>
        
        {/* Live Priority Queue Overview */}
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
            <h3 className="card-title" style={{ margin: 0 }}>
              <HeartPulse size={18} color="#dc2626" />
              <span>Priority Queue Dispatch Order</span>
            </h3>
            <button
              className="btn btn-secondary"
              style={{ fontSize: '0.75rem', padding: '4px 10px' }}
              onClick={() => onNavigate('requests')}
            >
              <span>View All ({s.pendingRequests || 0})</span>
              <ArrowRight size={13} />
            </button>
          </div>
          <p style={{ fontSize: '0.82rem', color: '#64748b', marginBottom: '14px' }}>
            Top items evaluated via Java <code>PriorityQueue</code> (Urgency &gt; Affected People &gt; FIFO).
          </p>

          <div className="data-table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Heap Rank</th>
                  <th>Request ID</th>
                  <th>Destination</th>
                  <th>Urgency</th>
                  <th>Resource</th>
                </tr>
              </thead>
              <tbody>
                {s.priorityQueuePreview && s.priorityQueuePreview.length > 0 ? (
                  s.priorityQueuePreview.map((req) => (
                    <tr key={req.id}>
                      <td>
                        <span
                          style={{
                            display: 'inline-block',
                            padding: '2px 8px',
                            borderRadius: '12px',
                            fontSize: '0.75rem',
                            fontWeight: 700,
                            backgroundColor: req.rank === 1 ? '#fee2e2' : '#f1f5f9',
                            color: req.rank === 1 ? '#dc2626' : '#475569'
                          }}
                        >
                          {req.rank === 1 ? 'Next (#1)' : `#${req.rank}`}
                        </span>
                      </td>
                      <td><strong>{req.id}</strong></td>
                      <td>
                        <div style={{ fontWeight: 600, fontSize: '0.82rem' }}>{req.locationName}</div>
                        <div style={{ fontSize: '0.72rem', color: '#64748b' }}>{req.affectedPeopleCount} people affected</div>
                      </td>
                      <td><AlertBadge level={req.urgency} /></td>
                      <td style={{ fontSize: '0.82rem' }}>
                        {req.quantityRequired} × {req.resourceName}
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan="5" style={{ textAlign: 'center', color: '#94a3b8', padding: '20px' }}>
                      Priority queue is currently empty. All relief requests serviced!
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Recent Operational Activities Feed */}
        <div className="card">
          <h3 className="card-title">
            <Clock size={18} color="#2563eb" />
            <span>Recent Operational Activities</span>
          </h3>
          <p style={{ fontSize: '0.82rem', color: '#64748b', marginBottom: '14px' }}>
            Real-time activity audit log synced from Java runtime events.
          </p>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {s.recentActivities && s.recentActivities.length > 0 ? (
              s.recentActivities.map((act) => (
                <div
                  key={act.id}
                  style={{
                    display: 'flex',
                    alignItems: 'flex-start',
                    justifyContent: 'space-between',
                    padding: '12px 14px',
                    backgroundColor: '#f8fafc',
                    borderRadius: '8px',
                    border: '1px solid #e2e8f0'
                  }}
                >
                  <div>
                    <div style={{ fontWeight: 600, fontSize: '0.88rem', color: '#0f172a' }}>
                      {act.action}
                    </div>
                    <div style={{ fontSize: '0.8rem', color: '#64748b', marginTop: '2px' }}>
                      {act.details}
                    </div>
                  </div>
                  <span
                    style={{
                      fontSize: '0.72rem',
                      fontWeight: 600,
                      color: act.time === 'In Transit' ? '#7c3aed' : act.time === 'Delivered' ? '#16a34a' : '#2563eb',
                      backgroundColor: act.time === 'In Transit' ? '#f3e8ff' : act.time === 'Delivered' ? '#dcfce7' : '#eff6ff',
                      padding: '3px 8px',
                      borderRadius: '12px',
                      whiteSpace: 'nowrap'
                    }}
                  >
                    {act.time}
                  </span>
                </div>
              ))
            ) : (
              <p style={{ fontSize: '0.85rem', color: '#94a3b8' }}>No recent activity records available.</p>
            )}
          </div>
        </div>
      </div>

      {/* Drone Fleet Status Overview Snapshot */}
      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
          <h3 className="card-title" style={{ margin: 0 }}>
            <Navigation size={18} color="#059669" />
            <span>Drone Fleet Readiness Snapshot</span>
          </h3>
          <button
            className="btn btn-secondary"
            style={{ fontSize: '0.75rem', padding: '4px 10px' }}
            onClick={() => onNavigate('drones')}
          >
            <span>Manage Fleet</span>
            <ArrowRight size={13} />
          </button>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '16px' }}>
          {s.droneFleetPreview && s.droneFleetPreview.length > 0 ? (
            s.droneFleetPreview.map((d) => {
              const batColor = d.batteryPercentage > 50 ? '#16a34a' : d.batteryPercentage > 20 ? '#d97706' : '#dc2626';

              return (
                <div
                  key={d.id}
                  style={{
                    border: '1px solid #e2e8f0',
                    borderRadius: '8px',
                    padding: '14px',
                    backgroundColor: '#ffffff'
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                    <strong style={{ fontSize: '0.88rem', color: '#0f172a' }}>{d.name}</strong>
                    <span
                      style={{
                        fontSize: '0.7rem',
                        fontWeight: 700,
                        padding: '2px 8px',
                        borderRadius: '10px',
                        backgroundColor: d.status === 'AVAILABLE' ? '#dcfce7' : d.status === 'IN_FLIGHT' ? '#ffedd5' : d.status === 'ASSIGNED' ? '#eff6ff' : '#fee2e2',
                        color: d.status === 'AVAILABLE' ? '#16a34a' : d.status === 'IN_FLIGHT' ? '#ea580c' : d.status === 'ASSIGNED' ? '#2563eb' : '#dc2626'
                      }}
                    >
                      {d.status}
                    </span>
                  </div>

                  <div style={{ fontSize: '0.76rem', color: '#64748b', marginBottom: '8px' }}>
                    Stationed at: <strong>{d.currentLocationName}</strong>
                  </div>

                  {/* Battery Bar */}
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', fontSize: '0.76rem', marginBottom: '4px' }}>
                    <span style={{ color: '#475569', display: 'flex', alignItems: 'center', gap: '4px' }}>
                      <BatteryCharging size={13} color={batColor} /> Battery
                    </span>
                    <strong style={{ color: batColor }}>{d.batteryPercentage}%</strong>
                  </div>
                  <div style={{ width: '100%', height: '6px', backgroundColor: '#e2e8f0', borderRadius: '3px', overflow: 'hidden' }}>
                    <div
                      style={{
                        width: `${d.batteryPercentage}%`,
                        height: '100%',
                        backgroundColor: batColor,
                        borderRadius: '3px'
                      }}
                    />
                  </div>

                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.72rem', color: '#64748b', marginTop: '8px', borderTop: '1px solid #f8fafc', paddingTop: '6px' }}>
                    <span>Payload: <strong>{d.capacityKg} kg</strong></span>
                    <span>Range: <strong>{d.maxFlightRangeKm} km</strong></span>
                  </div>
                </div>
              );
            })
          ) : (
            <p style={{ fontSize: '0.85rem', color: '#94a3b8' }}>Loading fleet snapshot...</p>
          )}
        </div>
      </div>
    </div>
  );
}
