#!/bin/bash
# Run this after starting Piston to install language runtimes
CONTAINER=$(docker compose ps -q api)

echo "Installing Python..."
docker exec "$CONTAINER" piston ppman install python

echo "Installing JavaScript (Node.js)..."
docker exec "$CONTAINER" piston ppman install javascript

echo "Installing Java..."
docker exec "$CONTAINER" piston ppman install java

echo "Installing C..."
docker exec "$CONTAINER" piston ppman install c

echo "Installing C++..."
docker exec "$CONTAINER" piston ppman install "c++"

echo "Done. Available runtimes:"
docker exec "$CONTAINER" piston ppman list | grep "installed"
