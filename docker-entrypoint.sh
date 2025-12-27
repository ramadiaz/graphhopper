#!/bin/bash
set -e

OSM_FILE="${OSM_FILE:-/app/berlin-latest.osm.pbf}"

# Check if OSM file exists and is a file (not a directory)
if [ ! -f "$OSM_FILE" ]; then
    if [ -d "$OSM_FILE" ]; then
        echo "ERROR: $OSM_FILE is a directory, not a file!"
        echo ""
        echo "This usually happens when the OSM file doesn't exist on the host machine."
        echo "Please download the OSM file first:"
        echo "  wget https://download.geofabrik.de/europe/germany/berlin-latest.osm.pbf"
        echo ""
        echo "Or if using docker-compose, make sure the file exists before running:"
        echo "  docker-compose up"
        exit 1
    else
        echo "ERROR: OSM file not found: $OSM_FILE"
        echo "Please ensure the file exists and is mounted correctly."
        exit 1
    fi
fi

echo "Starting GraphHopper with OSM file: $OSM_FILE"
exec java $JAVA_OPTS -D"dw.graphhopper.datareader.file=${OSM_FILE}" -jar graphhopper.jar server config.yml

