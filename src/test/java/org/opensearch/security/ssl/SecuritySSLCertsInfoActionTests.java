/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.security.ssl;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minidev.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.opensearch.common.settings.Settings;
import org.opensearch.security.ssl.util.SSLConfigConstants;
import org.opensearch.security.support.ConfigConstants;
import org.opensearch.security.test.SingleClusterTest;
import org.opensearch.security.test.helper.file.FileHelper;
import org.opensearch.security.test.helper.rest.RestHelper;

import static org.opensearch.security.OpenSearchSecurityPlugin.LEGACY_OPENDISTRO_PREFIX;
import static org.opensearch.security.OpenSearchSecurityPlugin.PLUGINS_PREFIX;

@RunWith(Parameterized.class)
public class SecuritySSLCertsInfoActionTests extends SingleClusterTest {
    private final String ENDPOINT;

    public SecuritySSLCertsInfoActionTests(String endpoint){
        ENDPOINT = endpoint;
    }

    @Parameterized.Parameters
    public static Iterable<String> endpoints() {
        return ImmutableList.of(
                LEGACY_OPENDISTRO_PREFIX +  "/api/ssl/certs",
                PLUGINS_PREFIX +  "/api/ssl/certs"
        );
    }

    private final List<Map<String, String>> NODE_CERT_DETAILS = ImmutableList.of(
        ImmutableMap.of(
            "issuer_dn", "CN=Example Com Inc. Signing CA,OU=Example Com Inc. Signing CA,O=Example Com Inc.,DC=example,DC=com",
            "subject_dn", "CN=node-0.example.com,OU=SSL,O=Test,L=Test,C=DE",
            "san", "[[2, node-0.example.com], [2, localhost], [7, 127.0.0.1], [8, 1.2.3.4.5.5]]",
            "not_before","2018-05-05T14:37:09Z",
            "not_after","2028-05-02T14:37:09Z"
        ));

    @Test
    public void testCertInfo_Pass() throws Exception {
        initTestCluster();
        final RestHelper rh = restHelper();
        rh.enableHTTPClientSSL = true;
        rh.trustHTTPServerCertificate = true;
        rh.sendAdminCertificate = true;
        rh.keystore = "kirk-keystore.jks";

        final RestHelper.HttpResponse transportInfoRestResponse = rh.executeGetRequest(ENDPOINT);
        JSONObject expectedJsonResponse = new JSONObject();
        expectedJsonResponse.appendField("http_certificates_list", NODE_CERT_DETAILS);
        expectedJsonResponse.appendField("transport_certificates_list", NODE_CERT_DETAILS);
        Assert.assertEquals(expectedJsonResponse.toString(), transportInfoRestResponse.getBody());
    }

    @Test
    public void testCertInfoFail_NonAdmin() throws Exception {
        initTestCluster();
        final RestHelper rh = restHelper();
        rh.enableHTTPClientSSL = true;
        rh.trustHTTPServerCertificate = true;
        rh.sendAdminCertificate = true;
        rh.keystore = "spock-keystore.jks";

        final RestHelper.HttpResponse transportInfoRestResponse = rh.executeGetRequest(ENDPOINT);
        Assert.assertEquals(401, transportInfoRestResponse.getStatusCode()); // Forbidden for non-admin
        Assert.assertEquals("Unauthorized", transportInfoRestResponse.getStatusReason());
    }

    /**
     * Helper method to initialize test cluster for CertInfoAction Tests
     */
    private void initTestCluster() throws Exception {
        final Settings settings = Settings.builder()
            .put(SSLConfigConstants.SECURITY_SSL_TRANSPORT_ENABLED, true)
            .put(SSLConfigConstants.SECURITY_SSL_TRANSPORT_PEMCERT_FILEPATH, FileHelper. getAbsoluteFilePathFromClassPath("ssl/node-0.crt.pem"))
            .put(SSLConfigConstants.SECURITY_SSL_TRANSPORT_PEMKEY_FILEPATH, FileHelper. getAbsoluteFilePathFromClassPath("ssl/node-0.key.pem"))
            .put(SSLConfigConstants.SECURITY_SSL_TRANSPORT_PEMTRUSTEDCAS_FILEPATH, FileHelper. getAbsoluteFilePathFromClassPath("ssl/root-ca.pem"))
            .put(SSLConfigConstants.SECURITY_SSL_TRANSPORT_ENFORCE_HOSTNAME_VERIFICATION, false)
            .put(SSLConfigConstants.SECURITY_SSL_TRANSPORT_ENFORCE_HOSTNAME_VERIFICATION_RESOLVE_HOST_NAME, false)
            .put(SSLConfigConstants.SECURITY_SSL_HTTP_ENABLED, true)
            .put(SSLConfigConstants.SECURITY_SSL_HTTP_PEMCERT_FILEPATH, FileHelper.getAbsoluteFilePathFromClassPath("ssl/node-0.crt.pem"))
            .put(SSLConfigConstants.SECURITY_SSL_HTTP_PEMKEY_FILEPATH, FileHelper.getAbsoluteFilePathFromClassPath("ssl/node-0.key.pem"))
            .put(SSLConfigConstants.SECURITY_SSL_HTTP_PEMTRUSTEDCAS_FILEPATH, FileHelper.getAbsoluteFilePathFromClassPath("ssl/root-ca.pem"))
            .put(ConfigConstants.SECURITY_SSL_CERT_RELOAD_ENABLED, true)
            .build();
        setup(settings);
    }
}
