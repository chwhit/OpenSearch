/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.index.search.geo;

import org.apache.lucene.spatial.prefix.tree.Cell;
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree;
import org.apache.lucene.spatial.prefix.tree.QuadPrefixTree;
import org.opensearch.OpenSearchParseException;
import org.opensearch.common.geo.GeoPoint;
import org.opensearch.common.geo.GeoUtils;
import org.opensearch.common.xcontent.XContentBuilder;
import org.opensearch.common.xcontent.XContentParser;
import org.opensearch.common.xcontent.XContentParser.Token;
import org.opensearch.geometry.utils.Geohash;
import org.opensearch.test.OpenSearchTestCase;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.distance.DistanceUtils;

import java.io.IOException;

import static org.opensearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;

public class GeoUtilsTests extends OpenSearchTestCase {
    private static final char[] BASE_32 = {'0', '1', '2', '3', '4', '5', '6',
        '7', '8', '9', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'j', 'k', 'm', 'n',
        'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};
    private static final double MAX_ACCEPTABLE_ERROR = 0.000000001;

    public void testGeohashCellWidth() {
        double equatorialDistance = 2 * Math.PI * 6378137.0;
        assertThat(GeoUtils.geoHashCellWidth(0), equalTo(equatorialDistance));
        assertThat(GeoUtils.geoHashCellWidth(1), equalTo(equatorialDistance / 8));
        assertThat(GeoUtils.geoHashCellWidth(2), equalTo(equatorialDistance / 32));
        assertThat(GeoUtils.geoHashCellWidth(3), equalTo(equatorialDistance / 256));
        assertThat(GeoUtils.geoHashCellWidth(4), equalTo(equatorialDistance / 1024));
        assertThat(GeoUtils.geoHashCellWidth(5), equalTo(equatorialDistance / 8192));
        assertThat(GeoUtils.geoHashCellWidth(6), equalTo(equatorialDistance / 32768));
        assertThat(GeoUtils.geoHashCellWidth(7), equalTo(equatorialDistance / 262144));
        assertThat(GeoUtils.geoHashCellWidth(8), equalTo(equatorialDistance / 1048576));
        assertThat(GeoUtils.geoHashCellWidth(9), equalTo(equatorialDistance / 8388608));
        assertThat(GeoUtils.geoHashCellWidth(10), equalTo(equatorialDistance / 33554432));
        assertThat(GeoUtils.geoHashCellWidth(11), equalTo(equatorialDistance / 268435456));
        assertThat(GeoUtils.geoHashCellWidth(12), equalTo(equatorialDistance / 1073741824));
    }

    public void testGeohashCellHeight() {
        double polarDistance = Math.PI * 6356752.314245;
        assertThat(GeoUtils.geoHashCellHeight(0), equalTo(polarDistance));
        assertThat(GeoUtils.geoHashCellHeight(1), equalTo(polarDistance / 4));
        assertThat(GeoUtils.geoHashCellHeight(2), equalTo(polarDistance / 32));
        assertThat(GeoUtils.geoHashCellHeight(3), equalTo(polarDistance / 128));
        assertThat(GeoUtils.geoHashCellHeight(4), equalTo(polarDistance / 1024));
        assertThat(GeoUtils.geoHashCellHeight(5), equalTo(polarDistance / 4096));
        assertThat(GeoUtils.geoHashCellHeight(6), equalTo(polarDistance / 32768));
        assertThat(GeoUtils.geoHashCellHeight(7), equalTo(polarDistance / 131072));
        assertThat(GeoUtils.geoHashCellHeight(8), equalTo(polarDistance / 1048576));
        assertThat(GeoUtils.geoHashCellHeight(9), equalTo(polarDistance / 4194304));
        assertThat(GeoUtils.geoHashCellHeight(10), equalTo(polarDistance / 33554432));
        assertThat(GeoUtils.geoHashCellHeight(11), equalTo(polarDistance / 134217728));
        assertThat(GeoUtils.geoHashCellHeight(12), equalTo(polarDistance / 1073741824));
    }

    public void testGeohashCellSize() {
        double equatorialDistance = 2 * Math.PI * 6378137.0;
        double polarDistance = Math.PI * 6356752.314245;
        assertThat(GeoUtils.geoHashCellSize(0), equalTo(Math.sqrt(Math.pow(polarDistance, 2) + Math.pow(equatorialDistance, 2))));
        assertThat(GeoUtils.geoHashCellSize(1),
            equalTo(Math.sqrt(Math.pow(polarDistance / 4, 2) + Math.pow(equatorialDistance / 8, 2))));
        assertThat(GeoUtils.geoHashCellSize(2),
            equalTo(Math.sqrt(Math.pow(polarDistance / 32, 2) + Math.pow(equatorialDistance / 32, 2))));
        assertThat(GeoUtils.geoHashCellSize(3),
                equalTo(Math.sqrt(Math.pow(polarDistance / 128, 2) + Math.pow(equatorialDistance / 256, 2))));
        assertThat(GeoUtils.geoHashCellSize(4),
                equalTo(Math.sqrt(Math.pow(polarDistance / 1024, 2) + Math.pow(equatorialDistance / 1024, 2))));
        assertThat(GeoUtils.geoHashCellSize(5),
                equalTo(Math.sqrt(Math.pow(polarDistance / 4096, 2) + Math.pow(equatorialDistance / 8192, 2))));
        assertThat(GeoUtils.geoHashCellSize(6),
                equalTo(Math.sqrt(Math.pow(polarDistance / 32768, 2) + Math.pow(equatorialDistance / 32768, 2))));
        assertThat(GeoUtils.geoHashCellSize(7),
                equalTo(Math.sqrt(Math.pow(polarDistance / 131072, 2) + Math.pow(equatorialDistance / 262144, 2))));
        assertThat(GeoUtils.geoHashCellSize(8),
                equalTo(Math.sqrt(Math.pow(polarDistance / 1048576, 2) + Math.pow(equatorialDistance / 1048576, 2))));
        assertThat(GeoUtils.geoHashCellSize(9),
                equalTo(Math.sqrt(Math.pow(polarDistance / 4194304, 2) + Math.pow(equatorialDistance / 8388608, 2))));
        assertThat(GeoUtils.geoHashCellSize(10),
                equalTo(Math.sqrt(Math.pow(polarDistance / 33554432, 2) + Math.pow(equatorialDistance / 33554432, 2))));
        assertThat(GeoUtils.geoHashCellSize(11),
                equalTo(Math.sqrt(Math.pow(polarDistance / 134217728, 2) + Math.pow(equatorialDistance / 268435456, 2))));
        assertThat(GeoUtils.geoHashCellSize(12),
                equalTo(Math.sqrt(Math.pow(polarDistance / 1073741824, 2) + Math.pow(equatorialDistance / 1073741824, 2))));
    }

    public void testGeoHashLevelsForPrecision() {
        for (int i = 0; i < 100; i++) {
            double precision = randomDouble() * 100;
            int level = GeoUtils.geoHashLevelsForPrecision(precision);
            assertThat(GeoUtils.geoHashCellSize(level), lessThanOrEqualTo(precision));
        }
    }

    public void testGeoHashLevelsForPrecision_String() {
        for (int i = 0; i < 100; i++) {
            double precision = randomDouble() * 100;
            String precisionString = precision + "m";
            int level = GeoUtils.geoHashLevelsForPrecision(precisionString);
            assertThat(GeoUtils.geoHashCellSize(level), lessThanOrEqualTo(precision));
        }
    }

    public void testQuadTreeCellWidth() {
        double equatorialDistance = 2 * Math.PI * 6378137.0;
        assertThat(GeoUtils.quadTreeCellWidth(0), equalTo(equatorialDistance));
        assertThat(GeoUtils.quadTreeCellWidth(1), equalTo(equatorialDistance / 2));
        assertThat(GeoUtils.quadTreeCellWidth(2), equalTo(equatorialDistance / 4));
        assertThat(GeoUtils.quadTreeCellWidth(3), equalTo(equatorialDistance / 8));
        assertThat(GeoUtils.quadTreeCellWidth(4), equalTo(equatorialDistance / 16));
        assertThat(GeoUtils.quadTreeCellWidth(5), equalTo(equatorialDistance / 32));
        assertThat(GeoUtils.quadTreeCellWidth(6), equalTo(equatorialDistance / 64));
        assertThat(GeoUtils.quadTreeCellWidth(7), equalTo(equatorialDistance / 128));
        assertThat(GeoUtils.quadTreeCellWidth(8), equalTo(equatorialDistance / 256));
        assertThat(GeoUtils.quadTreeCellWidth(9), equalTo(equatorialDistance / 512));
        assertThat(GeoUtils.quadTreeCellWidth(10), equalTo(equatorialDistance / 1024));
        assertThat(GeoUtils.quadTreeCellWidth(11), equalTo(equatorialDistance / 2048));
        assertThat(GeoUtils.quadTreeCellWidth(12), equalTo(equatorialDistance / 4096));
    }

    public void testQuadTreeCellHeight() {
        double polarDistance = Math.PI * 6356752.314245;
        assertThat(GeoUtils.quadTreeCellHeight(0), equalTo(polarDistance));
        assertThat(GeoUtils.quadTreeCellHeight(1), equalTo(polarDistance / 2));
        assertThat(GeoUtils.quadTreeCellHeight(2), equalTo(polarDistance / 4));
        assertThat(GeoUtils.quadTreeCellHeight(3), equalTo(polarDistance / 8));
        assertThat(GeoUtils.quadTreeCellHeight(4), equalTo(polarDistance / 16));
        assertThat(GeoUtils.quadTreeCellHeight(5), equalTo(polarDistance / 32));
        assertThat(GeoUtils.quadTreeCellHeight(6), equalTo(polarDistance / 64));
        assertThat(GeoUtils.quadTreeCellHeight(7), equalTo(polarDistance / 128));
        assertThat(GeoUtils.quadTreeCellHeight(8), equalTo(polarDistance / 256));
        assertThat(GeoUtils.quadTreeCellHeight(9), equalTo(polarDistance / 512));
        assertThat(GeoUtils.quadTreeCellHeight(10), equalTo(polarDistance / 1024));
        assertThat(GeoUtils.quadTreeCellHeight(11), equalTo(polarDistance / 2048));
        assertThat(GeoUtils.quadTreeCellHeight(12), equalTo(polarDistance / 4096));
    }

    public void testQuadTreeCellSize() {
        double equatorialDistance = 2 * Math.PI * 6378137.0;
        double polarDistance = Math.PI * 6356752.314245;
        assertThat(GeoUtils.quadTreeCellSize(0),
            equalTo(Math.sqrt(Math.pow(polarDistance, 2) + Math.pow(equatorialDistance, 2))));
        assertThat(GeoUtils.quadTreeCellSize(1),
            equalTo(Math.sqrt(Math.pow(polarDistance / 2, 2) + Math.pow(equatorialDistance / 2, 2))));
        assertThat(GeoUtils.quadTreeCellSize(2),
            equalTo(Math.sqrt(Math.pow(polarDistance / 4, 2) + Math.pow(equatorialDistance / 4, 2))));
        assertThat(GeoUtils.quadTreeCellSize(3),
            equalTo(Math.sqrt(Math.pow(polarDistance / 8, 2) + Math.pow(equatorialDistance / 8, 2))));
        assertThat(GeoUtils.quadTreeCellSize(4),
            equalTo(Math.sqrt(Math.pow(polarDistance / 16, 2) + Math.pow(equatorialDistance / 16, 2))));
        assertThat(GeoUtils.quadTreeCellSize(5),
            equalTo(Math.sqrt(Math.pow(polarDistance / 32, 2) + Math.pow(equatorialDistance / 32, 2))));
        assertThat(GeoUtils.quadTreeCellSize(6),
            equalTo(Math.sqrt(Math.pow(polarDistance / 64, 2) + Math.pow(equatorialDistance / 64, 2))));
        assertThat(GeoUtils.quadTreeCellSize(7),
                equalTo(Math.sqrt(Math.pow(polarDistance / 128, 2) + Math.pow(equatorialDistance / 128, 2))));
        assertThat(GeoUtils.quadTreeCellSize(8),
                equalTo(Math.sqrt(Math.pow(polarDistance / 256, 2) + Math.pow(equatorialDistance / 256, 2))));
        assertThat(GeoUtils.quadTreeCellSize(9),
                equalTo(Math.sqrt(Math.pow(polarDistance / 512, 2) + Math.pow(equatorialDistance / 512, 2))));
        assertThat(GeoUtils.quadTreeCellSize(10),
                equalTo(Math.sqrt(Math.pow(polarDistance / 1024, 2) + Math.pow(equatorialDistance / 1024, 2))));
        assertThat(GeoUtils.quadTreeCellSize(11),
                equalTo(Math.sqrt(Math.pow(polarDistance / 2048, 2) + Math.pow(equatorialDistance / 2048, 2))));
        assertThat(GeoUtils.quadTreeCellSize(12),
                equalTo(Math.sqrt(Math.pow(polarDistance / 4096, 2) + Math.pow(equatorialDistance / 4096, 2))));
    }

    public void testQuadTreeLevelsForPrecision() {
        for (int i = 0; i < 100; i++) {
            double precision = randomDouble() * 100;
            int level = GeoUtils.quadTreeLevelsForPrecision(precision);
            assertThat(GeoUtils.quadTreeCellSize(level), lessThanOrEqualTo(precision));
        }
    }

    public void testQuadTreeLevelsForPrecisionString() {
        for (int i = 0; i < 100; i++) {
            double precision = randomDouble() * 100;
            String precisionString = precision + "m";
            int level = GeoUtils.quadTreeLevelsForPrecision(precisionString);
            assertThat(GeoUtils.quadTreeCellSize(level), lessThanOrEqualTo(precision));
        }
    }

    public void testNormalizeLatInNormalRange() {
        for (int i = 0; i < 100; i++) {
            double testValue = (randomDouble() * 180.0) - 90.0;
            assertThat(GeoUtils.normalizeLat(testValue), closeTo(testValue, MAX_ACCEPTABLE_ERROR));
        }
    }

    public void testNormalizeLatOutsideNormalRange() {
        for (int i = 0; i < 100; i++) {
            double normalisedValue = (randomDouble() * 180.0) - 90.0;
            int shift = (randomBoolean() ? 1 : -1) * randomIntBetween(1, 10000);
            double testValue = normalisedValue + (180.0 * shift);
            double expectedValue = normalisedValue * (shift % 2 == 0 ? 1 : -1);
            assertThat(GeoUtils.normalizeLat(testValue), closeTo(expectedValue, MAX_ACCEPTABLE_ERROR));
        }
    }

    public void testNormalizeLatHuge() {
        assertThat(GeoUtils.normalizeLat(-18000000000091.0), equalTo(GeoUtils.normalizeLat(-091.0)));
        assertThat(GeoUtils.normalizeLat(-18000000000090.0), equalTo(GeoUtils.normalizeLat(-090.0)));
        assertThat(GeoUtils.normalizeLat(-18000000000089.0), equalTo(GeoUtils.normalizeLat(-089.0)));
        assertThat(GeoUtils.normalizeLat(-18000000000088.0), equalTo(GeoUtils.normalizeLat(-088.0)));
        assertThat(GeoUtils.normalizeLat(-18000000000001.0), equalTo(GeoUtils.normalizeLat(-001.0)));
        assertThat(GeoUtils.normalizeLat(+18000000000000.0), equalTo(GeoUtils.normalizeLat(+000.0)));
        assertThat(GeoUtils.normalizeLat(+18000000000001.0), equalTo(GeoUtils.normalizeLat(+001.0)));
        assertThat(GeoUtils.normalizeLat(+18000000000002.0), equalTo(GeoUtils.normalizeLat(+002.0)));
        assertThat(GeoUtils.normalizeLat(+18000000000088.0), equalTo(GeoUtils.normalizeLat(+088.0)));
        assertThat(GeoUtils.normalizeLat(+18000000000089.0), equalTo(GeoUtils.normalizeLat(+089.0)));
        assertThat(GeoUtils.normalizeLat(+18000000000090.0), equalTo(GeoUtils.normalizeLat(+090.0)));
        assertThat(GeoUtils.normalizeLat(+18000000000091.0), equalTo(GeoUtils.normalizeLat(+091.0)));
    }

    public void testNormalizeLatEdgeCases() {
        assertThat(GeoUtils.normalizeLat(Double.POSITIVE_INFINITY), equalTo(Double.NaN));
        assertThat(GeoUtils.normalizeLat(Double.NEGATIVE_INFINITY), equalTo(Double.NaN));
        assertThat(GeoUtils.normalizeLat(Double.NaN), equalTo(Double.NaN));
        assertThat(0.0, not(equalTo(-0.0)));
        assertThat(GeoUtils.normalizeLat(-0.0), equalTo(0.0));
        assertThat(GeoUtils.normalizeLat(0.0), equalTo(0.0));
        assertThat(GeoUtils.normalizeLat(-180.0), equalTo(0.0));
        assertThat(GeoUtils.normalizeLat(180.0), equalTo(0.0));
        assertThat(GeoUtils.normalizeLat(-90.0), equalTo(-90.0));
        assertThat(GeoUtils.normalizeLat(90.0), equalTo(90.0));
    }

    public void testNormalizeLonInNormalRange() {
        for (int i = 0; i < 100; i++) {
            double testValue = (randomDouble() * 360.0) - 180.0;
            assertThat(GeoUtils.normalizeLon(testValue), closeTo(testValue, MAX_ACCEPTABLE_ERROR));
        }
    }

    public void testNormalizeLonOutsideNormalRange() {
        for (int i = 0; i < 100; i++) {
            double normalisedValue = (randomDouble() * 360.0) - 180.0;
            double testValue = normalisedValue + ((randomBoolean() ? 1 : -1) * 360.0 * randomIntBetween(1, 10000));
            assertThat(GeoUtils.normalizeLon(testValue), closeTo(normalisedValue, MAX_ACCEPTABLE_ERROR));
        }
    }

    public void testNormalizeLonHuge() {
        assertThat(GeoUtils.normalizeLon(-36000000000181.0), equalTo(GeoUtils.normalizeLon(-181.0)));
        assertThat(GeoUtils.normalizeLon(-36000000000180.0), equalTo(GeoUtils.normalizeLon(-180.0)));
        assertThat(GeoUtils.normalizeLon(-36000000000179.0), equalTo(GeoUtils.normalizeLon(-179.0)));
        assertThat(GeoUtils.normalizeLon(-36000000000178.0), equalTo(GeoUtils.normalizeLon(-178.0)));
        assertThat(GeoUtils.normalizeLon(-36000000000001.0), equalTo(GeoUtils.normalizeLon(-001.0)));
        assertThat(GeoUtils.normalizeLon(+36000000000000.0), equalTo(GeoUtils.normalizeLon(+000.0)));
        assertThat(GeoUtils.normalizeLon(+36000000000001.0), equalTo(GeoUtils.normalizeLon(+001.0)));
        assertThat(GeoUtils.normalizeLon(+36000000000002.0), equalTo(GeoUtils.normalizeLon(+002.0)));
        assertThat(GeoUtils.normalizeLon(+36000000000178.0), equalTo(GeoUtils.normalizeLon(+178.0)));
        assertThat(GeoUtils.normalizeLon(+36000000000179.0), equalTo(GeoUtils.normalizeLon(+179.0)));
        assertThat(GeoUtils.normalizeLon(+36000000000180.0), equalTo(GeoUtils.normalizeLon(+180.0)));
        assertThat(GeoUtils.normalizeLon(+36000000000181.0), equalTo(GeoUtils.normalizeLon(+181.0)));
    }

    public void testNormalizeLonEdgeCases() {
        assertThat(GeoUtils.normalizeLon(Double.POSITIVE_INFINITY), equalTo(Double.NaN));
        assertThat(GeoUtils.normalizeLon(Double.NEGATIVE_INFINITY), equalTo(Double.NaN));
        assertThat(GeoUtils.normalizeLon(Double.NaN), equalTo(Double.NaN));
        assertThat(0.0, not(equalTo(-0.0)));
        assertThat(GeoUtils.normalizeLon(-0.0), equalTo(0.0));
        assertThat(GeoUtils.normalizeLon(0.0), equalTo(0.0));
        assertThat(GeoUtils.normalizeLon(-360.0), equalTo(0.0));
        assertThat(GeoUtils.normalizeLon(360.0), equalTo(0.0));
        assertThat(GeoUtils.normalizeLon(-180.0), equalTo(180.0));
        assertThat(GeoUtils.normalizeLon(180.0), equalTo(180.0));
    }

    public void testNormalizePointInNormalRange() {
        for (int i = 0; i < 100; i++) {
            double testLat = (randomDouble() * 180.0) - 90.0;
            double testLon = (randomDouble() * 360.0) - 180.0;
            GeoPoint testPoint = new GeoPoint(testLat, testLon);
            assertNormalizedPoint(testPoint, testPoint);
        }
    }

    public void testNormalizePointOutsideNormalRange() {
        for (int i = 0; i < 100; i++) {
            double normalisedLat = (randomDouble() * 180.0) - 90.0;
            double normalisedLon = (randomDouble() * 360.0) - 180.0;
            int shiftLat = (randomBoolean() ? 1 : -1) * randomIntBetween(1, 10000);
            int shiftLon = (randomBoolean() ? 1 : -1) * randomIntBetween(1, 10000);
            double testLat = normalisedLat + (180.0 * shiftLat);
            double testLon = normalisedLon + (360.0 * shiftLon);
            double expectedLat = normalisedLat * (shiftLat % 2 == 0 ? 1 : -1);
            double expectedLon = normalisedLon + (shiftLat % 2 == 0 ? 0 : 180);
            if (expectedLon > 180.0) {
                expectedLon -= 360;
            }
            GeoPoint testPoint = new GeoPoint(testLat, testLon);
            GeoPoint expectedPoint = new GeoPoint(expectedLat, expectedLon);
            assertNormalizedPoint(testPoint, expectedPoint);
        }
    }

    public void testNormalizePointOutsideNormalRange_withOptions() {
        for (int i = 0; i < 100; i++) {
            boolean normalize = randomBoolean();
            double normalisedLat = (randomDouble() * 180.0) - 90.0;
            double normalisedLon = (randomDouble() * 360.0) - 180.0;
            int shift = randomIntBetween(1, 10000);
            double testLat = normalisedLat + (180.0 * shift);
            double testLon = normalisedLon + (360.0 * shift);

            double expectedLat;
            double expectedLon;
            if (normalize) {
                expectedLat = normalisedLat * (shift % 2 == 0 ? 1 : -1);
                expectedLon = normalisedLon + ((shift % 2 == 1) ? 180 : 0);
                if (expectedLon > 180.0) {
                    expectedLon -= 360;
                }
            } else {
                expectedLat = testLat;
                expectedLon = testLon;
            }
            GeoPoint testPoint = new GeoPoint(testLat, testLon);
            GeoPoint expectedPoint = new GeoPoint(expectedLat, expectedLon);
            GeoUtils.normalizePoint(testPoint, normalize, normalize);
            assertThat("Unexpected Latitude", testPoint.lat(), closeTo(expectedPoint.lat(), MAX_ACCEPTABLE_ERROR));
            assertThat("Unexpected Longitude", testPoint.lon(), closeTo(expectedPoint.lon(), MAX_ACCEPTABLE_ERROR));
        }
    }

    public void testNormalizePointHuge() {
        assertNormalizedPoint(new GeoPoint(-18000000000091.0, -36000000000181.0), new GeoPoint(-089.0, -001.0));
        assertNormalizedPoint(new GeoPoint(-18000000000090.0, -36000000000180.0), new GeoPoint(-090.0, +180.0));
        assertNormalizedPoint(new GeoPoint(-18000000000089.0, -36000000000179.0), new GeoPoint(-089.0, -179.0));
        assertNormalizedPoint(new GeoPoint(-18000000000088.0, -36000000000178.0), new GeoPoint(-088.0, -178.0));
        assertNormalizedPoint(new GeoPoint(-18000000000001.0, -36000000000001.0), new GeoPoint(-001.0, -001.0));
        assertNormalizedPoint(new GeoPoint(+18000000000000.0, +18000000000000.0), new GeoPoint(+000.0, +000.0));
        assertNormalizedPoint(new GeoPoint(+18000000000001.0, +36000000000001.0), new GeoPoint(+001.0, +001.0));
        assertNormalizedPoint(new GeoPoint(+18000000000002.0, +36000000000002.0), new GeoPoint(+002.0, +002.0));
        assertNormalizedPoint(new GeoPoint(+18000000000088.0, +36000000000178.0), new GeoPoint(+088.0, +178.0));
        assertNormalizedPoint(new GeoPoint(+18000000000089.0, +36000000000179.0), new GeoPoint(+089.0, +179.0));
        assertNormalizedPoint(new GeoPoint(+18000000000090.0, +36000000000180.0), new GeoPoint(+090.0, +180.0));
        assertNormalizedPoint(new GeoPoint(+18000000000091.0, +36000000000181.0), new GeoPoint(+089.0, +001.0));

    }

    public void testNormalizePointEdgeCases() {
        assertNormalizedPoint(new GeoPoint(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY), new GeoPoint(Double.NaN, Double.NaN));
        assertNormalizedPoint(new GeoPoint(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY), new GeoPoint(Double.NaN, Double.NaN));
        assertNormalizedPoint(new GeoPoint(Double.NaN, Double.NaN), new GeoPoint(Double.NaN, Double.NaN));
        assertThat(0.0, not(equalTo(-0.0)));
        assertNormalizedPoint(new GeoPoint(-0.0, -0.0), new GeoPoint(0.0, 0.0));
        assertNormalizedPoint(new GeoPoint(0.0, 0.0), new GeoPoint(0.0, 0.0));
        assertNormalizedPoint(new GeoPoint(-180.0, -360.0), new GeoPoint(0.0, 180.0));
        assertNormalizedPoint(new GeoPoint(180.0, 360.0), new GeoPoint(0.0, 180.0));
        assertNormalizedPoint(new GeoPoint(-90.0, -180.0), new GeoPoint(-90.0, -180.0));
        assertNormalizedPoint(new GeoPoint(90.0, 180.0), new GeoPoint(90.0, 180.0));
        assertNormalizedPoint(new GeoPoint(100.0, 180.0), new GeoPoint(80.0, 0.0));
        assertNormalizedPoint(new GeoPoint(100.0, -180.0), new GeoPoint(80.0, 0.0));
        assertNormalizedPoint(new GeoPoint(-100.0, 180.0), new GeoPoint(-80.0, 0.0));
        assertNormalizedPoint(new GeoPoint(-100.0, -180.0), new GeoPoint(-80.0, 0.0));
    }

    public void testParseGeoPoint() throws IOException {
        for (int i = 0; i < 100; i++) {
            double lat = randomDouble() * 180 - 90 + randomIntBetween(-1000, 1000) * 180;
            double lon = randomDouble() * 360 - 180 + randomIntBetween(-1000, 1000) * 360;
            XContentBuilder json = jsonBuilder().startObject().field("lat", lat).field("lon", lon).endObject();
            try (XContentParser parser = createParser(json)) {
                parser.nextToken();
                GeoPoint point = GeoUtils.parseGeoPoint(parser);
                assertThat(point, equalTo(new GeoPoint(lat, lon)));
                assertThat(parser.currentToken(), is(Token.END_OBJECT));
                assertNull(parser.nextToken());
            }
            json = jsonBuilder().startObject().field("lat", String.valueOf(lat)).field("lon", String.valueOf(lon)).endObject();
            try (XContentParser parser = createParser(json)) {
                parser.nextToken();
                GeoPoint point = GeoUtils.parseGeoPoint(parser);
                assertThat(point, equalTo(new GeoPoint(lat, lon)));
            }
            json = jsonBuilder().startObject().startArray("foo").value(lon).value(lat).endArray().endObject();
            try (XContentParser parser = createParser(json)) {
                while (parser.currentToken() != Token.START_ARRAY) {
                    parser.nextToken();
                }
                GeoPoint point = GeoUtils.parseGeoPoint(parser);
                assertThat(point, equalTo(new GeoPoint(lat, lon)));
            }
            json = jsonBuilder().startObject().field("foo", lat + "," + lon).endObject();
            try (XContentParser parser = createParser(json)) {
                while (parser.currentToken() != Token.VALUE_STRING) {
                    parser.nextToken();
                }
                GeoPoint point = GeoUtils.parseGeoPoint(parser);
                assertThat(point, equalTo(new GeoPoint(lat, lon)));
            }
        }
    }

    public void testParseGeoPointStringZValueError() throws IOException {
        double lat = randomDouble() * 180 - 90 + randomIntBetween(-1000, 1000) * 180;
        double lon = randomDouble() * 360 - 180 + randomIntBetween(-1000, 1000) * 360;
        double alt = randomDouble() * 1000;
        XContentBuilder json = jsonBuilder().startObject().field("foo", lat + "," + lon + "," + alt).endObject();
        try (XContentParser parser = createParser(json)) {
            while (parser.currentToken() != Token.VALUE_STRING) {
                parser.nextToken();
            }
            Exception e = expectThrows(OpenSearchParseException.class,
                () -> GeoUtils.parseGeoPoint(parser, new GeoPoint(), false));
            assertThat(e.getMessage(), containsString("but [ignore_z_value] parameter is [false]"));
        }
    }

    public void testParseGeoPointArrayZValueError() throws IOException {
        double lat = randomDouble() * 180 - 90 + randomIntBetween(-1000, 1000) * 180;
        double lon = randomDouble() * 360 - 180 + randomIntBetween(-1000, 1000) * 360;
        double alt = randomDouble() * 1000;
        XContentBuilder json = jsonBuilder().startArray().value(lat).value(lon).value(alt).endArray();
        try (XContentParser parser = createParser(json)) {
            parser.nextToken();
            Exception e = expectThrows(OpenSearchParseException.class,
                () -> GeoUtils.parseGeoPoint(parser, new GeoPoint(), false));
            assertThat(e.getMessage(), containsString("but [ignore_z_value] parameter is [false]"));
            assertThat(parser.currentToken(), is(Token.END_ARRAY));
            assertNull(parser.nextToken());
        }
    }

    public void testParseGeoPointGeohash() throws IOException {
        for (int i = 0; i < 100; i++) {
            int geoHashLength = randomIntBetween(1, Geohash.PRECISION);
            StringBuilder geohashBuilder = new StringBuilder(geoHashLength);
            for (int j = 0; j < geoHashLength; j++) {
                geohashBuilder.append(BASE_32[randomInt(BASE_32.length - 1)]);
            }
            XContentBuilder json = jsonBuilder().startObject().field("geohash", geohashBuilder.toString()).endObject();
            try (XContentParser parser = createParser(json)) {
                parser.nextToken();
                GeoPoint point = GeoUtils.parseGeoPoint(parser);
                assertThat(point.lat(), allOf(lessThanOrEqualTo(90.0), greaterThanOrEqualTo(-90.0)));
                assertThat(point.lon(), allOf(lessThanOrEqualTo(180.0), greaterThanOrEqualTo(-180.0)));
                assertThat(parser.currentToken(), is(Token.END_OBJECT));
                assertNull(parser.nextToken());
            }
            json = jsonBuilder().startObject().field("geohash", geohashBuilder.toString()).endObject();
            try (XContentParser parser = createParser(json)) {
                while (parser.currentToken() != Token.VALUE_STRING) {
                    parser.nextToken();
                }
                GeoPoint point = GeoUtils.parseGeoPoint(parser);
                assertThat(point.lat(), allOf(lessThanOrEqualTo(90.0), greaterThanOrEqualTo(-90.0)));
                assertThat(point.lon(), allOf(lessThanOrEqualTo(180.0), greaterThanOrEqualTo(-180.0)));
            }
        }
    }

    public void testParseGeoPointGeohashWrongType() throws IOException {
        XContentBuilder json = jsonBuilder().startObject().field("geohash", 1.0).endObject();
        try (XContentParser parser = createParser(json)) {
            parser.nextToken();
            Exception e = expectThrows(OpenSearchParseException.class, () -> GeoUtils.parseGeoPoint(parser));
            assertThat(e.getMessage(), containsString("geohash must be a string"));
            assertThat(parser.currentToken(), is(Token.END_OBJECT));
            assertNull(parser.nextToken());
        }
    }

    public void testParseGeoPointLatNoLon() throws IOException {
        double lat = 0.0;
        XContentBuilder json = jsonBuilder().startObject().field("lat", lat).endObject();
        try (XContentParser parser = createParser(json)) {
            parser.nextToken();
            Exception e = expectThrows(OpenSearchParseException.class, () -> GeoUtils.parseGeoPoint(parser));
            assertThat(e.getMessage(), is("field [lon] missing"));
            assertThat(parser.currentToken(), is(Token.END_OBJECT));
            assertNull(parser.nextToken());
        }
    }

    public void testParseGeoPointLonNoLat() throws IOException {
        double lon = 0.0;
        XContentBuilder json = jsonBuilder().startObject().field("lon", lon).endObject();
        try (XContentParser parser = createParser(json)) {
            parser.nextToken();
            Exception e = expectThrows(OpenSearchParseException.class, () -> GeoUtils.parseGeoPoint(parser));
            assertThat(e.getMessage(), is("field [lat] missing"));
            assertThat(parser.currentToken(), is(Token.END_OBJECT));
            assertNull(parser.nextToken());
        }
    }

    public void testParseGeoPointLonWrongType() throws IOException {
        double lat = 0.0;
        XContentBuilder json = jsonBuilder().startObject().field("lat", lat).field("lon", false).endObject();
        try (XContentParser parser = createParser(json)) {
            parser.nextToken();
            Exception e = expectThrows(OpenSearchParseException.class, () -> GeoUtils.parseGeoPoint(parser));
            assertThat(e.getMessage(), is("longitude must be a number"));
            assertThat(parser.currentToken(), is(Token.END_OBJECT));
            assertNull(parser.nextToken());
        }
    }

    public void testParseGeoPointLatWrongType() throws IOException {
        double lon = 0.0;
        XContentBuilder json = jsonBuilder().startObject().field("lat", false).field("lon", lon).endObject();
        try (XContentParser parser = createParser(json)) {
            parser.nextToken();
            Exception e = expectThrows(OpenSearchParseException.class, () -> GeoUtils.parseGeoPoint(parser));
            assertThat(e.getMessage(), is("latitude must be a number"));
            assertThat(parser.currentToken(), is(Token.END_OBJECT));
            assertNull(parser.nextToken());
        }
    }

    public void testParseGeoPointExtraField() throws IOException {
        double lat = 0.0;
        double lon = 0.0;
        XContentBuilder json = jsonBuilder().startObject()
            .field("lat", lat).field("lon", lon).field("foo", true).endObject();
        try (XContentParser parser = createParser(json)) {
            parser.nextToken();
            Exception e = expectThrows(OpenSearchParseException.class, () -> GeoUtils.parseGeoPoint(parser));
            assertThat(e.getMessage(), is("field must be either [lat], [lon] or [geohash]"));
        }
    }

    public void testParseGeoPointLonLatGeoHash() throws IOException {
        double lat = 0.0;
        double lon = 0.0;
        String geohash = "abcd";
        XContentBuilder json = jsonBuilder().startObject()
            .field("lat", lat).field("lon", lon).field("geohash", geohash).endObject();
        try (XContentParser parser = createParser(json)) {
            parser.nextToken();
            Exception e = expectThrows(OpenSearchParseException.class, () -> GeoUtils.parseGeoPoint(parser));
            assertThat(e.getMessage(), containsString("field must be either lat/lon or geohash"));
        }
    }

    public void testParseGeoPointArrayTooManyValues() throws IOException {
        double lat = 0.0;
        double lon = 0.0;
        double elev = 0.0;
        XContentBuilder json = jsonBuilder().startObject().startArray("foo").value(lon).value(lat).value(elev).endArray().endObject();
        try (XContentParser parser = createParser(json)) {
            while (parser.currentToken() != Token.START_ARRAY) {
                parser.nextToken();
            }
            Exception e = expectThrows(OpenSearchParseException.class, () -> GeoUtils.parseGeoPoint(parser));
            assertThat(e.getMessage(),
                is("Exception parsing coordinates: found Z value [0.0] but [ignore_z_value] parameter is [false]"));
        }
    }

    public void testParseGeoPointArray3D() throws IOException {
        double lat = 90.0;
        double lon = -180.0;
        double elev = 0.0;
        XContentBuilder json = jsonBuilder().startObject().startArray("foo").value(lon).value(lat).value(elev).endArray().endObject();
        try (XContentParser parser = createParser(json)) {
            while (parser.currentToken() != Token.START_ARRAY) {
                parser.nextToken();
            }
            GeoPoint point = GeoUtils.parseGeoPoint(parser, new GeoPoint(), true);
            assertThat(point.lat(), equalTo(lat));
            assertThat(point.lon(), equalTo(lon));
        }
    }

    public void testParseGeoPointArrayWrongType() throws IOException {
        double lat = 0.0;
        boolean lon = false;
        XContentBuilder json = jsonBuilder().startObject().startArray("foo").value(lon).value(lat).endArray().endObject();
        try (XContentParser parser = createParser(json)) {
            while (parser.currentToken() != Token.START_ARRAY) {
                parser.nextToken();
            }
            Exception e = expectThrows(OpenSearchParseException.class, () -> GeoUtils.parseGeoPoint(parser));
            assertThat(e.getMessage(), is("numeric value expected"));
            assertThat(parser.currentToken(), is(Token.END_ARRAY));
            assertThat(parser.nextToken(), is(Token.END_OBJECT));
            assertNull(parser.nextToken());
        }
    }

    public void testParseGeoPointInvalidType() throws IOException {
        XContentBuilder json = jsonBuilder().startObject().field("foo", 5).endObject();
        try (XContentParser parser = createParser(json)) {
            while (parser.currentToken() != Token.VALUE_NUMBER) {
                parser.nextToken();
            }
            Exception e = expectThrows(OpenSearchParseException.class, () -> GeoUtils.parseGeoPoint(parser));
            assertThat(e.getMessage(), is("geo_point expected"));
        }
    }

    public void testPrefixTreeCellSizes() {
        assertThat(GeoUtils.EARTH_SEMI_MAJOR_AXIS, equalTo(DistanceUtils.EARTH_EQUATORIAL_RADIUS_KM * 1000));
        assertThat(GeoUtils.quadTreeCellWidth(0), lessThanOrEqualTo(GeoUtils.EARTH_EQUATOR));

        SpatialContext spatialContext = new SpatialContext(true);

        GeohashPrefixTree geohashPrefixTree = new GeohashPrefixTree(spatialContext, GeohashPrefixTree.getMaxLevelsPossible() / 2);
        Cell gNode = geohashPrefixTree.getWorldCell();

        for (int i = 0; i < geohashPrefixTree.getMaxLevels(); i++) {
            double width = GeoUtils.geoHashCellWidth(i);
            double height = GeoUtils.geoHashCellHeight(i);
            double size = GeoUtils.geoHashCellSize(i);
            double degrees = 360.0 * width / GeoUtils.EARTH_EQUATOR;
            int level = GeoUtils.quadTreeLevelsForPrecision(size);

            assertThat(GeoUtils.quadTreeCellWidth(level), lessThanOrEqualTo(width));
            assertThat(GeoUtils.quadTreeCellHeight(level), lessThanOrEqualTo(height));
            assertThat(GeoUtils.geoHashLevelsForPrecision(size), equalTo(geohashPrefixTree.getLevelForDistance(degrees)));

            assertThat("width at level " + i,
                gNode.getShape().getBoundingBox().getWidth(), equalTo(360.d * width / GeoUtils.EARTH_EQUATOR));
            assertThat("height at level " + i, gNode.getShape().getBoundingBox().getHeight(), equalTo(180.d * height
                    / GeoUtils.EARTH_POLAR_DISTANCE));

            gNode = gNode.getNextLevelCells(null).next();
        }

        QuadPrefixTree quadPrefixTree = new QuadPrefixTree(spatialContext);
        Cell qNode = quadPrefixTree.getWorldCell();
        for (int i = 0; i < quadPrefixTree.getMaxLevels(); i++) {

            double degrees = 360.0 / (1L << i);
            double width = GeoUtils.quadTreeCellWidth(i);
            double height = GeoUtils.quadTreeCellHeight(i);
            double size = GeoUtils.quadTreeCellSize(i);
            int level = GeoUtils.quadTreeLevelsForPrecision(size);

            assertThat(GeoUtils.quadTreeCellWidth(level), lessThanOrEqualTo(width));
            assertThat(GeoUtils.quadTreeCellHeight(level), lessThanOrEqualTo(height));
            assertThat(GeoUtils.quadTreeLevelsForPrecision(size), equalTo(quadPrefixTree.getLevelForDistance(degrees)));

            assertThat("width at level " + i,
                qNode.getShape().getBoundingBox().getWidth(), equalTo(360.d * width / GeoUtils.EARTH_EQUATOR));
            assertThat("height at level " + i, qNode.getShape().getBoundingBox().getHeight(), equalTo(180.d * height
                    / GeoUtils.EARTH_POLAR_DISTANCE));

            qNode = qNode.getNextLevelCells(null).next();
        }
    }

    public void testParseGeoPointGeohashPositions() throws IOException {
        assertNormalizedPoint(parseGeohash("drt5",
            GeoUtils.EffectivePoint.TOP_LEFT), new GeoPoint(42.890625, -71.71875));
        assertNormalizedPoint(parseGeohash("drt5",
            GeoUtils.EffectivePoint.TOP_RIGHT), new GeoPoint(42.890625, -71.3671875));
        assertNormalizedPoint(parseGeohash("drt5",
            GeoUtils.EffectivePoint.BOTTOM_LEFT), new GeoPoint(42.71484375, -71.71875));
        assertNormalizedPoint(parseGeohash("drt5",
            GeoUtils.EffectivePoint.BOTTOM_RIGHT), new GeoPoint(42.71484375, -71.3671875));
        assertNormalizedPoint(parseGeohash("drtk",
            GeoUtils.EffectivePoint.BOTTOM_LEFT), new GeoPoint(42.890625, -71.3671875));
    }

    private GeoPoint parseGeohash(String geohash, GeoUtils.EffectivePoint effectivePoint) throws IOException {
        try (XContentParser parser = createParser(jsonBuilder().startObject().field("geohash", geohash).endObject())) {
            parser.nextToken();
            return GeoUtils.parseGeoPoint(parser, new GeoPoint(), randomBoolean(), effectivePoint);
        }
    }

    private static void assertNormalizedPoint(GeoPoint input, GeoPoint expected) {
        GeoUtils.normalizePoint(input);
        if (Double.isNaN(expected.lat())) {
            assertThat("Unexpected Latitude", input.lat(), equalTo(expected.lat()));
        } else {
            assertThat("Unexpected Latitude", input.lat(), closeTo(expected.lat(), MAX_ACCEPTABLE_ERROR));
        }
        if (Double.isNaN(expected.lon())) {
            assertThat("Unexpected Longitude", input.lon(), equalTo(expected.lon()));
        } else {
            assertThat("Unexpected Longitude", input.lon(), closeTo(expected.lon(), MAX_ACCEPTABLE_ERROR));
        }
    }
}
