import React from 'react';
import {
  LayoutDashboard,
  Flame,
  MapPin,
  HeartPulse,
  Navigation,
  GitFork,
  Truck,
  Boxes,
  BarChart3,
  History,
  Settings,
  Radio
} from 'lucide-react';

export const MENUS = [
  { id: 'dashboard', label: 'Dashboard', icon: LayoutDashboard, section: 'Core' },
  { id: 'disasters', label: 'Disaster Management', icon: Flame, section: 'Operations' },
  { id: 'locations', label: 'Location Management', icon: MapPin, section: 'Operations' },
  { id: 'requests', label: 'Relief Request Management', icon: HeartPulse, section: 'Operations' },
  { id: 'drones', label: 'Drone Management', icon: Navigation, section: 'Fleet & Logistics' },
  { id: 'routes', label: 'Route Optimization', icon: GitFork, section: 'Fleet & Logistics' },
  { id: 'deliveries', label: 'Delivery Management', icon: Truck, section: 'Fleet & Logistics' },
  { id: 'resources', label: 'Resource Management', icon: Boxes, section: 'Inventory' },
  { id: 'reports', label: 'Reports & Analytics', icon: BarChart3, section: 'Intelligence' },
  { id: 'history', label: 'Delivery History', icon: History, section: 'Intelligence' },
  { id: 'settings', label: 'Settings / System Info', icon: Settings, section: 'System' }
];

export default function Sidebar({ activeMenu, onSelectMenu, backendConnected }) {
  let currentSection = '';

  return (
    <aside className="sidebar">
      <div className="sidebar-header">
        <div className="logo-badge">
          <Radio size={22} />
        </div>
        <div className="sidebar-brand">
          <h1>RescueDrone</h1>
          <span>Smart Disaster Relief</span>
        </div>
      </div>

      <nav className="sidebar-nav">
        {MENUS.map((item) => {
          const showSection = item.section !== currentSection;
          if (showSection) currentSection = item.section;

          const IconComponent = item.icon;
          const isActive = activeMenu === item.id;

          return (
            <React.Fragment key={item.id}>
              {showSection && <div className="nav-section-title">{item.section}</div>}
              <button
                className={`nav-item ${isActive ? 'active' : ''}`}
                onClick={() => onSelectMenu(item.id)}
              >
                <IconComponent className="nav-item-icon" />
                <span>{item.label}</span>
              </button>
            </React.Fragment>
          );
        })}
      </nav>

      <div className="sidebar-footer">
        <div className="system-status-indicator">
          <span
            className="pulse-dot"
            style={{
              backgroundColor: backendConnected ? '#22c55e' : '#ef4444',
              boxShadow: backendConnected ? '0 0 8px #22c55e' : '0 0 8px #ef4444'
            }}
          />
          <span>
            {backendConnected ? 'Java Backend Online (8080)' : 'Connecting Backend...'}
          </span>
        </div>
      </div>
    </aside>
  );
}
