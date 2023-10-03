/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.alerting.resthandler

import org.apache.logging.log4j.LogManager
import org.opensearch.alerting.AlertingPlugin
import org.opensearch.alerting.action.GetRemoteIndexesAction
import org.opensearch.alerting.action.GetRemoteIndexesRequest
import org.opensearch.client.node.NodeClient
import org.opensearch.common.xcontent.XContentParserUtils.ensureExpectedToken
import org.opensearch.core.common.Strings
import org.opensearch.core.xcontent.XContentParser
import org.opensearch.rest.BaseRestHandler
import org.opensearch.rest.RestHandler
import org.opensearch.rest.RestRequest
import org.opensearch.rest.action.RestToXContentListener

private val log = LogManager.getLogger(RestGetRemoteIndexesAction::class.java)

class RestGetRemoteIndexesAction : BaseRestHandler() {
    val ROUTE = "${AlertingPlugin.REMOTE_BASE_URI}/indexes/{name}"

    override fun getName(): String {
        return "get_remote_indexes_action"
    }

    override fun routes(): List<RestHandler.Route> {
        return mutableListOf(
            RestHandler.Route(RestRequest.Method.GET, ROUTE)
//            ,
//            RestHandler.Route(RestRequest.Method.POST, ROUTE)
        )
    }

    override fun prepareRequest(request: RestRequest, client: NodeClient): RestChannelConsumer {
        log.debug("${request.method()} $ROUTE")
        log.info("hurneyt RestGetRemoteIndexesAction::request = {}", request)
        log.info("hurneyt RestGetRemoteIndexesAction::request.uri = {}", request.uri())

        val includeMappings = request.paramAsBoolean(GetRemoteIndexesRequest.INCLUDE_MAPPINGS_FIELD, false)
        log.info("hurneyt RestGetRemoteIndexesAction::includeMappings = {}", includeMappings)

//        val clusterAliases = getClusterAliases(request.contentParser())
        val clusterField = request.paramAsStringArray(GetRemoteIndexesRequest.CLUSTERS_FIELD, emptyArray<String>())
        log.info("hurneyt RestGetRemoteIndexesAction::clusterField = ", clusterField)

        val indexes = Strings.splitStringByCommaToArray(request.param("name"))
        log.info("hurneyt RestGetRemoteIndexesAction::indexes = ", indexes)

        val indexes2 = request.paramAsStringArray("name", emptyArray<String>())
        log.info("hurneyt RestGetRemoteIndexesAction::indexes2 = ", indexes2)
        return RestChannelConsumer {
                channel ->
            client.execute(
                GetRemoteIndexesAction.INSTANCE,
                GetRemoteIndexesRequest(indexes.toList(), includeMappings),
                RestToXContentListener(channel)
            )
        }
    }

    private fun getClusterAliases(xcp: XContentParser): List<String> {
        log.info("hurneyt RestGetRemoteIndexesAction::getClusterAliases START")
        val clusterAliases = mutableListOf<String>()
        ensureExpectedToken(XContentParser.Token.START_OBJECT, xcp.nextToken(), xcp)
        while (xcp.nextToken() != XContentParser.Token.END_OBJECT) {
            val fieldName = xcp.currentName()
            log.info("hurneyt RestGetRemoteIndexesAction::getClusterAliases fieldName = $fieldName")
            xcp.nextToken()
            when (fieldName) {
                GetRemoteIndexesRequest.CLUSTERS_FIELD -> {
                    ensureExpectedToken(XContentParser.Token.START_ARRAY, xcp.currentToken(), xcp)
                    while (xcp.nextToken() != XContentParser.Token.END_ARRAY) {
                        log.info("hurneyt RestGetRemoteIndexesAction::getClusterAliases xcp.text() = ${xcp.text()}")
                        clusterAliases.add(xcp.text())
                    }
                }
            }
        }
        log.info("hurneyt RestGetRemoteIndexesAction::getClusterAliases END")
        log.info("hurneyt RestGetRemoteIndexesAction::getClusterAliases clusterAliases = ${clusterAliases.joinToString(", ")}")
        return clusterAliases
    }
}
