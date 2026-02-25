#!/bin/bash

echo "Starting AI Secure Identity Verifier - Full Stack Application..."
echo

# Function to start backend
start_backend() {
    cd backend
    echo "Starting backend server..."
    mvn spring-boot:run
}

# Function to start frontend
start_frontend() {
    cd frontend/client
    echo "Installing frontend dependencies and starting frontend server..."
    npm install
    npm run dev
}

# Start backend in background
echo "Starting backend server in background..."
start_backend &
BACKEND_PID=$!

# Wait for a few seconds to let backend start
sleep 5

# Start frontend in foreground
echo "Starting frontend server..."
start_frontend

# Cleanup function to kill background processes on exit
cleanup() {
    echo "Shutting down servers..."
    kill $BACKEND_PID 2>/dev/null
    exit 0
}

trap cleanup EXIT INT TERM

# Wait for backend process
wait $BACKEND_PID