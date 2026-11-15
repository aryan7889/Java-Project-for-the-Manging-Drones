import React, { useState, useEffect } from 'react';
import Sidebar, { MENUS } from './components/Sidebar';
import Header from './components/Header';
import Dashboard from './pages/Dashboard';
import DisasterManagement from './pages/DisasterManagement';
import LocationManagement from './pages/LocationManagement';
import ReliefRequests from './pages/ReliefRequests';
import DroneManagement from './pages/DroneManagement';
import RouteOptimization from './pages/RouteOptimization';
import DeliveryManagement from './pages/DeliveryManagement';
import ResourceManagement from './pages/ResourceManagement';
import ReportsAnalytics from './pages/ReportsAnalytics';
import DeliveryHistory from './pages/DeliveryHistory';
import SettingsSystem from './pages/SettingsSystem';
import { api } from './services/api';
import './App.css';

export default function App() {
  const [activeMenu, setActiveMenu] = useState('dashboard');
  const [backendConnected, setBackendConnected] = useState(false);
  const [loading, setLoading] = useState(false);
  const [stats, setStats] = useState(null);
  const [health, setHealth] = useState(null);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [healthData, statsData] = await Promise.all([
        api.getHealth(),
        api.getDashboardStats()
      ]);
      setHealth(healthData);
      setStats(statsData);
      setBackendConnected(true);
    } catch (err) {
      console.warn('Backend sync warning:', err.message);
      setBackendConnected(false);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
    const interval = setInterval(fetchData, 15000); // Poll health every 15s
    return () => clearInterval(interval);
  }, []);

  const currentMenuObj = MENUS.find((m) => m.id === activeMenu) || MENUS[0];

  const renderActivePage = () => {
    switch (activeMenu) {
      case 'dashboard':
        return <Dashboard stats={stats} onNavigate={setActiveMenu} />;
      case 'disasters':
        return <DisasterManagement />;
      case 'locations':
        return <LocationManagement />;
      case 'requests':
        return <ReliefRequests />;
      case 'drones':
        return <DroneManagement />;
      case 'routes':
        return <RouteOptimization />;
      case 'deliveries':
        return <DeliveryManagement />;
      case 'resources':
        return <ResourceManagement />;
      case 'reports':
        return <ReportsAnalytics />;
      case 'history':
        return <DeliveryHistory />;
      case 'settings':
        return <SettingsSystem health={health} onRefreshHealth={fetchData} />;
      default:
        return <Dashboard stats={stats} onNavigate={setActiveMenu} />;
    }
  };

  return (
    <div className="app-container">
      <Sidebar
        activeMenu={activeMenu}
        onSelectMenu={setActiveMenu}
        backendConnected={backendConnected}
      />
      <div className="main-wrapper">
        <Header
          title={currentMenuObj.label}
          subtitle={`RescueDrone Operational Command System – ${currentMenuObj.section}`}
          onRefresh={fetchData}
          loading={loading}
        />
        <main className="content-scrollable">
          {renderActivePage()}
        </main>
      </div>
    </div>
  );
}
