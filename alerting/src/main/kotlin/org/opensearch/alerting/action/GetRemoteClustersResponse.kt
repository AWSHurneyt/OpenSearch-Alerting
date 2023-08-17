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

class GetRemoteClustersResponse : ActionResponse, ToXContentObject {
    var clusterAliases: List<ClusterInfo> = emptyList()

    constructor(clusters: List<ClusterInfo>) : super() {
        this.clusterAliases = clusters
    }

    @Throws(IOException::class)
    constructor(sin: StreamInput) : this(
        clusters = sin.readList((ClusterInfo.Companion)::readFrom)
    )

    override fun toXContent(builder: XContentBuilder, params: ToXContent.Params): XContentBuilder {
        builder.startObject()
            .startArray(CLUSTER_ALIASES_FIELD)
        clusterAliases.forEach { it.toXContent(builder, params) }
        return builder.endArray().endObject()
    }

    override fun writeTo(out: StreamOutput) {
        clusterAliases.forEach { it.writeTo(out) }
    }

    companion object {
        const val CLUSTER_ALIASES_FIELD = "clusters"
    }

    data class ClusterInfo(
        val clusterAlias: String,
        val connected: Boolean
    ) : ToXContentObject, Writeable {

        @Throws(IOException::class)
        constructor(sin: StreamInput) : this(
            clusterAlias = sin.readString(),
            connected = sin.readBoolean()
        )

        override fun toXContent(builder: XContentBuilder, params: ToXContent.Params): XContentBuilder {
            return builder.startObject()
                .field(CLUSTER_ALIAS_FIELD, clusterAlias)
                .field(CONNECTED_FIELD, connected)
                .endObject()
        }

        override fun writeTo(out: StreamOutput) {
            out.writeString(clusterAlias)
            out.writeBoolean(connected)
        }

        companion object {
            const val CLUSTER_ALIAS_FIELD = "cluster"
            const val CONNECTED_FIELD = "connected"

            @JvmStatic
            @Throws(IOException::class)
            fun readFrom(sin: StreamInput): ClusterInfo {
                return ClusterInfo(sin)
            }
        }
    }
}
