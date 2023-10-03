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
    var clusterAliases: List<String> = listOf()
    var includeMappings: Boolean

    constructor(clusterAliases: List<String>, includeMappings: Boolean) : super() {
        this.clusterAliases = clusterAliases
        this.includeMappings = includeMappings
    }

    @Throws(IOException::class)
    constructor(sin: StreamInput) : this(
        sin.readStringList(),
        sin.readBoolean()
    )

    override fun validate(): ActionRequestValidationException? {
        return null
    }

    @Throws(IOException::class)
    override fun writeTo(out: StreamOutput) {
        out.writeStringArray(clusterAliases.toTypedArray())
        out.writeBoolean(includeMappings)
    }

    companion object {
        // TODO hurneyt: is this companion needed?
        const val CLUSTERS_FIELD = "clusters"
        const val INCLUDE_MAPPINGS_FIELD = "include_mappings"
    }
}
