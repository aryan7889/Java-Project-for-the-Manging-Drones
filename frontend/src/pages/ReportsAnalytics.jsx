import React, { useState, useEffect } from 'react';
import {
  BarChart3,
  TrendingUp,
  IndianRupee,
  ShieldCheck,
  Package,
  Clock,
  RefreshCw,
  Download,
  CheckCircle2,
  AlertTriangle,
  Zap,
  BatteryCharging,
  PieChart as PieIcon,
  Layers
} from 'lucide-react';
import StatCard from '../components/StatCard';
import AlertBadge from '../components/AlertBadge';
import { api } from '../services/api';

export default function ReportsAnalytics() {
  const [reportData, setReportData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchReports = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.getReports();
      setReportData(data);
    } catch (err) {
      console.error('Failed to load analytical reports:', err);
      setError(err.message || 'Failed to communicate with Java Analytics Service');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchReports();
  }, []);

  const exportReportCsv = () => {
    if (!reportData) return;
    const { deliveryMetrics, droneMetrics, requestMetrics, resourceMetrics } = reportData;

    let csvContent = 'data:text/csv;charset=utf-8,';
    csvContent += 'RESCUEDRONE OPERATIONAL ANALYTICS REPORT\n';
    csvContent += `Generated At,${new Date().toLocaleString()}\n\n`;

    csvContent += 'DELIVERY & COST METRICS\n';
    csvContent += `Total Deliveries,${deliveryMetrics.totalDeliveries}\n`;
    csvContent += `Completed Deliveries,${deliveryMetrics.completedDeliveries}\n`;
    csvContent += `Cancelled Deliveries,${deliveryMetrics.cancelledDeliveries}\n`;
    csvContent += `Success Rate (%),${deliveryMetrics.successRatePercent}%\n`;
    csvContent += `Total Relief Cost (INR),INR ${deliveryMetrics.totalReliefCost}\n`;
    csvContent += `Average Cost per Mission (INR),INR ${deliveryMetrics.avgDeliveryCost}\n`;
    csvContent += `Total Distance Flown (km),${deliveryMetrics.totalDistanceKm} km\n`;
    csvContent += `Average Distance per Mission (km),${deliveryMetrics.avgDistanceKm} km\n\n`;

    csvContent += 'FLEET UTILIZATION\n';
    csvContent += `Total Drones,${droneMetrics.totalDrones}\n`;
    csvContent += `Available Drones,${droneMetrics.availableDrones}\n`;
    csvContent += `Busy / In-Flight Drones,${droneMetrics.busyDrones}\n`;
    csvContent += `Fleet Utilization Rate,${droneMetrics.fleetUtilizationPercent}%\n`;
    csvContent += `Average Battery Level,${droneMetrics.avgBatteryPercent}%\n\n`;

    csvContent += 'RESOURCE CONSUMPTION BREAKDOWN\n';
    csvContent += 'Resource ID,Name,Category,Available Stock,Reserved Stock,Delivered Stock,Unit\n';
    (resourceMetrics.resources || []).forEach((r) => {
      csvContent += `${r.id},"${r.name}",${r.category},${r.availableStock},${r.reservedStock},${r.deliveredStock},${r.unit}\n`;
    });

    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `RescueDrone_Analytics_Report_${Date.now()}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  if (loading && !reportData) {
    return (
      <div style={{ textAlign: 'center', padding: '60px 20px', color: '#64748b' }}>
        <RefreshCw size={36} className="spin" style={{ margin: '0 auto 16px', color: '#2563eb' }} />
        <h4 style={{ fontWeight: 600 }}>Computing Analytical Aggregations in Java...</h4>
        <p style={{ fontSize: '0.85rem' }}>Processing delivery logs, graph routes, drone telemetry, and inventory turnover.</p>
      </div>
    );
  }

  const {
    deliveryMetrics = {},
    droneMetrics = {},
    requestMetrics = {},
    resourceMetrics = {},
    disasterBreakdown = []
  } = reportData || {};

  // Donut chart calculations for Urgency Breakdown
  const urgencyData = requestMetrics.urgencyBreakdown || { CRITICAL: 0, HIGH: 0, MEDIUM: 0, LOW: 0 };
  const urgencyTotal = (urgencyData.CRITICAL || 0) + (urgencyData.HIGH || 0) + (urgencyData.MEDIUM || 0) + (urgencyData.LOW || 0);

  const urgencyColors = {
    CRITICAL: '#dc2626',
    HIGH: '#f97316',
    MEDIUM: '#2563eb',
    LOW: '#10b981'
  };

  // Helper to generate SVG Donut Chart Paths
  const renderDonutSlices = () => {
    if (urgencyTotal === 0) {
      return <circle cx="100" cy="100" r="70" fill="none" stroke="#e2e8f0" strokeWidth="28" />;
    }

    let cumulativeAngle = 0;
    const slices = [];
    const radius = 70;
    const cx = 100;
    const cy = 100;

    const entries = Object.entries(urgencyColors);
    entries.forEach(([key, color]) => {
      const count = urgencyData[key] || 0;
      if (count === 0) return;

      const sliceAngle = (count / urgencyTotal) * 360;
      const startAngle = cumulativeAngle;
      const endAngle = cumulativeAngle + sliceAngle;
      cumulativeAngle += sliceAngle;

      const startRad = (startAngle - 90) * (Math.PI / 180);
      const endRad = (endAngle - 90) * (Math.PI / 180);

      const x1 = cx + radius * Math.cos(startRad);
      const y1 = cy + radius * Math.sin(startRad);
      const x2 = cx + radius * Math.cos(endRad);
      const y2 = cy + radius * Math.sin(endRad);

      const largeArc = sliceAngle > 180 ? 1 : 0;
      const d = `M ${cx} ${cy} L ${x1} ${y1} A ${radius} ${radius} 0 ${largeArc} 1 ${x2} ${y2} Z`;

      slices.push(
        <path
          key={key}
          d={d}
          fill={color}
          stroke="#ffffff"
          strokeWidth="2"
        />
      );
    });

    return (
      <g>
        {slices}
        <circle cx="100" cy="100" r="42" fill="#ffffff" />
      </g>
    );
  };

  return (
    <div>
      {/* Top Banner and Actions */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px', flexWrap: 'wrap', gap: '12px' }}>
        <div>
          <h3 style={{ fontSize: '1.25rem', fontWeight: 800, color: '#0f172a' }}>
            Reports &amp; Operational Analytics
          </h3>
          <p style={{ fontSize: '0.85rem', color: '#64748b' }}>
            Audited mission efficiency, fleet telemetry, inventory turnover, and cost calculations computed strictly in pure Java.
          </p>
        </div>
        <div style={{ display: 'flex', gap: '10px' }}>
          <button className="btn btn-secondary" onClick={fetchReports} disabled={loading}>
            <RefreshCw size={15} className={loading ? 'spin' : ''} />
            <span>Refresh Analytics</span>
          </button>
          <button className="btn btn-primary" onClick={exportReportCsv} disabled={!reportData}>
            <Download size={15} />
            <span>Export CSV Report</span>
          </button>
        </div>
      </div>

      {error && (
        <div style={{ backgroundColor: '#fee2e2', border: '1px solid #f87171', color: '#b91c1c', padding: '12px 16px', borderRadius: '8px', marginBottom: '20px', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <AlertTriangle size={18} />
          <span>{error}</span>
        </div>
      )}

      {/* 6 Executive KPI Stat Cards */}
      <div className="stat-grid">
        <StatCard
          title="Total Logistics Spend"
          value={`₹${(deliveryMetrics.totalReliefCost || 0).toLocaleString()}`}
          subtext={`Avg ₹${deliveryMetrics.avgDeliveryCost || 0} / completed mission`}
          icon={IndianRupee}
          color="#16a34a"
          bgLight="#dcfce7"
        />
        <StatCard
          title="Fleet Utilization"
          value={`${droneMetrics.fleetUtilizationPercent || 0}%`}
          subtext={`${droneMetrics.busyDrones || 0} of ${droneMetrics.totalDrones || 0} drones in-flight/assigned`}
          icon={TrendingUp}
          color="#2563eb"
          bgLight="#eff6ff"
        />
        <StatCard
          title="Mission Success Rate"
          value={`${deliveryMetrics.successRatePercent || 0}%`}
          subtext={`${deliveryMetrics.completedDeliveries || 0} delivered, ${deliveryMetrics.cancelledDeliveries || 0} cancelled`}
          icon={ShieldCheck}
          color="#059669"
          bgLight="#d1fae5"
        />
        <StatCard
          title="Avg Flight Distance"
          value={`${deliveryMetrics.avgDistanceKm || 0} km`}
          subtext={`Total ${deliveryMetrics.totalDistanceKm || 0} km flown`}
          icon={BarChart3}
          color="#7c3aed"
          bgLight="#f3e8ff"
        />
        <StatCard
          title="Resources Distributed"
          value={`${resourceMetrics.totalDistributedUnits || 0} units`}
          subtext={`Across ${resourceMetrics.totalResourceTypes || 0} inventory categories`}
          icon={Package}
          color="#ea580c"
          bgLight="#ffedd5"
        />
        <StatCard
          title="Fleet Battery Health"
          value={`${droneMetrics.avgBatteryPercent || 0}%`}
          subtext={`Average across all active aircraft`}
          icon={BatteryCharging}
          color="#0891b2"
          bgLight="#cffafe"
        />
      </div>

      {/* Grid: 2 Analytical Visual Charts */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(420px, 1fr))', gap: '24px', marginBottom: '24px' }}>
        
        {/* Chart 1: Relief Request Urgency Distribution (Donut Chart) */}
        <div className="card">
          <h4 className="card-title">
            <PieIcon size={18} color="#2563eb" />
            <span>Emergency Request Urgency Distribution</span>
          </h4>
          <p style={{ fontSize: '0.82rem', color: '#64748b', marginBottom: '16px' }}>
            Total requests processed: <strong>{urgencyTotal}</strong> across severity priority tiers.
          </p>

          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-around', flexWrap: 'wrap', gap: '20px' }}>
            <div style={{ width: '180px', height: '180px', position: 'relative' }}>
              <svg viewBox="0 0 200 200" width="100%" height="100%">
                {renderDonutSlices()}
              </svg>
              <div style={{
                position: 'absolute',
                top: '50%',
                left: '50%',
                transform: 'translate(-50%, -50%)',
                textAlign: 'center'
              }}>
                <div style={{ fontSize: '1.3rem', fontWeight: 800, color: '#0f172a' }}>{urgencyTotal}</div>
                <div style={{ fontSize: '0.68rem', color: '#64748b', textTransform: 'uppercase' }}>Requests</div>
              </div>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', minWidth: '160px' }}>
              {Object.entries(urgencyColors).map(([tier, color]) => {
                const count = urgencyData[tier] || 0;
                const pct = urgencyTotal > 0 ? Math.round((count / urgencyTotal) * 100) : 0;
                return (
                  <div key={tier} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '12px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <span style={{ width: '12px', height: '12px', borderRadius: '3px', backgroundColor: color }} />
                      <span style={{ fontSize: '0.82rem', fontWeight: 600, color: '#334155' }}>{tier}</span>
                    </div>
                    <div style={{ fontSize: '0.82rem', color: '#64748b' }}>
                      <strong>{count}</strong> ({pct}%)
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>

        {/* Chart 2: Disaster Logistics Spend & Delivery Volume (Bar Visualization) */}
        <div className="card">
          <h4 className="card-title">
            <BarChart3 size={18} color="#7c3aed" />
            <span>Disaster Expenditure &amp; Mission Delivery Volume</span>
          </h4>
          <p style={{ fontSize: '0.82rem', color: '#64748b', marginBottom: '16px' }}>
            Logistics expenditure and completed relief drops allocated by emergency event.
          </p>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            {disasterBreakdown.length > 0 ? (
              disasterBreakdown.map((dis) => {
                const maxCost = Math.max(...disasterBreakdown.map((d) => d.totalCost), 1);
                const costBarPct = Math.min(100, Math.round((dis.totalCost / maxCost) * 100));

                return (
                  <div key={dis.id} style={{ borderBottom: '1px solid #f1f5f9', paddingBottom: '12px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '4px' }}>
                      <span style={{ fontWeight: 700, fontSize: '0.88rem', color: '#0f172a' }}>
                        {dis.name}
                      </span>
                      <span style={{ fontWeight: 700, fontSize: '0.9rem', color: '#16a34a' }}>
                        ₹{dis.totalCost.toLocaleString()}
                      </span>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.76rem', color: '#64748b', marginBottom: '6px' }}>
                      <span>Region: {dis.region} | Severity: <strong>{dis.severity}</strong></span>
                      <span>Missions Completed: <strong>{dis.completedDeliveries}</strong></span>
                    </div>
                    <div style={{ width: '100%', height: '8px', backgroundColor: '#e2e8f0', borderRadius: '4px', overflow: 'hidden' }}>
                      <div
                        style={{
                          width: `${Math.max(5, costBarPct)}%`,
                          height: '100%',
                          backgroundColor: '#7c3aed',
                          borderRadius: '4px',
                          transition: 'width 0.4s ease'
                        }}
                      />
                    </div>
                  </div>
                );
              })
            ) : (
              <p style={{ fontSize: '0.85rem', color: '#94a3b8' }}>No disaster cost logs recorded yet.</p>
            )}
          </div>
        </div>
      </div>

      {/* Drone Fleet Telemetry & Battery Health Gauge Grid */}
      <div className="card" style={{ marginBottom: '24px' }}>
        <h4 className="card-title">
          <Zap size={18} color="#059669" />
          <span>Drone Fleet Telemetry, Battery Gauges &amp; Mission History</span>
        </h4>
        <p style={{ fontSize: '0.82rem', color: '#64748b', marginBottom: '16px' }}>
          Monitors individual aircraft readiness, onboard battery reserves, payload ratings, and location assignments.
        </p>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '16px' }}>
          {(droneMetrics.drones || []).map((drone) => {
            const isLowBat = drone.batteryPercentage < 25;
            const batColor = drone.batteryPercentage > 50 ? '#16a34a' : drone.batteryPercentage > 20 ? '#d97706' : '#dc2626';

            return (
              <div
                key={drone.id}
                style={{
                  border: '1px solid #e2e8f0',
                  borderRadius: '10px',
                  padding: '16px',
                  backgroundColor: '#ffffff',
                  boxShadow: '0 1px 3px rgba(0,0,0,0.04)'
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                  <span style={{ fontWeight: 700, fontSize: '0.92rem', color: '#0f172a' }}>
                    {drone.name}
                  </span>
                  <span
                    style={{
                      fontSize: '0.7rem',
                      fontWeight: 700,
                      padding: '2px 8px',
                      borderRadius: '12px',
                      backgroundColor: drone.status === 'AVAILABLE' ? '#dcfce7' : drone.status === 'IN_FLIGHT' ? '#ffedd5' : drone.status === 'ASSIGNED' ? '#eff6ff' : '#fee2e2',
                      color: drone.status === 'AVAILABLE' ? '#16a34a' : drone.status === 'IN_FLIGHT' ? '#ea580c' : drone.status === 'ASSIGNED' ? '#2563eb' : '#dc2626'
                    }}
                  >
                    {drone.status}
                  </span>
                </div>

                <div style={{ fontSize: '0.78rem', color: '#64748b', marginBottom: '12px' }}>
                  {drone.type} • Stationed: <strong>{drone.currentLocationName}</strong>
                </div>

                {/* Battery Meter */}
                <div style={{ marginBottom: '12px' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.78rem', marginBottom: '4px' }}>
                    <span style={{ color: '#475569', display: 'flex', alignItems: 'center', gap: '4px' }}>
                      <BatteryCharging size={14} color={batColor} /> Battery Reserve
                    </span>
                    <strong style={{ color: batColor }}>{drone.batteryPercentage}%</strong>
                  </div>
                  <div style={{ width: '100%', height: '8px', backgroundColor: '#e2e8f0', borderRadius: '4px', overflow: 'hidden' }}>
                    <div
                      style={{
                        width: `${drone.batteryPercentage}%`,
                        height: '100%',
                        backgroundColor: batColor,
                        borderRadius: '4px'
                      }}
                    />
                  </div>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px', fontSize: '0.75rem', color: '#475569', borderTop: '1px solid #f1f5f9', paddingTop: '8px' }}>
                  <div>Payload: <strong>{drone.capacityKg} kg</strong></div>
                  <div>Max Range: <strong>{drone.maxFlightRangeKm} km</strong></div>
                  <div style={{ gridColumn: 'span 2' }}>
                    Completed Missions: <strong style={{ color: '#2563eb' }}>{drone.completedMissions}</strong>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Resource Inventory Turnover & Delivery Fulfillment Table */}
      <div className="card">
        <h4 className="card-title">
          <Layers size={18} color="#2563eb" />
          <span>Relief Resource Inventory &amp; Distribution Turnover</span>
        </h4>
        <p style={{ fontSize: '0.82rem', color: '#64748b', marginBottom: '16px' }}>
          Real-time tracking of stockpiled relief goods, reserved mission payloads, and fulfilled humanitarian deliveries.
        </p>

        <div className="data-table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Resource Name</th>
                <th>Category</th>
                <th>Unit Weight</th>
                <th>Available</th>
                <th>Reserved</th>
                <th>Delivered</th>
                <th>Turnover Rate</th>
              </tr>
            </thead>
            <tbody>
              {(resourceMetrics.resources || []).map((res) => (
                <tr key={res.id}>
                  <td>
                    <strong>{res.name}</strong>
                    <div style={{ fontSize: '0.72rem', color: '#64748b' }}>{res.id}</div>
                  </td>
                  <td>
                    <span style={{ fontSize: '0.75rem', fontWeight: 600, padding: '2px 8px', borderRadius: '4px', backgroundColor: '#f1f5f9' }}>
                      {res.category}
                    </span>
                  </td>
                  <td>{res.unitWeightKg} kg / {res.unit}</td>
                  <td>
                    <span style={{ fontWeight: 700, color: res.availableStock > 0 ? '#16a34a' : '#dc2626' }}>
                      {res.availableStock} {res.unit}
                    </span>
                  </td>
                  <td>
                    <span style={{ fontWeight: 600, color: '#d97706' }}>
                      {res.reservedStock} {res.unit}
                    </span>
                  </td>
                  <td>
                    <span style={{ fontWeight: 700, color: '#2563eb' }}>
                      {res.deliveredStock} {res.unit}
                    </span>
                  </td>
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <div style={{ width: '80px', height: '6px', backgroundColor: '#e2e8f0', borderRadius: '3px', overflow: 'hidden' }}>
                        <div
                          style={{
                            width: `${Math.min(100, res.turnoverRatePercent)}%`,
                            height: '100%',
                            backgroundColor: '#2563eb',
                            borderRadius: '3px'
                          }}
                        />
                      </div>
                      <span style={{ fontSize: '0.76rem', fontWeight: 600 }}>{res.turnoverRatePercent}%</span>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
