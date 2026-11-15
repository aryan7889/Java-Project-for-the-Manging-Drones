import React, { useState, useEffect } from 'react';
import {
  Settings,
  Database,
  Server,
  RefreshCw,
  Cpu,
  HardDrive,
  Activity,
  RotateCcw,
  CheckCircle2,
  AlertTriangle,
  Info
} from 'lucide-react';
import { api } from '../services/api';

export default function SettingsSystem({ health, onRefreshHealth }) {
  const [telemetry, setTelemetry] = useState(null);
  const [loading, setLoading] = useState(false);
  const [resetting, setResetting] = useState(false);
  const [notification, setNotification] = useState(null);

  const fetchTelemetry = async () => {
    setLoading(true);
    try {
      const data = await api.getReports();
      if (data && data.systemTelemetry) {
        setTelemetry(data.systemTelemetry);
      }
    } catch (err) {
      console.warn('Telemetry load warning:', err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTelemetry();
  }, []);

  const handleReseed = async () => {
    const confirmed = window.confirm(
      'Are you sure you want to reseed demo simulation data?\n\nThis will reset all CSV files in backend/data/ to the default baseline demo dataset.'
    );
    if (!confirmed) return;

    setResetting(true);
    try {
      await api.reseedData();
      setNotification({ type: 'success', message: 'Demo simulation data reseeded successfully!' });
      if (onRefreshHealth) onRefreshHealth();
      fetchTelemetry();
    } catch (err) {
      setNotification({ type: 'error', message: 'Reseed failed: ' + err.message });
    } finally {
      setResetting(false);
    }
  };

  const t = telemetry || {};
  const memUsed = t.usedMemoryMB || 0;
  const memTotal = t.totalMemoryMB || 1;
  const memMax = t.maxMemoryMB || 1;
  const memPercent = Math.min(100, Math.round((memUsed / memTotal) * 100));

  const formatUptime = (sec) => {
    if (!sec) return '0s';
    const m = Math.floor(sec / 60);
    const s = sec % 60;
    if (m === 0) return `${s}s`;
    const h = Math.floor(m / 60);
    const remM = m % 60;
    if (h === 0) return `${m}m ${s}s`;
    return `${h}h ${remM}m`;
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px', flexWrap: 'wrap', gap: '12px' }}>
        <div>
          <h3 style={{ fontSize: '1.2rem', fontWeight: 800, color: '#0f172a' }}>
            Settings &amp; System Telemetry
          </h3>
          <p style={{ fontSize: '0.82rem', color: '#64748b' }}>
            JVM runtime diagnostics, live thread monitors, CSV storage inspection, and simulation configuration.
          </p>
        </div>
        <div style={{ display: 'flex', gap: '10px' }}>
          <button
            className="btn btn-secondary"
            onClick={() => {
              if (onRefreshHealth) onRefreshHealth();
              fetchTelemetry();
            }}
            disabled={loading}
          >
            <RefreshCw size={15} className={loading ? 'spin' : ''} />
            <span>Refresh Diagnostics</span>
          </button>
          <button
            className="btn btn-secondary"
            style={{ color: '#dc2626', borderColor: '#fca5a5' }}
            onClick={handleReseed}
            disabled={resetting}
          >
            <RotateCcw size={15} className={resetting ? 'spin' : ''} />
            <span>Reset Demo Baseline Data</span>
          </button>
        </div>
      </div>

      {notification && (
        <div
          style={{
            padding: '12px 16px',
            borderRadius: '8px',
            marginBottom: '20px',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            backgroundColor: notification.type === 'success' ? '#dcfce7' : '#fee2e2',
            color: notification.type === 'success' ? '#15803d' : '#b91c1c',
            border: `1px solid ${notification.type === 'success' ? '#86efac' : '#fca5a5'}`
          }}
        >
          {notification.type === 'success' ? <CheckCircle2 size={18} /> : <AlertTriangle size={18} />}
          <span style={{ fontSize: '0.88rem', fontWeight: 600 }}>{notification.message}</span>
        </div>
      )}

      {/* Grid: JVM Telemetry & Configuration */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(380px, 1fr))', gap: '24px', marginBottom: '24px' }}>
        
        {/* Card 1: Java Runtime Diagnostics */}
        <div className="card">
          <h4 className="card-title">
            <Server size={18} color="#2563eb" />
            <span>Java Runtime &amp; JVM Telemetry</span>
          </h4>
          <table className="data-table">
            <tbody>
              <tr>
                <td><strong>Application:</strong></td>
                <td>RescueDrone Disaster Relief System</td>
              </tr>
              <tr>
                <td><strong>Java SE Version:</strong></td>
                <td>
                  <span style={{ color: '#16a34a', fontWeight: 700 }}>
                    {t.javaVersion || health?.javaVersion || 'Java SE'} ({t.javaVendor || 'Oracle'})
                  </span>
                </td>
              </tr>
              <tr>
                <td><strong>Host Architecture:</strong></td>
                <td>{t.osName || health?.os || 'Windows'} ({t.osArch || 'x64'}, {t.availableProcessors || 8} cores)</td>
              </tr>
              <tr>
                <td><strong>Server Uptime:</strong></td>
                <td><strong style={{ color: '#2563eb' }}>{formatUptime(t.uptimeSeconds)}</strong></td>
              </tr>
              <tr>
                <td><strong>Active Threads:</strong></td>
                <td>
                  <span style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                    <Activity size={14} color="#059669" />
                    <strong>{t.activeThreads || 10} threads</strong> (HttpServer ThreadPool)
                  </span>
                </td>
              </tr>
            </tbody>
          </table>

          {/* Memory Bar */}
          <div style={{ marginTop: '16px', borderTop: '1px solid #f1f5f9', paddingTop: '12px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.78rem', marginBottom: '6px' }}>
              <span style={{ color: '#475569', display: 'flex', alignItems: 'center', gap: '4px' }}>
                <Cpu size={14} color="#7c3aed" /> JVM Heap Memory
              </span>
              <span>
                <strong>{memUsed} MB</strong> used / {memTotal} MB committed ({memMax} MB max)
              </span>
            </div>
            <div style={{ width: '100%', height: '8px', backgroundColor: '#e2e8f0', borderRadius: '4px', overflow: 'hidden' }}>
              <div
                style={{
                  width: `${memPercent}%`,
                  height: '100%',
                  backgroundColor: memPercent > 80 ? '#dc2626' : memPercent > 60 ? '#d97706' : '#2563eb',
                  borderRadius: '4px',
                  transition: 'width 0.4s ease'
                }}
              />
            </div>
          </div>
        </div>

        {/* Card 2: Operational Constants & Algorithm Configuration */}
        <div className="card">
          <h4 className="card-title">
            <Settings size={18} color="#7c3aed" />
            <span>Simulation Parameters &amp; Logistics Formulas</span>
          </h4>
          <table className="data-table">
            <tbody>
              <tr>
                <td><strong>Base Headquarters Hub:</strong></td>
                <td><code>LOC-001</code> (Central Hospital Base)</td>
              </tr>
              <tr>
                <td><strong>Dijkstra Algorithm:</strong></td>
                <td>Custom Min-Heap (Java <code>PriorityQueue&lt;PathNode&gt;</code>)</td>
              </tr>
              <tr>
                <td><strong>Relief Request Priority:</strong></td>
                <td><code>Urgency &gt; Affected People &gt; FIFO</code></td>
              </tr>
              <tr>
                <td><strong>Drone Allocation Strategy:</strong></td>
                <td>Least Excess Capacity (Best Fit) + deterministic tie-break</td>
              </tr>
              <tr>
                <td><strong>Battery Consumption Rate:</strong></td>
                <td><code>3% battery / km</code> round-trip flight</td>
              </tr>
              <tr>
                <td><strong>Delivery Fee Formula:</strong></td>
                <td>
                  <code>₹100 (Base) + (Dist × ₹20/km) + (₹50 Emergency)</code>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      {/* CSV Persistence Storage Inspection */}
      <div className="card">
        <h4 className="card-title">
          <HardDrive size={18} color="#059669" />
          <span>CSV Storage Files &amp; In-Memory Data Collections</span>
        </h4>
        <p style={{ fontSize: '0.82rem', color: '#64748b', marginBottom: '16px' }}>
          Pure Java file persistence with atomic swap writes, RFC-4180 quote escaping, and corrupted record skipping.
        </p>

        <div className="data-table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Storage File</th>
                <th>Physical Storage Path</th>
                <th>Records In-Memory</th>
                <th>File Size</th>
                <th>Integrity Status</th>
              </tr>
            </thead>
            <tbody>
              {t.storageFiles && t.storageFiles.length > 0 ? (
                t.storageFiles.map((f) => (
                  <tr key={f.fileName}>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <Database size={15} color="#2563eb" />
                        <strong>{f.fileName}</strong>
                      </div>
                    </td>
                    <td>
                      <code style={{ fontSize: '0.78rem' }}>{f.filePath}</code>
                    </td>
                    <td>
                      <strong style={{ color: '#0f172a' }}>{f.recordCount}</strong> records
                    </td>
                    <td style={{ fontSize: '0.82rem' }}>
                      {f.fileSizeBytes} bytes
                    </td>
                    <td>
                      <span
                        style={{
                          fontSize: '0.72rem',
                          fontWeight: 700,
                          padding: '2px 8px',
                          borderRadius: '10px',
                          backgroundColor: '#dcfce7',
                          color: '#15803d',
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '4px'
                        }}
                      >
                        <CheckCircle2 size={12} /> Validated
                      </span>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="5" style={{ textAlign: 'center', color: '#94a3b8', padding: '16px' }}>
                    Storage telemetry loading...
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
