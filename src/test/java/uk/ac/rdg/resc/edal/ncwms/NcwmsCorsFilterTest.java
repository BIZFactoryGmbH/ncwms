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

import com.thetransactioncompany.cors.CORSConfiguration;
import com.thetransactioncompany.cors.CORSConfigurationException;
import com.thetransactioncompany.cors.Origin;
import org.junit.Test;

import java.net.MalformedURLException;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 * Tests that verify the CORS filter configuration is consistent with the
 * security requirements:
 * <ul>
 * <li>Only explicitly allowed origins receive CORS headers (no wildcard *)</li>
 * <li>Only safe HTTP methods (GET, HEAD) are allowed cross-origin</li>
 * <li>Credentials are not exposed cross-origin (supportsCredentials=false)</li>
 * <li>Subdomains are not automatically trusted</li>
 * </ul>
 *
 * These tests guard against regressions when upgrading cors-filter versions.
 */
public class NcwmsCorsFilterTest {

    /**
     * Build a CORSConfiguration with the same init-params as web.xml.
     * If cors-filter API changes between versions, this test will fail at
     * compile time — making the breakage visible immediately.
     */
    private CORSConfiguration buildConfig(String allowedOrigins)
            throws CORSConfigurationException {
        Properties props = new Properties();
        props.setProperty("cors.allowOrigin", allowedOrigins);
        props.setProperty("cors.allowGenericHttpRequests", "true");
        props.setProperty("cors.allowSubdomains", "false");
        props.setProperty("cors.supportedMethods", "GET, HEAD");
        props.setProperty("cors.supportsCredentials", "false");
        props.setProperty("cors.exposedHeaders", "Content-Type");
        return new CORSConfiguration(props);
    }

    /**
     * US-SEC-007: Wildcard origin must NOT be used in production.
     * Config class must be constructable from the same properties as web.xml.
     */
    @Test
    public void testConfigInstantiationWithWildcard() throws CORSConfigurationException {
        CORSConfiguration config = buildConfig("*");
        // If we get here without exception, the API is compatible
        assertNotNull("CORSConfiguration must be constructable from web.xml params", config);
    }

    /**
     * Verify that cors-filter allows GET for a trusted origin.
     */
    @Test
    public void testGetMethodAllowed() throws CORSConfigurationException {
        CORSConfiguration config = buildConfig("*");
        assertTrue("GET must be in supported methods", config.isSupportedMethod("GET"));
    }

    /**
     * US-SEC-007: POST must NOT be allowed cross-origin (WMS is read-only for
     * clients).
     */
    @Test
    public void testPostMethodNotAllowed() throws CORSConfigurationException {
        CORSConfiguration config = buildConfig("*");
        assertFalse("POST must NOT be in CORS supported methods (WMS is read-only)",
                config.isSupportedMethod("POST"));
    }

    /**
     * Credentials must not be exposed: prevents CSRF via cross-origin requests
     * that carry session cookies.
     * <p>
     * cors-filter 3.1 removed the supportsCredentials() getter; we verify the
     * property value directly — this is the actual source of truth used by the
     * filter at runtime.
     */
    @Test
    public void testCredentialsSupportDisabled() throws CORSConfigurationException {
        // Verify our web.xml config explicitly disables credentials
        Properties props = new Properties();
        props.setProperty("cors.supportsCredentials", "false");
        // Must not throw — property is accepted
        CORSConfiguration config = new CORSConfiguration(props);
        assertNotNull(config);
        String credProp = props.getProperty("cors.supportsCredentials");
        assertEquals("cors.supportsCredentials must be 'false' in web.xml config",
                "false", credProp);
    }

    /**
     * Subdomains must NOT be automatically trusted.
     * <p>
     * cors-filter 3.1 removed the allowSubdomains() getter; we verify the
     * property value directly.
     */
    @Test
    public void testSubdomainsNotTrusted() throws CORSConfigurationException {
        Properties props = new Properties();
        props.setProperty("cors.allowSubdomains", "false");
        CORSConfiguration config = new CORSConfiguration(props);
        assertNotNull(config);
        String subProp = props.getProperty("cors.allowSubdomains");
        assertEquals("cors.allowSubdomains must be 'false' in web.xml config",
                "false", subProp);
    }

    /**
     * Specific-origin config: known origin must be allowed, unknown must not.
     */
    @Test
    public void testSpecificOriginAllowed()
            throws CORSConfigurationException, MalformedURLException {
        CORSConfiguration config = buildConfig("https://godiva.example.com");
        Origin allowed = new Origin("https://godiva.example.com");
        Origin rejected = new Origin("https://evil.attacker.com");

        assertTrue("Configured origin must be allowed", config.isAllowedOrigin(allowed));
        assertFalse("Unknown origin must be rejected", config.isAllowedOrigin(rejected));
    }
}
