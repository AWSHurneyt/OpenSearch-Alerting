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

import org.apache.commons.validator.routines.UrlValidator
import org.apache.http.client.utils.URIBuilder
import org.apache.logging.log4j.LogManager
import org.opensearch.common.CheckedFunction
import org.opensearch.common.ParseField
import org.opensearch.common.io.stream.StreamOutput
import org.opensearch.common.xcontent.NamedXContentRegistry
import org.opensearch.common.xcontent.ToXContent
import org.opensearch.common.xcontent.XContentBuilder
import org.opensearch.common.xcontent.XContentParser
import org.opensearch.common.xcontent.XContentParserUtils
import java.io.IOException
import java.net.URI

val ILLEGAL_PATH_PARAMETER_CHARACTERS = arrayOf('=', '?', '"', ' ')

/**
 * This is a data class for a URI type of input for Monitors specifically for local clusters.
 */
data class LocalUriInput(
    var path: String,
    var pathParams: String = "",
    var url: String,
    val connectionTimeout: Int,
    val socketTimeout: Int
) : Input {
    val apiType: ApiType
    val constructedUri: URI
    private val logger = LogManager.getLogger(LocalUriInput::class.java) // TODO hurneyt

    // Verify parameters are valid during creation
    init {
        // TODO hurneyt
        logger.info("fields PRE \npath = $path \npathParams = $pathParams \nurl = $url")
        logger.info("this = $this")

        require(validateFields()) {
            "The uri.api_type field, uri.path field, or uri.uri field must be defined."
        }
        require(connectionTimeout in MIN_CONNECTION_TIMEOUT..MAX_CONNECTION_TIMEOUT) {
            "Connection timeout: $connectionTimeout is not in the range of $MIN_CONNECTION_TIMEOUT - $MAX_CONNECTION_TIMEOUT."
        }
        require(socketTimeout in MIN_SOCKET_TIMEOUT..MAX_SOCKET_TIMEOUT) {
            "Socket timeout: $socketTimeout is not in the range of $MIN_SOCKET_TIMEOUT - $MAX_SOCKET_TIMEOUT."
        }

        // Create an UrlValidator that only accepts "http" and "https" as valid scheme and allows local URLs.
        val urlValidator = UrlValidator(arrayOf("http", "https"), UrlValidator.ALLOW_LOCAL_URLS)

        // Build url field by field if not provided as whole.
        // TODO LocalUri: Seems to be throwing a 500 internal error around here when the input includes illegal characters
        //  RE: https://amzn-aws.slack.com/archives/C02L5D8GF1S/p1636571432001300
        //  Perhaps check path, pathParams, and url against ILLEGAL_PATH_PARAMETER_CHARACTERS before continuing
        constructedUri = toConstructedUri()

        require(urlValidator.isValid(constructedUri.toString())) {
            "Invalid URL. constructedUri = $constructedUri" // TODO hurneyt
        }

        if (url.isNotEmpty() && validateFieldsNotEmpty())
            require(constructedUri == constructUrlFromInputs()) { // TODO hurneyt
                "The provided URL and URI fields form different URLs. \nconstructedUri = $constructedUri \nurlFromInputs = ${constructUrlFromInputs()}"
            }

        require(constructedUri.host.toLowerCase() == SUPPORTED_HOST) {
            "Only host '$SUPPORTED_HOST' is supported."
        }
        require(constructedUri.port == SUPPORTED_PORT) {
            "Only port '$SUPPORTED_PORT' is supported."
        }

        apiType = findApiType(constructedUri.path)
        this.parseEmptyFields()

        // TODO hurneyt
        logger.info("apiType = $apiType \nconstructedUri = $constructedUri")
        logger.info("fields POST \npath = $path \npathParams = $pathParams \nurl = $url")
        logger.info("this = $this")
    }

    override fun toXContent(builder: XContentBuilder, params: ToXContent.Params): XContentBuilder {
        return builder.startObject()
            .startObject(URI_FIELD)
            .field(API_TYPE_FIELD, apiType)
            .field(PATH_FIELD, path)
            .field(PATH_PARAMS_FIELD, pathParams)
            .field(URL_FIELD, url)
            .field(CONNECTION_TIMEOUT_FIELD, connectionTimeout)
            .field(SOCKET_TIMEOUT_FIELD, socketTimeout)
            .endObject()
            .endObject()
    }

    override fun name(): String {
        return URI_FIELD
    }

    override fun writeTo(out: StreamOutput) {
        out.writeString(apiType.toString())
        out.writeString(path)
        out.writeString(pathParams)
        out.writeString(url)
        out.writeInt(connectionTimeout)
        out.writeInt(socketTimeout)
    }

    companion object {
        const val MIN_CONNECTION_TIMEOUT = 1
        const val MAX_CONNECTION_TIMEOUT = 5
        const val MIN_SOCKET_TIMEOUT = 1
        const val MAX_SOCKET_TIMEOUT = 60

        const val SUPPORTED_SCHEME = "http"
        const val SUPPORTED_HOST = "localhost"
        const val SUPPORTED_PORT = 9200

        const val API_TYPE_FIELD = "api_type"
        const val PATH_FIELD = "path"
        const val PATH_PARAMS_FIELD = "path_params"
        const val URL_FIELD = "url"
        const val CONNECTION_TIMEOUT_FIELD = "connection_timeout"
        const val SOCKET_TIMEOUT_FIELD = "socket_timeout"
        const val URI_FIELD = "uri"

        val XCONTENT_REGISTRY = NamedXContentRegistry.Entry(Input::class.java, ParseField("uri"), CheckedFunction { parseInner(it) })

        /**
         * This parse function uses [XContentParser] to parse JSON input and store corresponding fields to create a [LocalUriInput] object
         */
        @JvmStatic @Throws(IOException::class)
        private fun parseInner(xcp: XContentParser): LocalUriInput {
            var path = ""
            var pathParams = ""
            var url = ""
            var connectionTimeout = MAX_CONNECTION_TIMEOUT
            var socketTimeout = MAX_SOCKET_TIMEOUT

            XContentParserUtils.ensureExpectedToken(XContentParser.Token.START_OBJECT, xcp.currentToken(), xcp)

            while (xcp.nextToken() != XContentParser.Token.END_OBJECT) {
                val fieldName = xcp.currentName()
                xcp.nextToken()
                when (fieldName) {
                    PATH_FIELD -> path = xcp.text()
                    PATH_PARAMS_FIELD -> pathParams = xcp.text()
                    URL_FIELD -> url = xcp.text()
                    CONNECTION_TIMEOUT_FIELD -> connectionTimeout = xcp.intValue()
                    SOCKET_TIMEOUT_FIELD -> socketTimeout = xcp.intValue()
                }
            }
            return LocalUriInput(path, pathParams, url, connectionTimeout, socketTimeout)
        }
    }

    /**
     * Constructs the [URI] using either the provided [url], or the
     * supported scheme, host, and port and provided [path]+[pathParams].
     * @return The [URI] constructed from [url] if it's defined;
     * otherwise a [URI] constructed from the provided [URI] fields.
     */
    private fun toConstructedUri(): URI {
        return if (url.isEmpty()) {
            constructUrlFromInputs()
        } else {
            URIBuilder(url).build()
        }
    }

    /**
     * Isolates just the path parameters from the [LocalUriInput] URI.
     * @return The path parameters portion of the [LocalUriInput] URI.
     * @throws IllegalArgumentException if the [ApiType] requires path parameters, but none are supplied;
     * or when path parameters are provided for an [ApiType] that does not use path parameters.
     */
    fun parsePathParams(): String {
        val path = this.constructedUri.path
        val apiType = this.apiType
        logger.info("hurneyt parsePathParams::path = $path")
        logger.info("hurneyt parsePathParams::apiType = $apiType")

        var pathParams: String
        if (this.pathParams.isNotEmpty()) {
            pathParams = this.pathParams
        } else {
            val prependPath = if (apiType.supportsPathParams) apiType.prependPath else apiType.defaultPath
            pathParams = path.removePrefix(prependPath)
            pathParams = pathParams.removeSuffix(apiType.appendPath)
        }

        if (pathParams.isNotEmpty()) {
            pathParams = pathParams.trim('/')
            ILLEGAL_PATH_PARAMETER_CHARACTERS.forEach { character ->
                if (pathParams.contains(character))
                    throw IllegalArgumentException("The provided path parameters contain invalid characters or spaces. Please omit: ${ILLEGAL_PATH_PARAMETER_CHARACTERS.joinToString(" ")}")
            }
        }

        if (apiType.requiresPathParams && pathParams.isEmpty())
            throw IllegalArgumentException("The API requires path parameters. \napiType = $apiType \npathParams = $pathParams") // TODO hurneyt
        if (!apiType.supportsPathParams && pathParams.isNotEmpty())
            throw IllegalArgumentException("The API does not use path parameters. hurneyt \napiType = $apiType \npathParams = $pathParams") // TODO hurneyt

        return pathParams
    }

    /**
     * Examines the path of a [LocalUriInput] to determine which API is being called.
     * @param uriPath The path to examine.
     * @return The [ApiType] associated with the [LocalUriInput] monitor.
     * @throws IllegalArgumentException when the API to call cannot be determined from the URI.
     */
    private fun findApiType(uriPath: String): ApiType {
        var apiType = ApiType.BLANK
        ApiType.values()
            .filter { option -> option != ApiType.BLANK }
            .forEach { option ->
                if (uriPath.startsWith(option.prependPath) || uriPath.startsWith(option.defaultPath))
                    apiType = option
            }
        if (apiType.isBlank())
            throw IllegalArgumentException("The API could not be determined from the provided URI.")
        return apiType
    }

    /**
     * Constructs a [URI] from the supported scheme, host, and port, and the provided [path], and [pathParams].
     * @return The constructed [URI].
     */
    private fun constructUrlFromInputs(): URI {
        val uriBuilder = URIBuilder()
            .setScheme(SUPPORTED_SCHEME)
            .setHost(SUPPORTED_HOST)
            .setPort(SUPPORTED_PORT)
            .setPath(path + pathParams)
        return uriBuilder.build()
    }

    /**
     * If [url] field is empty, populates it with [constructedUri].
     * If [path] and [pathParams] are empty, populates them with values from [url].
     */
    private fun parseEmptyFields() {
        if (pathParams.isEmpty())
            pathParams = this.parsePathParams()
        if (path.isEmpty())
            path = if (pathParams.isEmpty()) apiType.defaultPath else apiType.prependPath
        if (url.isEmpty())
            url = constructedUri.toString()
    }

    /**
     * Helper function to confirm at least [url], or required URI component fields are defined.
     * @return TRUE if at least either [url] or the other components are provided; otherwise FALSE.
     */
    private fun validateFields(): Boolean {
        return url.isNotEmpty() || validateFieldsNotEmpty()
    }

    /**
     * Confirms that required URI component fields are defined.
     * Only validating path for now, as that's the only required field.
     * @return TRUE if all those fields are defined; otherwise FALSE.
     */
    private fun validateFieldsNotEmpty(): Boolean {
        return path.isNotEmpty()
    }

    /**
     * An enum class to quickly reference various supported API.
     */
    enum class ApiType(
        val defaultPath: String,
        val prependPath: String,
        val appendPath: String,
        val supportsPathParams: Boolean,
        val requiresPathParams: Boolean
    ) {
        BLANK("", "", "", false, false),
        CAT_PENDING_TASKS(
            "/_cat/pending_tasks",
            "/_cat/pending_tasks",
            "",
            false,
            false
        ),
        CAT_RECOVERY(
            "/_cat/recovery",
            "/_cat/recovery",
            "",
            true,
            false
        ),
        CAT_REPOSITORIES(
            "/_cat/repositories",
            "/_cat/repositories",
            "",
            false,
            false
        ),
        CAT_SNAPSHOTS(
            "/_cat/snapshots",
            "/_cat/snapshots",
            "",
            true,
            true
        ),
        CAT_TASKS(
            "/_cat/tasks",
            "/_cat/tasks",
            "",
            false,
            false
        ),
        CLUSTER_HEALTH(
            "/_cluster/health",
            "/_cluster/health",
            "",
            true,
            false
        ),
        CLUSTER_SETTINGS(
            "/_cluster/settings",
            "/_cluster/settings",
            "",
            false,
            false
        ),
        CLUSTER_STATS(
            "/_cluster/stats",
            "/_cluster/stats",
            "",
            true,
            false
        ),
        NODES_STATS(
            "/_nodes/stats",
            "/_nodes",
            "",
            false,
            false
        );

        /**
         * @return TRUE if the [ApiType] is [BLANK]; otherwise FALSE.
         */
        fun isBlank(): Boolean {
            return this === BLANK
        }
    }
}
