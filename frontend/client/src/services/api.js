/**
 * API Service for AI Secure Identity Verifier
 * Handles communication with the backend API
 */

const API_BASE_URL = 'http://localhost:8080/api';

/**
 * Check if the backend API is healthy
 * @returns {Promise<Object>} Health check response
 */
export const healthCheck = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/health`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      throw new Error(`Health check failed with status ${response.status}`);
    }

    return await response.json();
  } catch (error) {
    console.error('Health check error:', error);
    throw error;
  }
};

/**
 * Verify an identity document
 * @param {File} file - The document file to verify
 * @returns {Promise<Object>} Verification result
 */
export const verifyDocument = async (file) => {
  try {
    const formData = new FormData();
    formData.append('file', file);

    const response = await fetch(`${API_BASE_URL}/verify`, {
      method: 'POST',
      body: formData,
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ 
        message: `Server error: ${response.status}` 
      }));
      throw new Error(errorData.message || `Server error: ${response.status}`);
    }

    return await response.json();
  } catch (error) {
    console.error('Verification error:', error);
    throw error;
  }
};

/**
 * Get API base URL
 * @returns {string} The API base URL
 */
export const getApiBaseUrl = () => {
  return API_BASE_URL;
};