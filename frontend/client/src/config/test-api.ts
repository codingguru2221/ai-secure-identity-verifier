// API Configuration Test
// This file helps verify that API configuration is working correctly

import { API_BASE_URL, API_ENDPOINTS, getEnvironmentInfo } from './api';

console.log('=== API Configuration Test ===');
console.log('API Base URL:', API_BASE_URL);
console.log('API Endpoints:', API_ENDPOINTS);
console.log('Environment Info:', getEnvironmentInfo());

// Test that all endpoints are properly formed
Object.entries(API_ENDPOINTS).forEach(([name, url]) => {
  console.log(`${name}: ${url}`);
  // Verify URL structure
  if (typeof url === 'string' && !url.startsWith('http://18.212.249.8:8080/api/')) {
    console.error(`❌ Invalid URL structure for ${name}: ${url}`);
  } else {
    console.log(`✅ Valid URL for ${name}`);
  }
});

export {};