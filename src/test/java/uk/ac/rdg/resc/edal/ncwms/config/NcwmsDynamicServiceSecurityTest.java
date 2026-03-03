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

package uk.ac.rdg.resc.edal.ncwms.config;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for security-relevant behaviour of {@link NcwmsDynamicService}.
 *
 * Guards against regressions in the ReDoS mitigations implemented as part of
 * the CodeQL regex-injection alert fix (US-CR-002 / java/regex-injection).
 */
public class NcwmsDynamicServiceSecurityTest {

    private NcwmsDynamicService buildService(String pattern) {
        NcwmsDynamicService svc = new NcwmsDynamicService();
        svc.setDatasetIdMatch(pattern);
        return svc;
    }

    // -------------------------------------------------------------------------
    // Normal usage — valid patterns must work
    // -------------------------------------------------------------------------

    @Test
    public void testSimplePatternIsAccepted() {
        NcwmsDynamicService svc = buildService(".*\\.nc");
        assertNotNull(svc.getIdMatchPattern());
    }

    @Test
    public void testGroupPatternIsAccepted() {
        NcwmsDynamicService svc = buildService("(model|obs)/.*");
        assertNotNull(svc.getIdMatchPattern());
    }

    @Test
    public void testPatternMatchesCorrectly() {
        NcwmsDynamicService svc = buildService("data/.*\\.nc");
        assertTrue(svc.getIdMatchPattern().matcher("data/model.nc").matches());
        assertFalse(svc.getIdMatchPattern().matcher("other/file.txt").matches());
    }

    @Test
    public void testLeadingTrailingWhitespaceIsTrimmed() {
        NcwmsDynamicService svc = buildService("  .* ");
        assertEquals(".*", svc.getDatasetIdMatch());
    }

    // -------------------------------------------------------------------------
    // ReDoS / injection mitigation
    // -------------------------------------------------------------------------

    @Test(expected = IllegalArgumentException.class)
    public void testPatternExceedingMaxLengthIsRejected() {
        // Build a pattern that's longer than MAX_REGEX_LENGTH (500 chars)
        String longPattern = "a".repeat(501);
        buildService(longPattern);
    }

    @Test
    public void testPatternAtExactMaxLengthIsAccepted() {
        String maxPattern = "a".repeat(500);
        NcwmsDynamicService svc = buildService(maxPattern);
        assertNotNull(svc.getIdMatchPattern());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidRegexSyntaxIsRejected() {
        // Unclosed group — invalid regex
        buildService("(unclosed");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidQuantifierIsRejected() {
        // Invalid repetition
        buildService("a{invalid}");
    }

    // -------------------------------------------------------------------------
    // getIdMatchPattern() — JAXB bypass path
    // -------------------------------------------------------------------------

    @Test
    public void testGetIdMatchPatternReturnsNullForNullDatasetIdMatch() {
        // New service with no pattern set — should not throw, should return null
        NcwmsDynamicService svc = new NcwmsDynamicService();
        assertNull("Pattern should be null when datasetIdMatch is not set",
                svc.getIdMatchPattern());
    }

    @Test(expected = IllegalStateException.class)
    public void testGetIdMatchPatternRejectsOversizedPatternFromXml() throws Exception {
        // Simulate JAXB loading: set field directly via reflection (bypasses setter)
        NcwmsDynamicService svc = new NcwmsDynamicService();
        java.lang.reflect.Field f = NcwmsDynamicService.class.getDeclaredField("datasetIdMatch");
        f.setAccessible(true);
        f.set(svc, "x".repeat(501)); // too long
        svc.getIdMatchPattern(); // must throw IllegalStateException
    }
}
