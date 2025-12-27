#!/bin/bash
# Script to clear GraphHopper cache and force rebuild with Indonesia map

set -e

echo "Clearing GraphHopper cache..."

# Check if using Docker
if command -v docker &> /dev/null && docker ps -a --format '{{.Names}}' | grep -q "^graphhopper$"; then
    echo "Docker container detected. Stopping container..."
    docker-compose down
    
    echo "Removing graph-cache volume..."
    docker volume rm graphhopper_graph-cache 2>/dev/null || docker volume rm graph-cache 2>/dev/null || echo "Volume not found or already removed"
    
    echo "Cache cleared. Restart with: docker-compose up"
else
    # Local cache directory
    if [ -d "graph-cache" ]; then
        echo "Removing local graph-cache directory..."
        rm -rf graph-cache
        echo "Cache cleared."
    else
        echo "No graph-cache directory found."
    fi
    
    echo ""
    echo "To rebuild the graph with Indonesia map, run:"
    echo "  java -D\"dw.graphhopper.datareader.file=indonesia-latest.osm.pbf\" -jar web/target/graphhopper-web-*.jar server config.yml"
fi

echo ""
echo "Note: The graph will be rebuilt from indonesia-latest.osm.pbf on next startup."
echo "This may take some time depending on the size of the map file."

