import type { Express } from "express";
import { createServer, type Server } from "http";
import { createProxyMiddleware } from 'http-proxy-middleware';
import { api } from "@shared/routes";

import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

export async function registerRoutes(
  httpServer: Server,
  app: Express
): Promise<Server> {
  
  // Proxy API requests to the backend server
  app.use('/api', createProxyMiddleware({
    target: process.env.BACKEND_URL || process.env.API_BASE_URL || 'http://18.212.249.8:8080',
    changeOrigin: true,
    secure: false, // Set to false if using self-signed certificates
    autoRewrite: true,
    protocolRewrite: 'http'
  }));

  // Additional fallback: if any /api request somehow reaches here without being proxied,
  // we log it to help with debugging
  app.get('/api/*', (req, res) => {
    console.log(`Unproxied API request reached server: ${req.path}`);
    res.status(500).json({ error: 'API proxy misconfiguration' });
  });

  return httpServer;
}