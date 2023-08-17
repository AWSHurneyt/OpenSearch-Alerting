/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.alerting.resthandler

import org.apache.logging.log4j.LogManager
import org.opensearch.alerting.AlertingPlugin
import org.opensearch.alerting.action.GetRemoteIndexesLatencyAction
import org.opensearch.alerting.action.GetRemoteIndexesLatencyRequest
import org.opensearch.client.node.NodeClient
import org.opensearch.rest.BaseRestHandler
import org.opensearch.rest.RestHandler
import org.opensearch.rest.RestRequest
import org.opensearch.rest.RestRequest.Method.GET
import org.opensearch.rest.action.RestToXContentListener

private val log = LogManager.getLogger(RestGetRemoteIndexesLatencyAction::class.java)

class RestGetRemoteIndexesLatencyAction : BaseRestHandler() {
    val ROUTE = "${AlertingPlugin.REMOTE_BASE_URI}/indexes/latency"

    override fun getName(): String {
        return "get_remote_indexes_latency_action"
    }

    override fun routes(): List<RestHandler.Route> {
        return mutableListOf(
            RestHandler.Route(GET, ROUTE)
        )
    }

    override fun prepareRequest(request: RestRequest, client: NodeClient): RestChannelConsumer {
        log.debug("${request.method()} $ROUTE")

        val indexes = request.param("clusters")

        log.info("hurneyt RestGetRemoteIndexesLatencyAction indexes = $indexes")
        return RestChannelConsumer {
                channel ->
            client.execute(
                GetRemoteIndexesLatencyAction.INSTANCE,
                GetRemoteIndexesLatencyRequest.parse(request.contentParser()),
                RestToXContentListener(channel)
            )
        }
    }
}
