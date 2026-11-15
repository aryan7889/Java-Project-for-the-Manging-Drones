import React from 'react';

export default function AlertBadge({ level, text }) {
  const normalized = (level || text || '').toString().toLowerCase();
  
  let badgeClass = 'badge-low';
  if (normalized.includes('critical') || normalized.includes('danger')) {
    badgeClass = 'badge-critical';
  } else if (normalized.includes('high') || normalized.includes('warning')) {
    badgeClass = 'badge-high';
  } else if (normalized.includes('medium')) {
    badgeClass = 'badge-medium';
  }

  return (
    <span className={`badge ${badgeClass}`}>
      {text || level}
    </span>
  );
}
