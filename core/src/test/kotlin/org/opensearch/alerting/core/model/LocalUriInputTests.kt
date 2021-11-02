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

/*
 *   Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   A copy of the License is located at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   or in the "license" file accompanying this file. This file is distributed
 *   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *   express or implied. See the License for the specific language governing
 *   permissions and limitations under the License.
 */

package org.opensearch.alerting.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LocalUriInputTests {
    private var scheme = "http"
    private var host = "localhost"
    private var port = 9200
    private var path = "/_cluster/health"
    private var pathParams = ""
    private var queryParams = hashMapOf<String, String>()
    private var url = ""
    private var connectionTimeout = 5
    private var socketTimeout = 5

    @Test
    fun `test valid LocalUriInput creation using HTTP URI component fields`() {
        // GIVEN + WHEN
        val localUriInput = LocalUriInput(scheme, host, port, path, pathParams, queryParams, url, connectionTimeout, socketTimeout)

        // THEN
        assertEquals(scheme, localUriInput.scheme)
        assertEquals(host, localUriInput.host)
        assertEquals(port, localUriInput.port)
        assertEquals(path, localUriInput.path)
        assertEquals(queryParams, localUriInput.query_params)
        assertEquals(url, localUriInput.url)
        assertEquals(connectionTimeout, localUriInput.connection_timeout)
        assertEquals(socketTimeout, localUriInput.socket_timeout)
    }

    @Test
    fun `test valid LocalUriInput creation using HTTP url field`() {
        // GIVEN
        scheme = ""
        host = ""
        port = -1
        path = ""
        url = "http://localhost:9200/_cluster/health"

        // WHEN
        val localUriInput = LocalUriInput(scheme, host, port, path, pathParams, queryParams, url, connectionTimeout, socketTimeout)

        // THEN
        assertEquals(url, localUriInput.url)
    }

    @Test
    fun `test valid LocalUriInput creation using HTTPS URI component fields`() {
        // GIVEN
        scheme = "https"

        // WHEN
        val localUriInput = LocalUriInput(scheme, host, port, path, pathParams, queryParams, url, connectionTimeout, socketTimeout)

        // THEN
        assertEquals(scheme, localUriInput.scheme)
        assertEquals(host, localUriInput.host)
        assertEquals(port, localUriInput.port)
        assertEquals(path, localUriInput.path)
        assertEquals(queryParams, localUriInput.query_params)
        assertEquals(url, localUriInput.url)
        assertEquals(connectionTimeout, localUriInput.connection_timeout)
        assertEquals(socketTimeout, localUriInput.socket_timeout)
    }

    @Test
    fun `test valid LocalUriInput creation using HTTPS url field`() {
        // GIVEN
        scheme = ""
        host = ""
        port = -1

        // WHEN
        val localUriInput = LocalUriInput(scheme, host, port, path, pathParams, queryParams, url, connectionTimeout, socketTimeout)

        // THEN
        assertEquals(url, localUriInput.url)
    }

    @Test
    fun `test valid LocalUriInput creation with path, but empty scheme, host, and port fields`() {
        // GIVEN
        scheme = ""
        host = ""
        port = -1

        // WHEN
        val localUriInput = LocalUriInput(scheme, host, port, path, pathParams, queryParams, url, connectionTimeout, socketTimeout)

        // THEN
        assertEquals(path, localUriInput.path)
        assertEquals(localUriInput.toConstructedUri().toString(), "http://localhost:9200/_cluster/health")
    }

    @Test
    fun `test invalid scheme`() {
        // GIVEN
        scheme = "invalidScheme"

        // WHEN + THEN
        assertFailsWith<IllegalArgumentException>("Invalid URL.") {
            LocalUriInput(scheme, host, port, path, pathParams, queryParams, url, connectionTimeout, socketTimeout)
        }
    }

    @Test
    fun `test invalid host`() {
        // GIVEN
        host = "loco//host"

        // WHEN + THEN
        assertFailsWith<IllegalArgumentException>("Invalid URL.") {
            LocalUriInput(scheme, host, port, path, pathParams, queryParams, url, connectionTimeout, socketTimeout)
        }
    }

    @Test
    fun `test invalid host is not localhost`() {
        // GIVEN
        host = "127.0.0.1"

        // WHEN + THEN
        assertFailsWith<IllegalArgumentException>(
            "Only host '${LocalUriInput.SUPPORTED_HOST}' is supported."
        ) {
            LocalUriInput(scheme, host, port, path, pathParams, queryParams, url, connectionTimeout, socketTimeout)
        }
    }

    @Test
    fun `test invalid path`() {
        // GIVEN
        path = "///"

        // WHEN + THEN
        assertFailsWith<IllegalArgumentException>("Invalid URL.") {
            LocalUriInput(scheme, host, port, path, pathParams, queryParams, url, connectionTimeout, socketTimeout)
        }
    }

    @Test
    fun `test invalid port`() {
        // GIVEN
        port = LocalUriInput.SUPPORTED_PORT + 1

        // WHEN + THEN
        assertFailsWith<IllegalArgumentException>(
            "Only port '${LocalUriInput.SUPPORTED_PORT}' is supported."
        ) {
            LocalUriInput(scheme, host, port, path, pathParams, queryParams, url, connectionTimeout, socketTimeout)
        }
    }

    @Test
    fun `test invalid connection timeout that's too low`() {
        // GIVEN
        connectionTimeout = LocalUriInput.MIN_CONNECTION_TIMEOUT - 1

        // WHEN + THEN
        assertFailsWith<IllegalArgumentException>(
            "Connection timeout: $connectionTimeout is not in the range of ${LocalUriInput.MIN_CONNECTION_TIMEOUT} - ${LocalUriInput.MIN_CONNECTION_TIMEOUT}."
        ) {
            LocalUriInput(scheme, host, port, path, pathParams, queryParams, url, connectionTimeout, socketTimeout)
        }
    }

    @Test
    fun `test invalid connection timeout that's too high`() {
        // GIVEN
        connectionTimeout = LocalUriInput.MAX_CONNECTION_TIMEOUT + 1

        // WHEN + THEN
        assertFailsWith<IllegalArgumentException>(
            "Connection timeout: $connectionTimeout is not in the range of ${LocalUriInput.MIN_CONNECTION_TIMEOUT} - ${LocalUriInput.MIN_CONNECTION_TIMEOUT}."
        ) {
            LocalUriInput(scheme, host, port, path, pathParams, queryParams, url, connectionTimeout, socketTimeout)
        }
    }

    @Test
    fun `test invalid socket timeout that's too low`() {
        // GIVEN
        socketTimeout = LocalUriInput.MIN_SOCKET_TIMEOUT - 1

        // WHEN + THEN
        assertFailsWith<IllegalArgumentException>(
            "Socket timeout: $socketTimeout is not in the range of ${LocalUriInput.MIN_SOCKET_TIMEOUT} - ${LocalUriInput.MAX_SOCKET_TIMEOUT}."
        ) {
            LocalUriInput(scheme, host, port, path, pathParams, queryParams, url, connectionTimeout, socketTimeout)
        }
    }

    @Test
    fun `test invalid socket timeout that's too high`() {
        // GIVEN
        socketTimeout = LocalUriInput.MAX_SOCKET_TIMEOUT + 1

        // WHEN + THEN
        assertFailsWith<IllegalArgumentException>(
            "Socket timeout: $socketTimeout is not in the range of ${LocalUriInput.MIN_SOCKET_TIMEOUT} - ${LocalUriInput.MAX_SOCKET_TIMEOUT}."
        ) {
            LocalUriInput(scheme, host, port, path, pathParams, queryParams, url, connectionTimeout, socketTimeout)
        }
    }

    @Test
    fun `test invalid url`() {
        // GIVEN
        url = "///"

        // WHEN + THEN
        assertFailsWith<IllegalArgumentException>("Invalid URL.") {
            LocalUriInput(scheme, host, port, path, pathParams, queryParams, url, connectionTimeout, socketTimeout)
        }
    }

    @Test
    fun `test url field and URI component fields create equal URI`() {
        // GIVEN
        url = "http://localhost:9200/_cluster/health"

        // WHEN
        val localUriInput = LocalUriInput(scheme, host, port, path, pathParams, queryParams, url, connectionTimeout, socketTimeout)

        // THEN
        assertEquals(scheme, localUriInput.scheme)
        assertEquals(host, localUriInput.host)
        assertEquals(port, localUriInput.port)
        assertEquals(path, localUriInput.path)
        assertEquals(pathParams, localUriInput.path_params)
        assertEquals(queryParams, localUriInput.query_params)
        assertEquals(url, localUriInput.url)
        assertEquals(connectionTimeout, localUriInput.connection_timeout)
        assertEquals(socketTimeout, localUriInput.socket_timeout)
        assertEquals(url, localUriInput.constructedUri.toString())
    }

    @Test
    fun `test url field and URI component fields create different URI`() {
        // GIVEN
        url = "http://localhost:9200/_cluster/stats"

        // WHEN + THEN
        assertFailsWith<IllegalArgumentException>("The provided URL and URI fields form different URLs.") {
            LocalUriInput(scheme, host, port, path, pathParams, queryParams, url, connectionTimeout, socketTimeout)
        }
    }

    @Test
    fun `test LocalUriInput creation when all inputs are empty`() {
        // GIVEN
        scheme = ""
        host = ""
        port = -1
        path = ""
        url = ""

        // WHEN + THEN
        assertFailsWith<IllegalArgumentException>("Either the url field, or scheme + host + port + path + params must be set.") {
            LocalUriInput(scheme, host, port, path, pathParams, queryParams, url, connectionTimeout, socketTimeout)
        }
    }

    @Test
    fun `test invalid host in url field`() {
        // GIVEN
        scheme = ""
        host = ""
        port = -1
        path = ""
        url = "http://127.0.0.1:9200/_cluster/health"

        // WHEN + THEN
        assertFailsWith<IllegalArgumentException>("Only host '${LocalUriInput.SUPPORTED_HOST}' is supported.") {
            LocalUriInput(scheme, host, port, path, pathParams, queryParams, url, connectionTimeout, socketTimeout)
        }
    }

    @Test
    fun `test invalid port in url field`() {
        // GIVEN
        scheme = ""
        host = ""
        port = -1
        path = ""
        url = "http://localhost:${LocalUriInput.SUPPORTED_PORT + 1}/_cluster/health"

        // WHEN + THEN
        assertFailsWith<IllegalArgumentException>("Only port '${LocalUriInput.SUPPORTED_PORT}' is supported.") {
            LocalUriInput(scheme, host, port, path, pathParams, queryParams, url, connectionTimeout, socketTimeout)
        }
    }

    @Test
    fun `test getPathParams with no path params`() {
        // GIVEN
        val testUrl = "http://localhost:9200/_cluster/health"
        val localUriInput = LocalUriInput(scheme, host, port, path, pathParams, queryParams, url, connectionTimeout, socketTimeout)

        // WHEN
        val params = localUriInput.getPathParams()

        // THEN
        assertEquals(pathParams, params)
        assertEquals(testUrl, localUriInput.constructedUri.toString())
    }

    @Test
    fun `test getPathParams with path params as URI field`() {
        // GIVEN
        path = "/_cluster/health/"
        pathParams = "index1,index2,index3,index4,index5"
        val testUrl = "http://localhost:9200/_cluster/health/index1,index2,index3,index4,index5"
        val localUriInput = LocalUriInput(scheme, host, port, path, pathParams, queryParams, url, connectionTimeout, socketTimeout)

        // WHEN
        val params = localUriInput.getPathParams()

        // THEN
        assertEquals(pathParams, params)
        assertEquals(testUrl, localUriInput.constructedUri.toString())
    }

    @Test
    fun `test getPathParams with path params in url`() {
        // GIVEN
        scheme = ""
        host = ""
        port = -1
        path = ""
        val testParams = "index1,index2,index3,index4,index5"
        url = "http://localhost:9200/_cluster/health/index1,index2,index3,index4,index5"
        val localUriInput = LocalUriInput(scheme, host, port, path, pathParams, queryParams, url, connectionTimeout, socketTimeout)

        // WHEN
        val params = localUriInput.getPathParams()

        // THEN
        assertEquals(testParams, params)
        assertEquals(url, localUriInput.constructedUri.toString())
    }

    @Test
    fun `test getPathParams with no path params for ApiType that requires path params`() {
        // GIVEN
        path = "/_cat/snapshots"
        val localUriInput = LocalUriInput(scheme, host, port, path, pathParams, queryParams, url, connectionTimeout, socketTimeout)

        // WHEN + THEN
        assertFailsWith<IllegalArgumentException>("The API requires path parameters.") {
            localUriInput.getPathParams()
        }
    }

    @Test
    fun `test getPathParams with path params for ApiType that doesn't support path params`() {
        // GIVEN
        path = "/_cluster/settings"
        pathParams = "index1,index2,index3,index4,index5"
        val localUriInput = LocalUriInput(scheme, host, port, path, pathParams, queryParams, url, connectionTimeout, socketTimeout)

        // WHEN + THEN
        assertFailsWith<IllegalArgumentException>("The API does not use path parameters.") {
            localUriInput.getPathParams()
        }
    }

    @Test
    fun `test getPathParams with path params containing illegal characters`() {
        var testCount = 0 // Start off with count of 1 to account for ApiType.BLANK
        ILLEGAL_PATH_PARAMETER_CHARACTERS.forEach { character ->
            // GIVEN
            pathParams = "index1,index2,$character,index4,index5"
            val localUriInput = LocalUriInput(scheme, host, port, path, pathParams, queryParams, url, connectionTimeout, socketTimeout)

            // WHEN + THEN
            assertFailsWith<IllegalArgumentException>(
                "The provided path parameters contain invalid characters or spaces. Please omit: ${ILLEGAL_PATH_PARAMETER_CHARACTERS.joinToString(" ")}"
            ) {
                localUriInput.getPathParams()
            }
            testCount++
        }
        assertEquals(ILLEGAL_PATH_PARAMETER_CHARACTERS.size, testCount)
    }

    @Test
    fun `test LocalUriInput correctly determines ApiType when path is provided as URI component`() {
        var testCount = 1 // Start off with count of 1 to account for ApiType.BLANK
        LocalUriInput.ApiType.values()
            .filter { enum -> enum != LocalUriInput.ApiType.BLANK }
            .forEach { testApiType ->
                // GIVEN
                path = testApiType.defaultPath

                // WHEN
                val localUriInput = LocalUriInput(scheme, host, port, path, pathParams, queryParams, url, connectionTimeout, socketTimeout)

                // THEN
                assertEquals(testApiType, localUriInput.apiType)
                testCount++
            }
        assertEquals(LocalUriInput.ApiType.values().size, testCount)
    }

    @Test
    fun `test LocalUriInput correctly determines ApiType when path and path params are provided as URI components`() {
        var testCount = 1 // Start off with count of 1 to account for ApiType.BLANK
        LocalUriInput.ApiType.values()
            .filter { enum -> enum != LocalUriInput.ApiType.BLANK }
            .forEach { testApiType ->
                // GIVEN
                path = testApiType.defaultPath
                pathParams = "index1,index2,index3,index4,index5"

                // WHEN
                val localUriInput = LocalUriInput(scheme, host, port, path, pathParams, queryParams, url, connectionTimeout, socketTimeout)

                // THEN
                assertEquals(testApiType, localUriInput.apiType)
                testCount++
            }
        assertEquals(LocalUriInput.ApiType.values().size, testCount)
    }

    @Test
    fun `test LocalUriInput correctly determines ApiType when path is provided in URL field`() {
        var testCount = 1 // Start off with count of 1 to account for ApiType.BLANK
        LocalUriInput.ApiType.values()
            .filter { enum -> enum != LocalUriInput.ApiType.BLANK }
            .forEach { testApiType ->
                // GIVEN
                scheme = ""
                host = ""
                port = -1
                path = ""
                url = "http://localhost:9200${testApiType.defaultPath}"

                // WHEN
                val localUriInput = LocalUriInput(scheme, host, port, path, pathParams, queryParams, url, connectionTimeout, socketTimeout)

                // THEN
                assertEquals(testApiType, localUriInput.apiType)
                testCount++
            }
        assertEquals(LocalUriInput.ApiType.values().size, testCount)
    }

    @Test
    fun `test LocalUriInput correctly determines ApiType when path and path params are provided in URL field`() {
        var testCount = 1 // Start off with count of 1 to account for ApiType.BLANK
        LocalUriInput.ApiType.values()
            .filter { enum -> enum != LocalUriInput.ApiType.BLANK }
            .forEach { testApiType ->
                // GIVEN
                scheme = ""
                host = ""
                port = -1
                path = ""
                url = "http://localhost:9200${testApiType.defaultPath}/index1,index2,index3,index4,index5"

                // WHEN
                val localUriInput = LocalUriInput(scheme, host, port, path, pathParams, queryParams, url, connectionTimeout, socketTimeout)

                // THEN
                assertEquals(testApiType, localUriInput.apiType)
                testCount++
            }
        assertEquals(LocalUriInput.ApiType.values().size, testCount)
    }

    @Test
    fun `test LocalUriInput cannot determine ApiType when invalid path is provided as URI component`() {
        // GIVEN
        path = "/_cat/paws"

        // WHEN + THEN
        assertFailsWith<IllegalArgumentException>("The API could not be determined from the provided URI.") {
            LocalUriInput(scheme, host, port, path, pathParams, queryParams, url, connectionTimeout, socketTimeout)
        }
    }

    @Test
    fun `test LocalUriInput cannot determine ApiType when invalid path and path params are provided as URI components`() {
        // GIVEN
        path = "/_cat/paws"
        pathParams = "index1,index2,index3,index4,index5"

        // WHEN + THEN
        assertFailsWith<IllegalArgumentException>("The API could not be determined from the provided URI.") {
            LocalUriInput(scheme, host, port, path, pathParams, queryParams, url, connectionTimeout, socketTimeout)
        }
    }

    @Test
    fun `test LocalUriInput cannot determine ApiType when invaid path is provided in URL`() {
        // GIVEN
        scheme = ""
        host = ""
        port = -1
        path = ""
        url = "http://localhost:9200/_cat/paws"

        // WHEN + THEN
        assertFailsWith<IllegalArgumentException>("The API could not be determined from the provided URI.") {
            LocalUriInput(scheme, host, port, path, pathParams, queryParams, url, connectionTimeout, socketTimeout)
        }
    }

    @Test
    fun `test LocalUriInput cannot determine ApiType when invaid path and path params are provided in URL`() {
        // GIVEN
        scheme = ""
        host = ""
        port = -1
        path = ""
        url = "http://localhost:9200/_cat/paws/index1,index2,index3,index4,index5"

        // WHEN + THEN
        assertFailsWith<IllegalArgumentException>("The API could not be determined from the provided URI.") {
            LocalUriInput(scheme, host, port, path, pathParams, queryParams, url, connectionTimeout, socketTimeout)
        }
    }
}
