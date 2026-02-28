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
    target: process.env.BACKEND_URL || 'http://localhost:8080',
    changeOrigin: true,
    secure: false, // Set to false if using self-signed certificates
    autoRewrite: true,
    protocolRewrite: 'http'
  }));



  return httpServer;
}