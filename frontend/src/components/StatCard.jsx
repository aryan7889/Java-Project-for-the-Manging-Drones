import React from 'react';

export default function StatCard({ title, value, subtext, icon: Icon, color = '#2563eb', bgLight = '#eff6ff' }) {
  return (
    <div className="stat-card">
      <div className="stat-header">
        <span className="stat-title">{title}</span>
        <div className="stat-icon-wrapper" style={{ backgroundColor: bgLight, color: color }}>
          {Icon && <Icon size={20} />}
        </div>
      </div>
      <div className="stat-value">{value ?? '-'}</div>
      {subtext && <div className="stat-subtext">{subtext}</div>}
    </div>
  );
}
