/*******************************************************************************
 * Copyright (c) 2024 The University of Reading
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the name of the University of Reading nor the names of its
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 ******************************************************************************/

package uk.ac.rdg.resc.edal.ncwms;

import org.junit.Before;
import org.junit.Test;
import uk.ac.rdg.resc.edal.ncwms.config.NcwmsDynamicService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit-Tests für die Performance-Fixes PERF-001 bis PERF-003 in
 * {@link NcwmsCatalogue}.
 *
 * <p>
 * Testet direkt die ohne Servlet-Container testbaren Methoden:
 * <ul>
 * <li>{@code rebuildAliasIndex()} — PERF-002</li>
 * <li>{@code getDynamicServiceFromLayerName()} — PERF-002 / PERF-003</li>
 * </ul>
 *
 * <p>
 * Da NcwmsCatalogue keinen einfachen Default-Konstruktor hat, werden
 * die zwei package-private Methoden über eine anonyme Testsubklasse getestet.
 */
public class NcwmsCataloguePerformanceTest {

    /**
     * Minimale Testsubklasse die nur die package-private Performance-Methoden
     * exponiert. Kein echter EhCache, kein Filesystem.
     */
    private static class TestCatalogue extends NcwmsCatalogue {
        TestCatalogue() {
            super(); // no-arg ctor — does NOT initialise cache or config
        }
    }

    private TestCatalogue catalogue;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private NcwmsDynamicService service(String alias, String regexOrNull) {
        NcwmsDynamicService svc = new NcwmsDynamicService();
        svc.setAlias(alias);
        if (regexOrNull != null) {
            svc.setDatasetIdMatch(regexOrNull);
        }
        return svc;
    }

    @Before
    public void setUp() {
        catalogue = new TestCatalogue();
    }

    // -------------------------------------------------------------------------
    // rebuildAliasIndex: PERF-002
    // -------------------------------------------------------------------------

    @Test
    public void testRebuildWithNullIsNoOp() {
        catalogue.rebuildAliasIndex(null);
        // Should not throw; getDynamicServiceFromLayerName returns null safely
        assertNull(catalogue.getDynamicServiceFromLayerName("anything"));
    }

    @Test
    public void testRebuildWithEmptyListIsNoOp() {
        catalogue.rebuildAliasIndex(Collections.emptyList());
        assertNull(catalogue.getDynamicServiceFromLayerName("data/file.nc"));
    }

    @Test
    public void testSingleServiceFound() {
        NcwmsDynamicService svc = service("myalias", ".*");
        catalogue.rebuildAliasIndex(Collections.singletonList(svc));
        NcwmsDynamicService found = catalogue.getDynamicServiceFromLayerName("myalias/some/path.nc");
        assertNotNull("Should resolve alias prefix", found);
        assertEquals("myalias", found.getAlias());
    }

    @Test
    public void testUnknownAliasReturnsNull() {
        catalogue.rebuildAliasIndex(Collections.singletonList(service("known", ".*")));
        assertNull("Unknown alias must return null",
                catalogue.getDynamicServiceFromLayerName("unknown/path.nc"));
    }

    // -------------------------------------------------------------------------
    // Longest-prefix-match: PERF-002 correctness regression guard
    // -------------------------------------------------------------------------

    @Test
    public void testLongestPrefixMatchWinsOverShorter() {
        NcwmsDynamicService shortAlias = service("data", ".*");
        NcwmsDynamicService longAlias = service("data/model", ".*");
        // Insert short first — old O(n) implementation would pick short (last match
        // bug)
        List<NcwmsDynamicService> services = Arrays.asList(shortAlias, longAlias);
        catalogue.rebuildAliasIndex(services);

        NcwmsDynamicService result = catalogue.getDynamicServiceFromLayerName("data/model/run1.nc");
        assertNotNull(result);
        assertEquals("data/model alias must win over 'data' (longest-prefix-match)",
                "data/model", result.getAlias());
    }

    @Test
    public void testShortAliasWhenNoLongerMatchExists() {
        NcwmsDynamicService shortAlias = service("data", ".*");
        NcwmsDynamicService longAlias = service("data/model", ".*");
        catalogue.rebuildAliasIndex(Arrays.asList(shortAlias, longAlias));

        // "data/obs" doesn't start with "data/model" but starts with "data"
        NcwmsDynamicService result = catalogue.getDynamicServiceFromLayerName("data/obs/run1.nc");
        assertNotNull(result);
        assertEquals("'data' alias must match when 'data/model' doesn't fit",
                "data", result.getAlias());
    }

    @Test
    public void testDuplicateAliasKeepsFirst() {
        NcwmsDynamicService first = service("dup", ".*");
        NcwmsDynamicService second = service("dup", ".*\\.nc");
        catalogue.rebuildAliasIndex(Arrays.asList(first, second));

        NcwmsDynamicService result = catalogue.getDynamicServiceFromLayerName("dup/run.nc");
        assertNotNull(result);
        // First entry wins on duplicate alias — deterministic behavior
        assertSame("First service for duplicate alias must be kept", first, result);
    }

    // -------------------------------------------------------------------------
    // Null safety: PERF-001 / PERF-003
    // -------------------------------------------------------------------------

    @Test
    public void testNullLayerNameReturnsNull() {
        catalogue.rebuildAliasIndex(Collections.singletonList(service("alias", ".*")));
        assertNull("Null layerName must not throw",
                catalogue.getDynamicServiceFromLayerName(null));
    }

    // -------------------------------------------------------------------------
    // Regex guard: correctness parity with old implementation
    // -------------------------------------------------------------------------

    @Test
    public void testRegexGuardBlocksNonMatchingPath() {
        // Only paths ending in .nc are allowed
        NcwmsDynamicService svc = service("model", ".*\\.nc");
        catalogue.rebuildAliasIndex(Collections.singletonList(svc));

        assertNull("Path not matching regex must be rejected",
                catalogue.getDynamicServiceFromLayerName("model/run.txt"));
    }

    @Test
    public void testRegexGuardAllowsMatchingPath() {
        NcwmsDynamicService svc = service("model", ".*\\.nc");
        catalogue.rebuildAliasIndex(Collections.singletonList(svc));

        assertNotNull("Path matching regex must be found",
                catalogue.getDynamicServiceFromLayerName("model/run.nc"));
    }

    @Test
    public void testServiceWithNoRegexAlwaysMatches() {
        // null datasetIdMatch means no regex filter
        NcwmsDynamicService svc = service("open", null);
        catalogue.rebuildAliasIndex(Collections.singletonList(svc));

        assertNotNull("Service with no regex must always match",
                catalogue.getDynamicServiceFromLayerName("open/any/path"));
    }

    // -------------------------------------------------------------------------
    // Rebuild is idempotent
    // -------------------------------------------------------------------------

    @Test
    public void testRebuildReplacesOldIndex() {
        catalogue.rebuildAliasIndex(Collections.singletonList(service("old", ".*")));
        assertNotNull(catalogue.getDynamicServiceFromLayerName("old/file.nc"));

        catalogue.rebuildAliasIndex(Collections.singletonList(service("new", ".*")));
        assertNull("Old alias must not be found after rebuild",
                catalogue.getDynamicServiceFromLayerName("old/file.nc"));
        assertNotNull("New alias must be found after rebuild",
                catalogue.getDynamicServiceFromLayerName("new/file.nc"));
    }
}
