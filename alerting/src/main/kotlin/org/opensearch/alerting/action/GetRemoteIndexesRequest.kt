/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.alerting.action

import org.apache.logging.log4j.LogManager
import org.opensearch.action.ActionRequest
import org.opensearch.action.ActionRequestValidationException
import org.opensearch.common.io.stream.StreamInput
import org.opensearch.common.io.stream.StreamOutput
import java.io.IOException

private val log = LogManager.getLogger(GetRemoteIndexesRequest::class.java)

class GetRemoteIndexesRequest : ActionRequest {
    var indexes: List<String> = listOf()
    var includeMappings: Boolean

    constructor(indexes: List<String>, includeMappings: Boolean) : super() {
        this.indexes = indexes
        this.includeMappings = includeMappings
    }

    @Throws(IOException::class)
    constructor(sin: StreamInput) : this(
        sin.readStringList(),
        sin.readBoolean()
    )

    override fun validate(): ActionRequestValidationException? {
        // TODO hurneyt
        return null
    }

    @Throws(IOException::class)
    override fun writeTo(out: StreamOutput) {
        out.writeStringArray(indexes.toTypedArray())
        out.writeBoolean(includeMappings)
    }

    companion object {
        // TODO hurneyt: is this companion needed?
        const val INDEXES_FIELD = "indexes"
        const val INCLUDE_MAPPINGS_FIELD = "include_mappings"
    }
}
