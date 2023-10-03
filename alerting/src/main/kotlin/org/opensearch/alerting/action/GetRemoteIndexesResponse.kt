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
    var clusters: List<ClusterInfo> = emptyList()

    constructor(clusters: List<ClusterInfo>) : super() {
        this.clusters = clusters
    }

    @Throws(IOException::class)
    constructor(sin: StreamInput) : this(
        clusters = sin.readList((ClusterInfo.Companion)::readFrom)
    )

    override fun toXContent(builder: XContentBuilder, params: ToXContent.Params): XContentBuilder {
        builder.startObject()
            .startArray(CLUSTERS_FIELD)
        clusters.forEach { it.toXContent(builder, params) }
        return builder.endArray().endObject()
    }

    override fun writeTo(out: StreamOutput) {
        clusters.forEach { it.writeTo(out) }
    }

    companion object {
        const val CLUSTERS_FIELD = "clusters"
    }

    data class ClusterInfo(
        val clusterName: String,
        val clusterHealth: ClusterHealthStatus,
//        val connected: Boolean,
        val hubCluster: Boolean,
        val indexes: List<IndexInfo> = listOf(),
        val latency: Long
    ) : ToXContentObject, Writeable {

        @Throws(IOException::class)
        constructor(sin: StreamInput) : this(
            clusterName = sin.readString(),
            clusterHealth = sin.readEnum(ClusterHealthStatus::class.java),
//            connected = sin.readBoolean(),
            hubCluster = sin.readBoolean(),
            indexes = sin.readList((IndexInfo.Companion)::readFrom),
            latency = sin.readLong()
        )

        override fun toXContent(builder: XContentBuilder, params: ToXContent.Params): XContentBuilder {
            builder.startObject()
                .field(CLUSTER_NAME_FIELD, clusterName)
                .field(CLUSTER_HEALTH_FIELD, clusterHealth)
//                .field(CONNECTED_FIELD, connected)
                .field(HUB_CLUSTER_FIELD, hubCluster)
                .field(INDEX_LATENCY_FIELD, latency)
                .startArray(INDEXES_FIELD)
            indexes.forEach { it.toXContent(builder, params) }
            return builder.endArray().endObject()
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
            const val CONNECTED_FIELD = "connected"
            const val HUB_CLUSTER_FIELD = "hub_cluster"
            const val INDEXES_FIELD = "indexes"
            const val INDEX_LATENCY_FIELD = "latency"

            @JvmStatic
            @Throws(IOException::class)
            fun readFrom(sin: StreamInput): ClusterInfo {
                return ClusterInfo(sin)
            }
        }

        data class IndexInfo(
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
                fun readFrom(sin: StreamInput): IndexInfo {
                    return IndexInfo(sin)
                }
            }
        }
    }
}
