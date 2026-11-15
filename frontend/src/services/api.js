/**
 * RescueDrone API Service Layer
 * Interfaces directly with the Java backend REST API.
 */
const BASE_URL = 'http://localhost:8080/api';

async function request(endpoint, options = {}) {
  const config = {
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {})
    },
    ...options
  };

  try {
    const res = await fetch(`${BASE_URL}${endpoint}`, config);
    if (!res.ok) {
      let errMessage = `HTTP Error ${res.status}`;
      try {
        const errorJson = await res.json();
        if (errorJson.error) errMessage = errorJson.error;
      } catch (e) {
        // Response wasn't JSON
      }
      throw new Error(errMessage);
    }
    return await res.json();
  } catch (error) {
    console.error(`API Error [${endpoint}]:`, error.message);
    throw error;
  }
}

export const api = {
  // Health & Dashboard
  getHealth: () => request('/health'),
  getDashboardStats: () => request('/dashboard/stats'),

  // Disasters
  getDisasters: () => request('/disasters'),
  createDisaster: (data) => request('/disasters', { method: 'POST', body: JSON.stringify(data) }),
  updateDisaster: (id, data) => request(`/disasters/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  deleteDisaster: (id) => request(`/disasters/${id}`, { method: 'DELETE' }),

  // Locations
  getLocations: () => request('/locations'),
  createLocation: (data) => request('/locations', { method: 'POST', body: JSON.stringify(data) }),
  updateLocation: (id, data) => request(`/locations/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  deleteLocation: (id) => request(`/locations/${id}`, { method: 'DELETE' }),

  // Routes
  getRoutes: () => request('/routes'),
  createRoute: (data) => request('/routes', { method: 'POST', body: JSON.stringify(data) }),
  optimizeRoute: (sourceId, destId) => request('/routes/optimize', { 
    method: 'POST', 
    body: JSON.stringify({ sourceId, destId }) 
  }),

  // Drones
  getDrones: () => request('/drones'),
  createDrone: (data) => request('/drones', { method: 'POST', body: JSON.stringify(data) }),
  updateDroneStatus: (id, status) => request(`/drones/${id}`, { 
    method: 'PUT', 
    body: JSON.stringify({ status }) 
  }),

  // Resources
  getResources: () => request('/resources'),
  createResource: (data) => request('/resources', { method: 'POST', body: JSON.stringify(data) }),
  updateStock: (id, amount) => request(`/resources/${id}`, {
    method: 'PUT',
    body: JSON.stringify({ amount })
  }),

  // Requests
  getRequests: () => request('/requests'),
  getRequestQueue: () => request('/requests/queue'),
  createRequest: (data) => request('/requests', { method: 'POST', body: JSON.stringify(data) }),
  assignDrone: (requestId) => request('/requests/assign', { 
    method: 'POST', 
    body: JSON.stringify({ requestId }) 
  }),

  // Deliveries
  getDeliveries: () => request('/deliveries'),
  startDelivery: (id) => request('/deliveries/start', { method: 'POST', body: JSON.stringify({ deliveryId: id }) }),
  completeDelivery: (id) => request('/deliveries/complete', { method: 'POST', body: JSON.stringify({ deliveryId: id }) }),
  getHistory: () => request('/history'),

  // Reports & System
  getReports: () => request('/reports'),
  reseedData: () => request('/system/reseed', { method: 'POST' })
};
