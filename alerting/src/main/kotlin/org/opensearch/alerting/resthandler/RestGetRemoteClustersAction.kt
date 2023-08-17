/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.alerting.resthandler

import org.apache.logging.log4j.LogManager
import org.opensearch.alerting.AlertingPlugin
import org.opensearch.alerting.action.GetRemoteClustersAction
import org.opensearch.alerting.action.GetRemoteClustersRequest
import org.opensearch.client.node.NodeClient
import org.opensearch.rest.BaseRestHandler
import org.opensearch.rest.RestHandler.Route
import org.opensearch.rest.RestRequest
import org.opensearch.rest.RestRequest.Method.GET
import org.opensearch.rest.action.RestToXContentListener

private val log = LogManager.getLogger(RestGetRemoteClustersAction::class.java)

class RestGetRemoteClustersAction : BaseRestHandler() {
    val ROUTE = "${AlertingPlugin.REMOTE_BASE_URI}/clusters"

    override fun getName(): String {
        return "get_remote_clusters_action"
    }

    override fun routes(): List<Route> {
        return mutableListOf(
            Route(GET, ROUTE)
        )
    }

    override fun prepareRequest(request: RestRequest, client: NodeClient): RestChannelConsumer {
        log.debug("${request.method()} $ROUTE")
        return RestChannelConsumer {
                channel ->
            client.execute(
                GetRemoteClustersAction.INSTANCE,
                GetRemoteClustersRequest(),
                RestToXContentListener(channel)
            )
        }
    }
}
