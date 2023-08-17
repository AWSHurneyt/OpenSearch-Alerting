/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.alerting.action

import org.apache.logging.log4j.LogManager
import org.opensearch.action.ActionRequest
import org.opensearch.action.ActionRequestValidationException
import org.opensearch.action.ValidateActions
import org.opensearch.alerting.util.IndexUtils.Companion.VALID_INDEX_NAME_REGEX
import org.opensearch.common.io.stream.StreamInput
import org.opensearch.common.io.stream.StreamOutput
import org.opensearch.common.io.stream.Writeable
import org.opensearch.common.xcontent.XContentParserUtils.ensureExpectedToken
import org.opensearch.core.xcontent.ToXContent
import org.opensearch.core.xcontent.ToXContentObject
import org.opensearch.core.xcontent.XContentBuilder
import org.opensearch.core.xcontent.XContentParser
import java.io.IOException

private val log = LogManager.getLogger(GetRemoteIndexesLatencyRequest::class.java)

class GetRemoteIndexesLatencyRequest : ActionRequest {
    val indexes: List<RemoteIndex>

    constructor(indexes: List<RemoteIndex>) {
        this.indexes = indexes
    }

    @Throws(IOException::class)
    constructor(sin: StreamInput) : this(
        indexes = sin.readList((RemoteIndex.Companion)::readFrom)
    )

    override fun validate(): ActionRequestValidationException? {
        var validationException: ActionRequestValidationException? = null

        if (indexes.isEmpty()) {
            validationException = ValidateActions.addValidationError(
                "Request contains no indexes.", validationException
            )
        }

        indexes.any {
            var error = validateClusterAlias(it.clusterAlias)
            if (!error.isNullOrEmpty()) {
                validationException = ValidateActions.addValidationError(
                    error, validationException
                )
            } else {
                error = validateIndexNames(it.indexes)
                if (!error.isNullOrEmpty()) {
                    validationException = ValidateActions.addValidationError(
                        error, validationException
                    )
                }
            }
            return@any error.isNullOrEmpty()
        }
        return validationException
    }

    private fun validateClusterAlias(clusterAlias: String): String? {
        // TODO hurneyt: need to confirm character limitations for cluster names. The following opensearch resources don't list them
        //  https://opensearch.org/docs/latest/tuning-your-cluster/index/#step-1-name-a-cluster
        //  https://docs.aws.amazon.com/opensearch-service/latest/developerguide/createupdatedomains.html#:~:text=For-,Domain%20name,-%2C%20enter%20a%20domain
        val isValid = true
        return if (isValid) null else "Request contains an invalid cluster alias."
    }

    private fun validateIndexNames(indexes: List<String>): String? {
        if (indexes.isEmpty()) return "Request contains an empty list of indexes."
        val isValid = indexes.any { !VALID_INDEX_NAME_REGEX.containsMatchIn(it) }
        return if (isValid) null else "Request contains an invalid index name."
    }

    companion object {
        const val CLUSTERS_FIELD = "clusters"

        @JvmStatic
        @JvmOverloads
        @Throws(IOException::class)
        fun parse(xcp: XContentParser): GetRemoteIndexesLatencyRequest {
            log.info("hurneyt GetRemoteIndexesLatencyRequest::parse START")
            val remoteIndexes = mutableListOf<RemoteIndex>()

            log.info("hurneyt GetRemoteIndexesLatencyRequest::parse xcp == null = ${xcp == null}")
            log.info("hurneyt GetRemoteIndexesLatencyRequest::parse xcp.map.keys = ${xcp?.map()?.keys?.joinToString(", ")}")
            log.info("hurneyt GetRemoteIndexesLatencyRequest::parse xcp.currentToken() = ${xcp?.currentToken()?.name}")

            ensureExpectedToken(XContentParser.Token.START_OBJECT, xcp.currentToken(), xcp)
            while (xcp.nextToken() != XContentParser.Token.END_OBJECT) {
                val fieldName = xcp.currentName()
                log.info("hurneyt GetRemoteIndexesLatencyRequest::parse fieldName = $fieldName")
                xcp.nextToken()

                when (fieldName) {
                    CLUSTERS_FIELD -> {
                        ensureExpectedToken(XContentParser.Token.START_ARRAY, xcp.currentToken(), xcp)
                        while (xcp.nextToken() != XContentParser.Token.END_ARRAY) {
                            remoteIndexes.add(RemoteIndex.parse(xcp))
                        }
                    }
                }
            }

            require(!remoteIndexes.isNullOrEmpty()) { "Cluster and indexes list cannot be null or empty." }

            log.info("hurneyt GetRemoteIndexesLatencyRequest::parse END")
            return GetRemoteIndexesLatencyRequest(remoteIndexes)
        }
    }

    data class RemoteIndex(
        val clusterAlias: String,
        val indexes: List<String>
    ) : ToXContentObject, Writeable {

        @Throws(IOException::class)
        constructor(sin: StreamInput) : this(
            clusterAlias = sin.readString(),
            indexes = sin.readStringList()
        )

        override fun toXContent(builder: XContentBuilder, params: ToXContent.Params): XContentBuilder {
            return builder.startObject()
                .field(CLUSTER_ALIAS_FIELD, clusterAlias)
                .array(INDEXES_FIELD, indexes)
                .endObject()
        }

        override fun writeTo(out: StreamOutput) {
            out.writeString(clusterAlias)
            out.writeStringArray(indexes.toTypedArray())
        }

        companion object {
            const val CLUSTER_ALIAS_FIELD = "cluster"
            const val INDEXES_FIELD = "indexes"

            @JvmStatic
            @Throws(IOException::class)
            fun readFrom(sin: StreamInput): RemoteIndex {
                return RemoteIndex(sin)
            }

            @JvmStatic
            @JvmOverloads
            @Throws(IOException::class)
            fun parse(xcp: XContentParser): RemoteIndex {
                log.info("hurneyt RemoteIndex::parse START")
                var clusterAlias: String? = null
                val indexes = mutableListOf<String>()

                ensureExpectedToken(XContentParser.Token.START_OBJECT, xcp.currentToken(), xcp)
                while (xcp.nextToken() != XContentParser.Token.END_OBJECT) {
                    val fieldName = xcp.currentName()
                    log.info("hurneyt RemoteIndex::parse fieldName = $fieldName")
                    xcp.nextToken()

                    when (fieldName) {
                        CLUSTER_ALIAS_FIELD -> clusterAlias = xcp.text()
                        INDEXES_FIELD -> {
                            ensureExpectedToken(XContentParser.Token.START_ARRAY, xcp.currentToken(), xcp)
                            while (xcp.nextToken() != XContentParser.Token.END_ARRAY) {
                                indexes.add(xcp.text())
                            }
                        }
                    }
                }

                requireNotNull(clusterAlias) { "Cluster alias cannot be null." }
                require(!indexes.isNullOrEmpty()) { "Indexes cannot be null or empty." }

                log.info("hurneyt RemoteIndex::parse END")
                return RemoteIndex(clusterAlias, indexes)
            }
        }
    }
}
