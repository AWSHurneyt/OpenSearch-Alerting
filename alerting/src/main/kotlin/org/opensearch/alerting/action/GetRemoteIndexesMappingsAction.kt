/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.alerting.action

import org.opensearch.action.ActionType

class GetRemoteIndexesMappingsAction private constructor() : ActionType<GetRemoteIndexesMappingsResponse>(NAME, ::GetRemoteIndexesMappingsResponse) {
    companion object {
        val INSTANCE = GetRemoteIndexesMappingsAction()
        const val NAME = "cluster:admin/opensearch/alerting/remote/indexes/mappings/get"
    }
}
