/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.alerting.action

import org.opensearch.action.ActionResponse
import org.opensearch.cluster.metadata.MappingMetadata
import org.opensearch.common.io.stream.StreamInput
import org.opensearch.common.io.stream.StreamOutput
import org.opensearch.core.xcontent.ToXContent
import org.opensearch.core.xcontent.ToXContentObject
import org.opensearch.core.xcontent.XContentBuilder
import java.io.IOException

class GetRemoteIndexesMappingsResponse : ActionResponse, ToXContentObject {
    var clusterIndexes: Map<String, MappingMetadata> = hashMapOf()

    constructor(clusterIndexes: Map<String, MappingMetadata>) {
        this.clusterIndexes = clusterIndexes
    }

    @Throws(IOException::class)
    constructor(sin: StreamInput) : this(
        clusterIndexes = sin.readMap(StreamInput::readString, ::MappingMetadata)
    )

    override fun toXContent(builder: XContentBuilder, params: ToXContent.Params): XContentBuilder {
        builder.startObject()
        clusterIndexes.forEach {
            builder.startObject(it.key)
            if (it.value == null) builder.startObject(MAPPINGS_FIELD).endObject()
            else builder.field(MAPPINGS_FIELD, it.value.sourceAsMap())
        }
        builder.endObject()
        return builder.endObject()
    }

    override fun writeTo(out: StreamOutput) {
        out.writeMap(clusterIndexes as Map<String, Any>?)
    }

    companion object {
        const val MAPPINGS_FIELD = "mappings"
    }
}
