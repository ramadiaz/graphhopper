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

package com.graphhopper.routing.util.countryrules.asia;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.RoadAccess;
import com.graphhopper.routing.ev.RoadClass;
import com.graphhopper.routing.util.TransportationMode;
import com.graphhopper.routing.util.countryrules.CountryRule;

/**
 * Defines the default rules for Indonesian roads
 * 
 * Indonesia uses left-hand traffic. This rule applies country-specific
 * access restrictions based on Indonesian road regulations.
 */
public class IndonesiaCountryRule implements CountryRule {

    @Override
    public RoadAccess getAccess(ReaderWay readerWay, TransportationMode transportationMode, RoadAccess currentRoadAccess) {
        // If access is already restricted, keep it
        if (currentRoadAccess != RoadAccess.YES)
            return currentRoadAccess;
        
        // Non-motorized vehicles (bicycles, pedestrians) are not restricted by this rule
        if (!transportationMode.isMotorVehicle())
            return RoadAccess.YES;
        
        RoadClass roadClass = RoadClass.find(readerWay.getTag("highway", ""));
        switch (roadClass) {
            case TRACK:
                // Tracks in Indonesia are often accessible but may require caution
                // Allow destination access for tracks
                return RoadAccess.DESTINATION;
            case PATH:
            case BRIDLEWAY:
            case CYCLEWAY:
            case FOOTWAY:
            case PEDESTRIAN:
                // Non-motorized paths are not accessible to motor vehicles
                return RoadAccess.NO;
            default:
                return RoadAccess.YES;
        }
    }

}

