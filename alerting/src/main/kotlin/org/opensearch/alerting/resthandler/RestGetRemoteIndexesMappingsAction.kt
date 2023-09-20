/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.alerting.resthandler

import org.apache.logging.log4j.LogManager
import org.opensearch.alerting.AlertingPlugin
import org.opensearch.alerting.action.GetRemoteIndexesMappingsAction
import org.opensearch.alerting.action.GetRemoteIndexesMappingsRequest
import org.opensearch.client.node.NodeClient
import org.opensearch.common.xcontent.XContentParserUtils
import org.opensearch.core.xcontent.XContentParser
import org.opensearch.rest.BaseRestHandler
import org.opensearch.rest.RestHandler
import org.opensearch.rest.RestRequest
import org.opensearch.rest.RestRequest.Method.GET
import org.opensearch.rest.RestRequest.Method.POST
import org.opensearch.rest.action.RestToXContentListener

private val log = LogManager.getLogger(RestGetRemoteIndexesMappingsAction::class.java)

class RestGetRemoteIndexesMappingsAction : BaseRestHandler() {
    val ROUTE = "${AlertingPlugin.REMOTE_BASE_URI}/indexes/mappings"

    override fun getName(): String {
        return "get_remote_indexes_mappings_action"
    }

    override fun routes(): List<RestHandler.Route> {
        return mutableListOf(
            RestHandler.Route(GET, ROUTE),
            RestHandler.Route(POST, ROUTE)
        )
    }

    override fun prepareRequest(request: RestRequest, client: NodeClient): RestChannelConsumer {
        log.debug("${request.method()} $ROUTE")
        val indexes = getIndexList(request.contentParser())
        return RestChannelConsumer {
                channel ->
            client.execute(
                GetRemoteIndexesMappingsAction.INSTANCE,
                GetRemoteIndexesMappingsRequest(indexes),
                RestToXContentListener(channel)
            )
        }
    }

    private fun getIndexList(xcp: XContentParser): List<String> {
        log.info("hurneyt RestGetRemoteIndexesMappingsAction::getIndexList START")
        val indexes = mutableListOf<String>()
        XContentParserUtils.ensureExpectedToken(XContentParser.Token.START_OBJECT, xcp.nextToken(), xcp)
        while (xcp.nextToken() != XContentParser.Token.END_OBJECT) {
            val fieldName = xcp.currentName()
            log.info("hurneyt RestGetRemoteIndexesMappingsAction::getIndexList fieldName = $fieldName")
            xcp.nextToken()
            when (fieldName) {
                GetRemoteIndexesMappingsRequest.INDEXES_FIELD -> {
                    XContentParserUtils.ensureExpectedToken(XContentParser.Token.START_ARRAY, xcp.currentToken(), xcp)
                    while (xcp.nextToken() != XContentParser.Token.END_ARRAY) {
                        log.info("hurneyt RestGetRemoteIndexesMappingsAction::getIndexList xcp.text() = ${xcp.text()}")
                        indexes.add(xcp.text())
                    }
                }
            }
        }
        log.info("hurneyt RestGetRemoteIndexesMappingsAction::getIndexList END")
        log.info("hurneyt RestGetRemoteIndexesMappingsAction::getIndexList indexes = ${indexes.joinToString(", ")}")
        return indexes
    }
}
