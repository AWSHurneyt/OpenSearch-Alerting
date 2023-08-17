/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.alerting.action

import org.opensearch.action.ActionRequest
import org.opensearch.action.ActionRequestValidationException
import org.opensearch.common.io.stream.StreamInput
import java.io.IOException

class GetRemoteClustersRequest : ActionRequest {
    constructor()

    @Throws(IOException::class)
    constructor(sin: StreamInput) : this()

    override fun validate(): ActionRequestValidationException? {
        return null
    }
}
