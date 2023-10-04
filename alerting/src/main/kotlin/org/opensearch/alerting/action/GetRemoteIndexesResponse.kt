/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.alerting.action

import org.apache.logging.log4j.LogManager
import org.opensearch.action.ActionResponse
import org.opensearch.cluster.health.ClusterHealthStatus
import org.opensearch.cluster.metadata.MappingMetadata
import org.opensearch.common.io.stream.StreamInput
import org.opensearch.common.io.stream.StreamOutput
import org.opensearch.common.io.stream.Writeable
import org.opensearch.core.xcontent.ToXContent
import org.opensearch.core.xcontent.ToXContentObject
import org.opensearch.core.xcontent.XContentBuilder
import java.io.IOException

private val log = LogManager.getLogger(GetRemoteIndexesResponse::class.java)

class GetRemoteIndexesResponse : ActionResponse, ToXContentObject {
    var clusterIndexes: List<ClusterIndexes> = emptyList()

    constructor(clusterIndexes: List<ClusterIndexes>) : super() {
        this.clusterIndexes = clusterIndexes
    }

    @Throws(IOException::class)
    constructor(sin: StreamInput) : this(
        clusterIndexes = sin.readList((ClusterIndexes.Companion)::readFrom)
    )

    override fun toXContent(builder: XContentBuilder, params: ToXContent.Params): XContentBuilder {
        builder.startObject()
        clusterIndexes.forEach {
            builder.startObject(it.clusterName)
            it.toXContent(builder, params)
            builder.endObject()
        }
        return builder.endObject()
    }

    override fun writeTo(out: StreamOutput) {
        clusterIndexes.forEach { it.writeTo(out) }
    }

    data class ClusterIndexes(
        val clusterName: String,
        val clusterHealth: ClusterHealthStatus,
        val hubCluster: Boolean,
        val indexes: List<ClusterIndex> = listOf(),
        val latency: Long
    ) : ToXContentObject, Writeable {

        @Throws(IOException::class)
        constructor(sin: StreamInput) : this(
            clusterName = sin.readString(),
            clusterHealth = sin.readEnum(ClusterHealthStatus::class.java),
            hubCluster = sin.readBoolean(),
            indexes = sin.readList((ClusterIndex.Companion)::readFrom),
            latency = sin.readLong()
        )

        override fun toXContent(builder: XContentBuilder, params: ToXContent.Params): XContentBuilder {
            builder.startObject()
                .field(CLUSTER_NAME_FIELD, clusterName)
                .field(CLUSTER_HEALTH_FIELD, clusterHealth)
                .field(HUB_CLUSTER_FIELD, hubCluster)
                .field(INDEX_LATENCY_FIELD, latency)
                .startObject(INDEXES_FIELD)
            indexes.forEach {
                builder.startObject(it.indexName)
                it.toXContent(builder, params)
                builder.endObject()
            }
            return builder.endObject().endObject()
        }

        override fun writeTo(out: StreamOutput) {
            out.writeString(clusterName)
            out.writeEnum(clusterHealth)
            indexes.forEach { it.writeTo(out) }
            out.writeLong(latency)
        }

        companion object {
            const val CLUSTER_NAME_FIELD = "cluster"
            const val CLUSTER_HEALTH_FIELD = "health"
            const val HUB_CLUSTER_FIELD = "hub_cluster"
            const val INDEXES_FIELD = "indexes"
            const val INDEX_LATENCY_FIELD = "latency"

            @JvmStatic
            @Throws(IOException::class)
            fun readFrom(sin: StreamInput): ClusterIndexes {
                return ClusterIndexes(sin)
            }
        }

        data class ClusterIndex(
            val indexName: String,
            val indexHealth: ClusterHealthStatus,
            val mappings: MappingMetadata?
        ) : ToXContentObject, Writeable {

            @Throws(IOException::class)
            constructor(sin: StreamInput) : this(
                indexName = sin.readString(),
                indexHealth = sin.readEnum(ClusterHealthStatus::class.java),
                mappings = sin.readOptionalWriteable(::MappingMetadata)
            )

            override fun toXContent(builder: XContentBuilder, params: ToXContent.Params): XContentBuilder {
                builder.startObject()
                    .field(INDEX_NAME_FIELD, indexName)
                    .field(INDEX_HEALTH_FIELD, indexHealth)
                if (mappings == null) builder.startObject(GetRemoteIndexesMappingsResponse.MAPPINGS_FIELD).endObject()
                else builder.field(MAPPINGS_FIELD, mappings.sourceAsMap())
                return builder.endObject()
            }

            override fun writeTo(out: StreamOutput) {
                out.writeString(indexName)
                out.writeEnum(indexHealth)
                if (mappings != null) out.writeMap(mappings.sourceAsMap)
            }

            companion object {
                const val INDEX_NAME_FIELD = "name"
                const val INDEX_HEALTH_FIELD = "health"
                const val MAPPINGS_FIELD = "mappings"

                @JvmStatic
                @Throws(IOException::class)
                fun readFrom(sin: StreamInput): ClusterIndex {
                    return ClusterIndex(sin)
                }
            }
        }
    }
}
