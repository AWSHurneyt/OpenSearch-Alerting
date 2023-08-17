/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.alerting.action

import org.opensearch.action.ActionType

class GetRemoteIndexesLatencyAction private constructor() : ActionType<GetRemoteIndexesLatencyResponse>(NAME, ::GetRemoteIndexesLatencyResponse) {
    companion object {
        val INSTANCE = GetRemoteIndexesLatencyAction()
        const val NAME = "cluster:admin/opensearch/alerting/remote/indexes/latency/get"
    }
}
