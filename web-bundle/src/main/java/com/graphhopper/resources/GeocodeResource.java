/*
 *  Licensed to GraphHopper GmbH under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for
 *  additional information regarding copyright ownership.
 *
 *  GraphHopper GmbH licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except in
 *  compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.graphhopper.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphhopper.GraphHopperConfig;
import com.graphhopper.api.model.GHGeocodingEntry;
import com.graphhopper.api.model.GHGeocodingResponse;
import com.graphhopper.jackson.Jackson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Geocode API endpoint for forward and reverse geocoding.
 * 
 * This implementation integrates with external geocoding services like Photon.
 * To use this endpoint, you need to:
 * 1. Configure the geocoding service URL in config.yml: geocoding.service.url
 * 2. Register this resource in GraphHopperBundle.java
 * 
 * Example usage:
 * - Forward geocoding: GET /geocode?q=Berlin&limit=5
 * - Reverse geocoding: GET /geocode?reverse=true&point=52.5200,13.4050&limit=5
 * 
 * @author Generated for GraphHopper
 */
@Path("geocode")
@Produces(MediaType.APPLICATION_JSON)
public class GeocodeResource {

    private static final Logger logger = LoggerFactory.getLogger(GeocodeResource.class);
    
    private final GraphHopperConfig config;
    private final HttpClient httpClient;
    private final String geocodingServiceUrl;
    private final ObjectMapper objectMapper;

    @Inject
    public GeocodeResource(GraphHopperConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = Jackson.newObjectMapper();
        // Default to Photon if not configured
        this.geocodingServiceUrl = config.getString("geocoding.service.url", 
            "https://photon.komoot.io/api");
    }

    /**
     * Forward geocoding: Convert address/query string to coordinates
     * 
     * @param q Query string (address or place name)
     * @param reverse If true, perform reverse geocoding
     * @param point Point coordinates for reverse geocoding (format: lat,lon)
     * @param limit Maximum number of results (default: 10)
     * @param locale Locale for results (default: en)
     * @return JSON response with geocoding results
     */
    @GET
    public Response geocode(
            @QueryParam("q") String q,
            @QueryParam("reverse") @DefaultValue("false") boolean reverse,
            @QueryParam("point") String point,
            @QueryParam("limit") @DefaultValue("10") int limit,
            @QueryParam("locale") @DefaultValue("en") String locale) {
        
        try {
            if (reverse) {
                return reverseGeocode(point, limit, locale);
            } else {
                return forwardGeocode(q, limit, locale);
            }
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid geocoding request", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        } catch (Exception e) {
            logger.error("Geocoding error", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Internal server error: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    private Response forwardGeocode(String q, int limit, String locale) throws IOException, InterruptedException {
        if (q == null || q.isEmpty()) {
            throw new IllegalArgumentException("Query parameter 'q' is required for forward geocoding");
        }

        // Build request URL for Photon API
        String url = geocodingServiceUrl + "/?q=" + URLEncoder.encode(q, StandardCharsets.UTF_8) 
                + "&limit=" + Math.min(limit, 50)  // Cap at 50
                + "&lang=" + locale;

        logger.debug("Forward geocoding request: {}", url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "GraphHopper/1.0")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            logger.warn("Geocoding service returned error: {} for URL: {}", response.statusCode(), url);
            return Response.status(Response.Status.BAD_GATEWAY)
                    .entity("{\"error\": \"Geocoding service returned error: " + response.statusCode() + "\"}")
                    .build();
        }

        GHGeocodingResponse geocodeResponse = parsePhotonResponse(response.body(), limit, locale);
        
        return Response.ok(geocodeResponse).build();
    }

    private Response reverseGeocode(String point, int limit, String locale) throws IOException, InterruptedException {
        if (point == null || point.isEmpty()) {
            throw new IllegalArgumentException("Point parameter is required for reverse geocoding");
        }

        // Parse point (format: lat,lon)
        String[] coords = point.split(",");
        if (coords.length != 2) {
            throw new IllegalArgumentException("Invalid point format. Expected: lat,lon");
        }

        double lat, lon;
        try {
            lat = Double.parseDouble(coords[0].trim());
            lon = Double.parseDouble(coords[1].trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid coordinates: " + point);
        }

        // Validate coordinates
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            throw new IllegalArgumentException("Coordinates out of range. Lat: [-90, 90], Lon: [-180, 180]");
        }

        // Build request URL for Photon reverse geocoding
        // Photon reverse endpoint is at /reverse (not /api/reverse)
        // So we need to remove /api from the base URL if present
        String reverseBaseUrl = geocodingServiceUrl;
        if (reverseBaseUrl.endsWith("/api")) {
            reverseBaseUrl = reverseBaseUrl.substring(0, reverseBaseUrl.length() - 4);
        } else if (reverseBaseUrl.endsWith("/api/")) {
            reverseBaseUrl = reverseBaseUrl.substring(0, reverseBaseUrl.length() - 5);
        }
        
        String url = reverseBaseUrl + "/reverse?lat=" + lat + "&lon=" + lon 
                + "&limit=" + Math.min(limit, 50)  // Cap at 50
                + "&lang=" + locale;

        logger.debug("Reverse geocoding request: {}", url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "GraphHopper/1.0")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            logger.warn("Geocoding service returned error: {} for URL: {}", response.statusCode(), url);
            return Response.status(Response.Status.BAD_GATEWAY)
                    .entity("{\"error\": \"Geocoding service returned error: " + response.statusCode() + "\"}")
                    .build();
        }

        GHGeocodingResponse geocodeResponse = parsePhotonResponse(response.body(), limit, locale);
        
        return Response.ok(geocodeResponse).build();
    }

    /**
     * Parse Photon API response and convert to GraphHopper geocoding format
     * 
     * Photon API response format:
     * {
     *   "features": [
     *     {
     *       "geometry": {"coordinates": [lon, lat]},
     *       "properties": {
     *         "name": "...",
     *         "country": "...",
     *         "city": "...",
     *         "state": "...",
     *         "street": "...",
     *         "housenumber": "...",
     *         "postcode": "...",
     *         "osm_id": ...,
     *         "osm_type": "...",
     *         "osm_key": "...",
     *         "osm_value": "...",
     *         "extent": [minLon, maxLat, maxLon, minLat]
     *       }
     *     }
     *   ]
     * }
     */
    private GHGeocodingResponse parsePhotonResponse(String jsonResponse, int limit, String locale) throws IOException {
        GHGeocodingResponse response = new GHGeocodingResponse();
        response.setLocale(locale);
        response.addCopyright("OpenStreetMap");
        response.addCopyright("Photon");
        
        JsonNode root = objectMapper.readTree(jsonResponse);
        JsonNode features = root.get("features");
        
        if (features == null || !features.isArray()) {
            logger.warn("Invalid Photon response format: missing features array");
            return response;
        }

        int count = 0;
        for (JsonNode feature : features) {
            if (count >= limit) break;
            
            JsonNode geometry = feature.get("geometry");
            JsonNode properties = feature.get("properties");
            
            if (geometry == null || properties == null) continue;
            
            JsonNode coordinates = geometry.get("coordinates");
            if (coordinates == null || !coordinates.isArray() || coordinates.size() < 2) continue;
            
            double lon = coordinates.get(0).asDouble();
            double lat = coordinates.get(1).asDouble();
            
            GHGeocodingEntry entry = new GHGeocodingEntry();
            
            // Set point
            GHGeocodingEntry.Point point = new GHGeocodingEntry.Point(lat, lon);
            entry.setPoint(point);
            
            // Set properties
            entry.setName(getString(properties, "name"));
            entry.setCountry(getString(properties, "country"));
            entry.setCity(getString(properties, "city"));
            entry.setState(getString(properties, "state"));
            entry.setStreet(getString(properties, "street"));
            entry.setHouseNumber(getString(properties, "housenumber"));
            entry.setPostcode(getString(properties, "postcode"));
            
            // Set OSM properties
            JsonNode osmId = properties.get("osm_id");
            if (osmId != null && !osmId.isNull()) {
                entry.setOsmId(osmId.asLong());
            }
            entry.setOsmType(getString(properties, "osm_type"));
            entry.setOsmKey(getString(properties, "osm_key"));
            entry.setOsmValue(getString(properties, "osm_value"));
            
            // Set extent if available (Photon format: [minLon, maxLat, maxLon, minLat])
            JsonNode extent = properties.get("extent");
            if (extent != null && extent.isArray() && extent.size() == 4) {
                Double[] extentArray = new Double[4];
                for (int i = 0; i < 4; i++) {
                    extentArray[i] = extent.get(i).asDouble();
                }
                entry.setExtent(extentArray);
            }
            
            response.add(entry);
            count++;
        }
        
        return response;
    }

    private String getString(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull()) {
            return null;
        }
        return field.asText();
    }
}

