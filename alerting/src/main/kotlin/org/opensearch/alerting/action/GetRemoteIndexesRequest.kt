/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.alerting.action

import org.opensearch.action.ActionRequest
import org.opensearch.action.ActionRequestValidationException
import org.opensearch.common.io.stream.StreamInput
import org.opensearch.common.io.stream.StreamOutput
import java.io.IOException

class GetRemoteIndexesRequest : ActionRequest {
    var clusterAliases: List<String> = listOf()

    constructor(clusterAliases: List<String>) : super() {
        this.clusterAliases = clusterAliases
    }

    @Throws(IOException::class)
    constructor(sin: StreamInput) : this(
        sin.readStringList()
    )

    override fun validate(): ActionRequestValidationException? {
        return null
    }

    @Throws(IOException::class)
    override fun writeTo(out: StreamOutput) {
        out.writeStringArray(clusterAliases.toTypedArray())
    }

    companion object {
        const val CLUSTERS_FIELD = "clusters"
    }
}
