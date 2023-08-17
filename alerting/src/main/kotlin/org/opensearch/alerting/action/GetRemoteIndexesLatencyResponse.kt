/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.alerting.action

import org.opensearch.action.ActionResponse
import org.opensearch.common.io.stream.StreamInput
import org.opensearch.common.io.stream.StreamOutput
import org.opensearch.common.io.stream.Writeable
import org.opensearch.core.xcontent.ToXContent
import org.opensearch.core.xcontent.ToXContentObject
import org.opensearch.core.xcontent.XContentBuilder
import java.io.IOException

class GetRemoteIndexesLatencyResponse : ActionResponse, ToXContentObject {
    var clusterIndexes: List<RemoteIndexesLatency> = emptyList()

    constructor(remoteIndexes: List<RemoteIndexesLatency>) {
        this.clusterIndexes = remoteIndexes
    }

    @Throws(IOException::class)
    constructor(sin: StreamInput) : this(
        remoteIndexes = sin.readList((RemoteIndexesLatency.Companion)::readFrom)
    )

    override fun toXContent(builder: XContentBuilder, params: ToXContent.Params): XContentBuilder {
        builder.startObject()
            .startArray(CLUSTER_INDEXES_FIELD)
        clusterIndexes.forEach { it.toXContent(builder, params) }
        return builder.endArray().endObject()
    }

    override fun writeTo(out: StreamOutput) {
        clusterIndexes.forEach { it.writeTo(out) }
    }

    companion object {
        const val CLUSTER_INDEXES_FIELD = "clusters"
    }

    data class RemoteIndexesLatency(
        val clusterAlias: String,
        val indexes: List<IndexLatencyInfo>
    ) : ToXContentObject, Writeable {

        @Throws(IOException::class)
        constructor(sin: StreamInput) : this(
            clusterAlias = sin.readString(),
            indexes = sin.readList((IndexLatencyInfo.Companion)::readFrom)
        )

        override fun toXContent(builder: XContentBuilder, params: ToXContent.Params): XContentBuilder {
            builder.startObject()
                .field(CLUSTER_ALIAS_FIELD, clusterAlias)
                .startArray(INDEXES_FIELD)
            indexes.forEach { it.toXContent(builder, params) }
            return builder.endArray().endObject()
        }

        override fun writeTo(out: StreamOutput) {
            out.writeString(clusterAlias)
            indexes.forEach { it.writeTo(out) }
        }

        companion object {
            const val CLUSTER_ALIAS_FIELD = "cluster"
            const val INDEXES_FIELD = "indexes"

            @JvmStatic
            @Throws(IOException::class)
            fun readFrom(sin: StreamInput): RemoteIndexesLatency {
                return RemoteIndexesLatency(sin)
            }
        }

        data class IndexLatencyInfo(
            val index: String,
            val latency: Long?
        ) : ToXContentObject, Writeable {

            @Throws(IOException::class)
            constructor(sin: StreamInput) : this(
                index = sin.readString(),
                latency = sin.readLong()
            )

            override fun toXContent(builder: XContentBuilder, params: ToXContent.Params): XContentBuilder {
                return builder.startObject()
                    .field(INDEX_FIELD, index)
                    .field(LATENCY_FIELD, latency)
                    .endObject()
            }

            override fun writeTo(out: StreamOutput) {
                out.writeString(index)
                if (latency != null) {
                    out.writeLong(latency)
                }
            }

            companion object {
                const val INDEX_FIELD = "index"
                const val LATENCY_FIELD = "latency"

                @JvmStatic
                @Throws(IOException::class)
                fun readFrom(sin: StreamInput): IndexLatencyInfo {
                    return IndexLatencyInfo(sin)
                }
            }
        }
    }
}
