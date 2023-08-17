/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.alerting.action

import org.opensearch.action.ActionType

class GetRemoteClustersAction private constructor() : ActionType<GetRemoteClustersResponse>(NAME, ::GetRemoteClustersResponse) {
    companion object {
        val INSTANCE = GetRemoteClustersAction()
        const val NAME = "cluster:admin/opensearch/alerting/remote/clusters/get"
    }
}
