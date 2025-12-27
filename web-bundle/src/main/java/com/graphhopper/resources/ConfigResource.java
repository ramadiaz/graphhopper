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

import com.graphhopper.GraphHopperConfig;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Resource that serves the config.js file dynamically with values from config.yml
 */
@Path("/maps/config.js")
@Produces("application/javascript")
public class ConfigResource {

    private final GraphHopperConfig config;

    @Inject
    public ConfigResource(GraphHopperConfig config) {
        this.config = config;
    }

    @GET
    public Response getConfig() {
        // Get API keys from config.yml with defaults
        String graphhopperKey = config.getString("maps.keys.graphhopper", "");
        String maptilerKey = config.getString("maps.keys.maptiler", "missing_api_key");
        String omniscaleKey = config.getString("maps.keys.omniscale", "missing_api_key");
        String thunderforestKey = config.getString("maps.keys.thunderforest", "missing_api_key");
        String kurvigerKey = config.getString("maps.keys.kurviger", "missing_api_key");
        
        // Get other config values
        String routingApi = config.getString("maps.routingApi", null);
        String geocodingApi = config.getString("maps.geocodingApi", null);
        String defaultTiles = config.getString("maps.defaultTiles", null);
        boolean routingGraphLayerAllowed = config.getBool("maps.routingGraphLayerAllowed", true);
        
        // Use defaults if not specified
        if (routingApi == null || routingApi.isEmpty()) {
            routingApi = "location.origin + '/'";
        }
        if (geocodingApi == null || geocodingApi.isEmpty()) {
            geocodingApi = "''";
        }
        if (defaultTiles == null || defaultTiles.isEmpty()) {
            defaultTiles = "'OpenStreetMap'";
        }
        
        // Build the JavaScript config object
        String jsConfig = String.format(
            "const config = {\n" +
            "    routingApi: %s,\n" +
            "    geocodingApi: %s,\n" +
            "    defaultTiles: %s,\n" +
            "    keys: {\n" +
            "        graphhopper: \"%s\",\n" +
            "        maptiler: \"%s\",\n" +
            "        omniscale: \"%s\",\n" +
            "        thunderforest: \"%s\",\n" +
            "        kurviger: \"%s\"\n" +
            "    },\n" +
            "    routingGraphLayerAllowed: %s,\n" +
            "    request: {\n" +
            "        details: [\n" +
            "            'road_class',\n" +
            "            'road_environment',\n" +
            "            'max_speed',\n" +
            "            'average_speed',\n" +
            "        ],\n" +
            "        snapPreventions: ['ferry'],\n" +
            "    },\n" +
            "    profile_group_mapping: {},\n" +
            "}\n",
            routingApi,
            geocodingApi,
            defaultTiles,
            escapeJavaScript(graphhopperKey),
            escapeJavaScript(maptilerKey),
            escapeJavaScript(omniscaleKey),
            escapeJavaScript(thunderforestKey),
            escapeJavaScript(kurvigerKey),
            routingGraphLayerAllowed
        );

        return Response.ok(jsConfig)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .header("Content-Type", "application/javascript; charset=utf-8")
                .build();
    }

    private String escapeJavaScript(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}

