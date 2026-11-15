import React from 'react';
import { Activity, ShieldCheck, RefreshCw } from 'lucide-react';

export default function Header({ title, subtitle, onRefresh, loading }) {
  return (
    <header className="top-header">
      <div className="header-title-area">
        <h2>{title}</h2>
        <p>{subtitle}</p>
      </div>
      <div className="header-actions">
        <button
          className="btn btn-secondary"
          onClick={onRefresh}
          disabled={loading}
          title="Refresh Data from Java Backend"
        >
          <RefreshCw size={15} className={loading ? 'spin' : ''} />
          <span>{loading ? 'Syncing...' : 'Sync Data'}</span>
        </button>
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.8rem', color: '#16a34a', fontWeight: '600' }}>
          <ShieldCheck size={18} />
          <span>Operational Ready</span>
        </div>
      </div>
    </header>
  );
}
