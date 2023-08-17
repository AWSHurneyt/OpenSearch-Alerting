/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.alerting.action

import org.opensearch.action.ActionResponse
import org.opensearch.cluster.health.ClusterHealthStatus
import org.opensearch.common.io.stream.StreamInput
import org.opensearch.common.io.stream.StreamOutput
import org.opensearch.common.io.stream.Writeable
import org.opensearch.core.xcontent.ToXContent
import org.opensearch.core.xcontent.ToXContentObject
import org.opensearch.core.xcontent.XContentBuilder
import java.io.IOException

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
        val clusterAlias: String,
        val indexes: List<IndexInfo> = listOf(),
        val latency: Long
    ) : ToXContentObject, Writeable {

        @Throws(IOException::class)
        constructor(sin: StreamInput) : this(
            clusterAlias = sin.readString(),
            indexes = sin.readList((IndexInfo.Companion)::readFrom),
            latency = sin.readLong()
        )

        override fun toXContent(builder: XContentBuilder, params: ToXContent.Params): XContentBuilder {
            builder.startObject()
                .field(CLUSTER_ALIAS_FIELD, clusterAlias)
                .field(INDEX_LATENCY_FIELD, latency)
                .startArray(INDEXES_FIELD)
            indexes.forEach { it.toXContent(builder, params) }
            return builder.endArray().endObject()
        }

        override fun writeTo(out: StreamOutput) {
            out.writeString(clusterAlias)
            indexes.forEach { it.writeTo(out) }
            out.writeLong(latency)
        }

        companion object {
            const val CLUSTER_ALIAS_FIELD = "cluster"
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
            val indexHealth: ClusterHealthStatus
        ) : ToXContentObject, Writeable {

            @Throws(IOException::class)
            constructor(sin: StreamInput) : this(
                indexName = sin.readString(),
                indexHealth = sin.readEnum(ClusterHealthStatus::class.java),
            )

            override fun toXContent(builder: XContentBuilder, params: ToXContent.Params): XContentBuilder {
                return builder.startObject()
                    .field(INDEX_NAME_FIELD, indexName)
                    .field(INDEX_HEALTH_FIELD, indexHealth)
                    .endObject()
            }

            override fun writeTo(out: StreamOutput) {
                out.writeString(indexName)
                out.writeEnum(indexHealth)
            }

            companion object {
                const val INDEX_NAME_FIELD = "name"
                const val INDEX_HEALTH_FIELD = "health"

                @JvmStatic
                @Throws(IOException::class)
                fun readFrom(sin: StreamInput): IndexInfo {
                    return IndexInfo(sin)
                }
            }
        }
    }
}
