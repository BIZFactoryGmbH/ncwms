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

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link NcwmsAdminServlet#validateLocation(String)} and
 * {@link NcwmsAdminServlet#htmlEscape(String)}.
 *
 * These tests serve as regression guards for the security fixes implemented in
 * US-CR-002 (dataset location allowlist / SSRF prevention) and the XSS fix.
 */
public class NcwmsAdminServletSecurityTest {

    // -------------------------------------------------------------------------
    // validateLocation — allowed locations
    // -------------------------------------------------------------------------

    @Test
    public void testNullLocationIsAllowed() {
        // Null is handled downstream — must not throw
        NcwmsAdminServlet.validateLocation(null);
    }

    @Test
    public void testEmptyLocationIsAllowed() {
        NcwmsAdminServlet.validateLocation("");
        NcwmsAdminServlet.validateLocation("   ");
    }

    @Test
    public void testHttpLocationIsAllowed() {
        NcwmsAdminServlet.validateLocation("http://thredds.example.com/catalog/data.nc");
    }

    @Test
    public void testHttpsLocationIsAllowed() {
        NcwmsAdminServlet.validateLocation("https://opendap.example.org/data/model.nc4");
    }

    @Test
    public void testRelativePathIsAllowed() {
        NcwmsAdminServlet.validateLocation("/data/model-runs/ecmwf.nc");
        NcwmsAdminServlet.validateLocation("data/local/test.nc");
    }

    @Test
    public void testSafeFileProtocolIsAllowed() {
        // file:// pointing to non-sensitive paths is acceptable
        NcwmsAdminServlet.validateLocation("file:///var/data/model.nc");
        NcwmsAdminServlet.validateLocation("file:///home/ncwms/data/test.nc");
    }

    // -------------------------------------------------------------------------
    // validateLocation — blocked locations (SSRF / path traversal)
    // -------------------------------------------------------------------------

    @Test(expected = IllegalArgumentException.class)
    public void testJarProtocolIsBlocked() {
        NcwmsAdminServlet.validateLocation("jar:file:///app/lib/evil.jar!/data");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGopherProtocolIsBlocked() {
        NcwmsAdminServlet.validateLocation("gopher://attacker.com/secret");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFtpProtocolIsBlocked() {
        NcwmsAdminServlet.validateLocation("ftp://files.example.com/secret.nc");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLdapProtocolIsBlocked() {
        NcwmsAdminServlet.validateLocation("ldap://ldap.example.com/cn=secret");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFileEtcPasswdIsBlocked() {
        NcwmsAdminServlet.validateLocation("file:///etc/passwd");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFileEtcShadowIsBlocked() {
        NcwmsAdminServlet.validateLocation("file:///etc/shadow");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFileProcSelfIsBlocked() {
        NcwmsAdminServlet.validateLocation("file:///proc/self/environ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFileSysIsBlocked() {
        NcwmsAdminServlet.validateLocation("file:///sys/class/net/eth0/address");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFileRootIsBlocked() {
        NcwmsAdminServlet.validateLocation("file:///root/.ssh/id_rsa");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAwsMetadataServiceIsBlocked() {
        // AWS IMDS — classic SSRF target
        NcwmsAdminServlet.validateLocation("http://169.254.169.254/latest/meta-data/");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAwsMetadataIpRangeIsBlocked() {
        NcwmsAdminServlet.validateLocation("http://169.254.0.1/secret");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGcpMetadataServiceIsBlocked() {
        // GCP metadata server
        NcwmsAdminServlet.validateLocation("http://metadata.google.internal/computeMetadata/v1/");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCaseInsensitiveJarIsBlocked() {
        // Protocol scheme must be matched case-insensitively
        NcwmsAdminServlet.validateLocation("JAR:file:///evil.jar!/");
    }

    // -------------------------------------------------------------------------
    // htmlEscape — XSS prevention
    // -------------------------------------------------------------------------

    @Test
    public void testHtmlEscapeNull() {
        assertEquals("", NcwmsAdminServlet.htmlEscape(null));
    }

    @Test
    public void testHtmlEscapeCleanString() {
        assertEquals("dataset123", NcwmsAdminServlet.htmlEscape("dataset123"));
    }

    @Test
    public void testHtmlEscapeAmpersand() {
        assertEquals("a&amp;b", NcwmsAdminServlet.htmlEscape("a&b"));
    }

    @Test
    public void testHtmlEscapeLessThan() {
        assertEquals("&lt;script&gt;", NcwmsAdminServlet.htmlEscape("<script>"));
    }

    @Test
    public void testHtmlEscapeXssPayload() {
        String payload = "<script>alert('xss')</script>";
        String escaped = NcwmsAdminServlet.htmlEscape(payload);
        assertFalse("Escaped output must not contain raw <script>", escaped.contains("<script>"));
        assertTrue(escaped.contains("&lt;script&gt;"));
    }

    @Test
    public void testHtmlEscapeDoubleQuote() {
        assertEquals("&quot;value&quot;", NcwmsAdminServlet.htmlEscape("\"value\""));
    }

    @Test
    public void testHtmlEscapeSingleQuote() {
        assertEquals("&#x27;value&#x27;", NcwmsAdminServlet.htmlEscape("'value'"));
    }
}
