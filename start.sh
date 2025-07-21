#!/bin/bash

# Arcoiris Chat Server Startup Script
# Default ports: 4000 for MCP, 4001 for web

MCP_PORT=${1:-4000}
WEB_PORT=${2:-4001}

echo "Starting Arcoiris chat server..."
echo "MCP server will be on port: $MCP_PORT"
echo "Web server will be on port: $WEB_PORT"
echo ""

# Check if Leiningen is installed
if ! command -v lein &> /dev/null; then
    echo "Error: Leiningen is not installed or not in PATH"
    echo "Please install Leiningen from https://leiningen.org/"
    exit 1
fi

# Install dependencies if needed
echo "Checking dependencies..."
lein deps

# Start the server
echo "Starting servers..."
lein run $MCP_PORT $WEB_PORT 