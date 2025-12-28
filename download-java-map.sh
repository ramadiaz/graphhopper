#!/bin/bash
# Script to download Indonesian OSM map data for GraphHopper
# Usage: ./download-java-map.sh

set -e

MAP_URL="https://download.geofabrik.de/asia/java-latest.osm.pbf"
MAP_FILE="java-latest.osm.pbf"

echo "Downloading Indonesian OSM map data..."
echo "URL: $MAP_URL"
echo "Output file: $MAP_FILE"
echo ""
echo "Note: This file is large (~1-2 GB). Download may take some time depending on your internet connection."
echo ""

# Check if file already exists
if [ -f "$MAP_FILE" ]; then
    echo "File $MAP_FILE already exists. Skipping download."
    echo "To re-download, please delete the file first: rm $MAP_FILE"
    exit 0
fi

# Download the file
wget -S -nv -O "$MAP_FILE" "$MAP_URL"

if [ -f "$MAP_FILE" ]; then
    echo ""
    echo "✓ Download complete!"
    echo "File: $MAP_FILE"
    echo "Size: $(du -h $MAP_FILE | cut -f1)"
    echo ""
    echo "You can now start GraphHopper with:"
    echo "  docker-compose up"
    echo ""
    echo "Or run directly:"
    echo "  java -D\"dw.graphhopper.datareader.file=$MAP_FILE\" -jar web/target/graphhopper-web-*.jar server config.yml"
else
    echo "ERROR: Download failed!"
    exit 1
fi

